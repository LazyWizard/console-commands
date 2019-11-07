@file:JvmName("ConsoleOverlay")

package org.lazywizard.console

import com.fs.starfarer.api.Global
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.BufferUtils
import org.lwjgl.Sys
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Keyboard.*
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL
import org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import java.awt.Color
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage
import java.text.DecimalFormat

private val Log = Global.getLogger(Console::class.java)
private var history = ""
private const val CURSOR_BLINK_SPEED = 0.7f
internal const val HORIZONTAL_MARGIN = 30f // Don't go below 30; TODO: scale minor UI elements using this setting
private var overlay: ConsoleOverlayInternal? = null

fun show(context: CommandContext) = with(ConsoleOverlayInternal(context,
        Console.getSettings().outputColor, Console.getSettings().outputColor.darker()))
{
    try {
        overlay = this
        show()
    } catch (ex: Exception) {
        Console.showException("The console overlay encountered an error and was destroyed: ", ex)
        Log.error("Scrollback at time of destruction:\n\n$history\n\n -- END SCROLLBACK --\n")
        history = ""
    } finally {
        overlay = null
        dispose()
    }
}

fun clear() {
    overlay?.clear()
}

internal fun addToHistory(toAdd: String) {
    history += toAdd
}

// TODO: This uses a lot of hardcoded numbers; need to refactor these into constants at some point
// TODO: Move UI element instantiation into initializer to make size/position details clearer
private class ConsoleOverlayInternal(private val context: CommandContext, mainColor: Color, secondaryColor: Color) : ConsoleListener {
    private val settings = Console.getSettings()
    private val bgTextureId = if (settings.showBackground) glGenTextures() else 0
    private val byteFormat = DecimalFormat("#,##0.#")
    private val memory = ManagementFactory.getMemoryMXBean()
    private val font = Console.getFont()
    private val width = Display.getWidth() * Display.getPixelScaleFactor()
    private val height = Display.getHeight() * Display.getPixelScaleFactor()
    private val fontSize = Console.getFontSize()
    private val minX = HORIZONTAL_MARGIN
    private val maxX = minX + Console.getScrollbackWidth()
    private val minY = 50f + fontSize
    private val maxY = height - 40f
    private val scrollback = font.createText(text = history, size = fontSize, color = mainColor, maxWidth = maxX - minX)
    private val query = font.createText(text = CommonStrings.INPUT_QUERY, color = secondaryColor, maxWidth = width, maxHeight = 30f)
    private val prompt = font.createText(text = "> ", color = secondaryColor, maxWidth = width, maxHeight = 30f)
    private val input = font.createText(text = "", color = mainColor, maxWidth = width - (prompt.width + 60f), maxHeight = fontSize * 30)
    private val mem = font.createText(text = getMemText(), color = Color.LIGHT_GRAY)
    private val curContext = font.createText(text = context.name, color = secondaryColor)
    private val devMode = font.createText(text = "DEVMODE", color = Color.LIGHT_GRAY)
    private val scrollbar = Scrollbar(10f, secondaryColor, secondaryColor.darker().darker())
    private val currentInput = StringBuilder()
    private var lastInput: String? = null
    private var scrollOffset = 0f
    private var minScroll = 0f
    private var currentIndex = 0
    private var lastIndex = 0
    private var nextBlink = CURSOR_BLINK_SPEED
    private var showCursor = true
    private var needsTextUpdate = true
    private var isOpen = false

    private inner class Scrollbar(val width: Float, val barColor: Color, val bgColor: Color) {
        fun draw(x: Float, y: Float, height: Float) {
            // Determine size and relative position of scrollbar
            val contentRatio = (maxY - minY) / scrollback.height
            val scrollRatio = scrollOffset / minScroll

            // Don't draw the scrollbar if the screen can fit the entire scrollback
            // scrollRatio should never be NaN unless contentRatio is infinity,
            // but I'm including the check just in case the code changes later
            val barHeight: Float
            val barY: Float
            if (contentRatio > 1f || scrollRatio.isNaN()) {
                barHeight = 0f
                barY = y
            } else {
                barHeight = height * contentRatio
                barY = y + ((height - barHeight) * scrollRatio)
            }

            // Only draw scrollbar if content exceeds screen space
            if (barHeight <= 0f) return

            glBegin(GL_QUADS)
            // Background
            glColor(bgColor, 0.2f, true)
            glVertex2f(x, y)
            glVertex2f(x, y + height)
            glVertex2f(x + width, y + height)
            glVertex2f(x + width, y)
            // Foreground
            glColor(barColor, 0.5f, true)
            glVertex2f(x, barY)
            glVertex2f(x, barY + barHeight)
            glVertex2f(x + width, barY + barHeight)
            glVertex2f(x + width, barY)
            glEnd()
        }
    }

    fun show() {
        // Calculates time elapsed since last frame
        var lastUpdate = Sys.getTime()
        fun calcDelta(): Float {
            val curTime = Sys.getTime()
            val delta = (curTime - lastUpdate).toFloat() / Sys.getTimerResolution()
            lastUpdate = curTime
            return delta
        }

        // Save the current screen to a texture, to be used as the overlay background
        // This texture will be extremely low quality to save VRAM, but since the
        // background will be drawn darkened, the compression shouldn't be noticeable
        if (settings.showBackground) {
            val buffer = BufferUtils.createByteBuffer(width.toInt() * height.toInt() * 3)
            glReadPixels(0, 0, width.toInt(), height.toInt(), GL_RGB, GL_UNSIGNED_BYTE, buffer)
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, bgTextureId)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_R3_G3_B2, width.toInt(), height.toInt(), 0, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        }

        // Show overlay until closed by player
        isOpen = true
        lastUpdate = Sys.getTime()
        while (isOpen) {
            Display.update()
            checkInput()

            // Alt+F4 support
            if (Display.isCloseRequested()) System.exit(0)

            advance(calcDelta())
            render()
            Display.sync(40)
        }

        // Clean up background texture (if any) and clear any remaining input events
        if (settings.showBackground) glDeleteTextures(bgTextureId)
        while (Keyboard.next()) Keyboard.poll()
    }

    fun clear() {
        scrollback.text = ""
    }

    fun dispose() {
        // Clean up native resources, limit memory used by console scrollback history
        history = scrollback.text.takeLast(ConsoleSettings.maxScrollback)
        scrollback.dispose()
        query.dispose()
        prompt.dispose()
        input.dispose()
        mem.dispose()
        curContext.dispose()
        devMode.dispose()
    }

    // TODO: Add explicit indentation support
    override fun showOutput(output: String): Boolean {
        if (!isOpen) return false

        scrollback.appendText(output)
        scrollOffset = 0f
        return true
    }

    override fun getContext() = context

    // Based on this StackOverflow answer: https://stackoverflow.com/a/5599842
    private fun asString(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val digits = size / Math.pow(1024.0, digitGroups.toDouble())
        return "${byteFormat.format(digits)} ${units[digitGroups]}"
    }

    // Creates readable memory usage string; ex: "8% (248.6 MB/2.9 GB)"
    private fun asString(usage: MemoryUsage): String = with(usage) {
        val percent = DecimalFormat.getPercentInstance().format(used / Math.max(max, committed).toDouble())
        "$percent (${asString(used)}/${asString(Math.max(max, committed))})"
    }

    private fun getMemText(): String {
        // TODO: Add setting to toggle non-heap memory usage visibility (default false) )
        return "Memory used: ${asString(memory.heapMemoryUsage)}" //"   |   Non-heap: ${asString(memory.nonHeapMemoryUsage)}"
    }

    private fun getMemColor(usage: MemoryUsage): Color = with(usage) {
        val total = Math.max(max, committed)
        val portion = used / total
        val remaining = total - used
        if (remaining < (1024 * 1024 * 200) || portion > 0.9) return Color.RED
        else if (remaining < (1024 * 1024 * 400) || portion > 0.8) return Color.YELLOW
        return Color.GREEN
    }

    private fun checkInput() {
        if (Keyboard.isKeyDown(KEY_ESCAPE)) {
            isOpen = false
            return
        }

        // Mouse wheel scrolling
        val scrollY = Mouse.getDWheel()
        scrollOffset -= scrollY * .2f

        val ctrlDown = Keyboard.isKeyDown(KEY_LCONTROL) || Keyboard.isKeyDown(KEY_RCONTROL)
        val shiftDown = Keyboard.isKeyDown(KEY_LSHIFT) || Keyboard.isKeyDown(KEY_RSHIFT)
        //val altDown = Keyboard.isKeyDown(KEY_LMETA) || Keyboard.isKeyDown(KEY_RMETA)
        val previousLength = currentInput.length
        while (Keyboard.next()) {
            try {
                // Only pay attention to key down events
                if (!Keyboard.getEventKeyState()) continue

                // Always show the current cursor position after any keypress
                showCursor = true
                needsTextUpdate = true
                nextBlink = CURSOR_BLINK_SPEED * 1.6f // Last a little longer than usual after a deliberate keypress

                // Load last command when user presses up on keyboard
                val keyPressed = Keyboard.getEventKey()
                if (keyPressed == KEY_UP && Console.getLastCommand() != null) {
                    lastInput = currentInput.toString()
                    lastIndex = currentIndex
                    currentInput.replace(0, currentInput.length, Console.getLastCommand())
                    currentIndex = currentInput.length
                    continue
                }

                // Down restores previous command overwritten by up
                if (keyPressed == KEY_DOWN && lastInput != null) {
                    currentInput.replace(0, currentInput.length, lastInput)
                    currentIndex = lastIndex
                    lastInput = null
                    lastIndex = currentInput.length
                    continue
                }

                // Tab auto-completes the current command
                if (keyPressed == KEY_TAB) {
                    // Get just the current command (separator support complicates things)
                    val startIndex = currentInput.lastIndexOf(settings.commandSeparator, currentIndex) + 1
                    val tmp = currentInput.indexOf(settings.commandSeparator, startIndex)
                    val endIndex = if (tmp < 0) currentInput.length else tmp
                    val toIndex = currentInput.substring(startIndex, Math.max(startIndex, currentIndex))
                    val fullCommand = currentInput.substring(startIndex, endIndex)

                    // Only auto-complete if arguments haven't been entered
                    if (' ' in fullCommand) {
                        continue
                    }

                    // Cycle through matching commands from current index forward
                    // If no further matches are found, start again from beginning
                    var firstMatch: String? = null
                    var nextMatch: String? = null
                    val commands = CommandStore.getApplicableCommands(context)
                    commands.sort()

                    // Reverse order when shift is held down
                    if (shiftDown) commands.reverse()

                    for (command in commands) {
                        if (command.regionMatches(0, toIndex, 0, toIndex.length, true)) {
                            // Used to cycle back to the beginning when no more matches are found
                            if (firstMatch == null) firstMatch = command

                            // Found next matching command
                            if ((shiftDown && command.compareTo(fullCommand, true) < 0)
                                    || (!shiftDown && command.compareTo(fullCommand, true) > 0)) {
                                nextMatch = command
                                break
                            }
                        }
                    }

                    if (nextMatch != null)
                        currentInput.replace(startIndex, endIndex, nextMatch)
                    else if (firstMatch != null)
                        currentInput.replace(startIndex, endIndex, firstMatch)

                    continue
                }

                // Left or right move the editing cursor; home and end move to start/end respectively
                if (keyPressed == KEY_LEFT) {
                    currentIndex = Math.max(0, currentIndex - 1)
                    continue
                } else if (keyPressed == KEY_RIGHT) {
                    currentIndex = Math.min(currentInput.length, currentIndex + 1)
                    continue
                } else if (keyPressed == KEY_HOME) {
                    currentIndex = 0
                    continue
                } else if (keyPressed == KEY_END) {
                    currentIndex = currentInput.length
                    continue
                }

                // PageUp/Down; scroll an entire page at once
                if (keyPressed == KEY_PRIOR) {
                    if (ctrlDown || shiftDown)
                        scrollOffset = minScroll
                    else
                        scrollOffset -= height - (80 + fontSize)
                } else if (keyPressed == KEY_NEXT) {
                    if (ctrlDown || shiftDown)
                        scrollOffset = 0f
                    else
                        scrollOffset += height - (80 + fontSize)
                }

                // Backspace handling; imitates vanilla text inputs
                if (keyPressed == KEY_BACK && currentIndex > 0) {
                    // Control+backspace, delete last word
                    if (ctrlDown) {
                        // Positional editing support
                        val lastSpace = currentInput.substring(0, currentIndex).lastIndexOfAny(listOf(" ", settings.commandSeparator))
                        if (lastSpace == -1) currentInput.delete(0, currentIndex)
                        else currentInput.delete(lastSpace, currentIndex)
                    } else { // Regular backspace, delete last character
                        currentInput.deleteCharAt(currentIndex - 1)
                    }
                } // Delete key handling
                else if (keyPressed == KEY_DELETE && currentIndex < currentInput.length) {
                    currentInput.deleteCharAt(currentIndex)
                    currentIndex++
                }
                // Return key handling
                else if (keyPressed == KEY_RETURN) {
                    // Shift+enter to enter a newline
                    if (shiftDown) {
                        currentInput.insert(currentIndex, '\n')
                    }
                    // Enter to execute current command
                    else {
                        val command = currentInput.toString()
                        when {
                            command.equals("exit", true) -> {
                                isOpen = false
                                return
                            }
                            // TODO: Ensure newlines are supported everywhere (currently replaced with spaces where not supported)
                            command.startsWith("runcode ", true) -> Console.parseInput(command, context)
                            else -> Console.parseInput(command.replace('\n', ' '), context)
                        }
                        currentInput.setLength(0)
                        currentIndex = 0
                        lastInput = null
                        lastIndex = 0
                    }
                }
                // Paste handling
                else if (keyPressed == KEY_V && ctrlDown && Sys.getClipboard() != null) {
                    val pasted = Sys.getClipboard() ?: continue
                    currentInput.insert(currentIndex, pasted)
                }
                // Normal typing
                else {
                    val char = Keyboard.getEventCharacter()
                    if (char.toInt() in 0x20..0x7e) {
                        currentInput.insert(currentIndex, char)
                    }
                }

                // Update cursor index based on what changed since last input
                currentIndex += currentInput.length - previousLength
                currentIndex = Math.min(Math.max(0, currentIndex), currentInput.length)
            } catch (ex: ArrayIndexOutOfBoundsException) {
                Console.showMessage("Something went wrong with the input parser!"
                        + "\nPlease send a copy of starsector.log to LazyWizard.")
                Log.error("Input dump:\n - Current input: $currentInput | Index:" +
                        " $currentIndex/${currentInput.length}\n - Last input: ${lastInput ?: "null"}" +
                        " | Index: $lastIndex/${lastInput?.length}", ex)
                currentIndex = currentInput.length
                lastIndex = 0
            }
        }
    }

    private fun advance(amount: Float) {
        Console.advance(this)

        // Handle cursor blinking
        nextBlink -= amount
        if (nextBlink <= 0f) {
            showCursor = !showCursor
            nextBlink = CURSOR_BLINK_SPEED
            needsTextUpdate = true
        }

        // Ensure scrolling can't go beyond the bounds of the displayed text
        minScroll = (-scrollback.height + (height - 80 - fontSize)).coerceAtMost(0f)
        scrollOffset = scrollOffset.coerceIn(minScroll, 0f)

        // Only update our DrawableStrings when there's actually been a change
        if (needsTextUpdate) {
            needsTextUpdate = false
            val cursor = if (showCursor) "|" else " "

            // Show blinking cursor in the proper position
            if (currentIndex == currentInput.length) input.text = "$currentInput$cursor"
            else input.text = "${currentInput.substring(0, currentIndex)}$cursor${currentInput.substring(currentIndex)}"

            if (settings.showCursorIndex) input.appendText(" | Index: $currentIndex/${currentInput.length}")
            if (settings.showMemoryUsage) {
                mem.text = getMemText()
                mem.color = getMemColor(memory.heapMemoryUsage)
            }
        }
    }

    private fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        glClearColor(0f, 0f, 0f, 1f)

        // Set up OpenGL flags
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        glMatrixMode(GL_PROJECTION)
        glPushMatrix()
        glLoadIdentity()
        glViewport(0, 0, width.toInt(), height.toInt())
        glOrtho(0.0, width.toDouble(), 0.0, height.toDouble(), -1.0, 1.0)
        glMatrixMode(GL_MODELVIEW)
        glPushMatrix()
        glLoadIdentity()
        glTranslatef(0.01f, 0.01f, 0f)

        glEnable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)

        // Draw background
        if (settings.showBackground) {
            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, bgTextureId)
            glPushMatrix()
            glBegin(GL_QUADS)
            glColor4f(0.1f, 0.1f, 0.1f, 1f)
            glTexCoord2f(0f, 0f)
            glVertex2f(0f, 0f)
            glTexCoord2f(1f, 0f)
            glVertex2f(width, 0f)
            glTexCoord2f(1f, 1f)
            glVertex2f(width, height)
            glTexCoord2f(0f, 1f)
            glVertex2f(0f, height)
            glEnd()
            glPopMatrix()
        }

        val inputHeight = Math.max(fontSize, input.height)

        // Draw scrollback
        val minY = minY + (inputHeight - fontSize)
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_STENCIL_TEST)
        glColorMask(false, false, false, false)
        glStencilFunc(GL_ALWAYS, 1, 1)
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE)
        glBegin(GL_QUADS)
        glVertex2f(minX, minY)  // LL
        glVertex2f(minX, maxY)  // UL
        glVertex2f(maxX, maxY)  // UR
        glVertex2f(maxX, minY)  // LR
        glEnd()
        glColorMask(true, true, true, true)
        glStencilFunc(GL_EQUAL, 1, 1)
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
        scrollback.draw(minX, minY + scrollback.height + scrollOffset)
        glDisable(GL_STENCIL_TEST)

        // Draw input prompt
        query.draw(30f, 35f + inputHeight)
        prompt.draw(30f, 15f + inputHeight)
        input.draw(30f + prompt.width, 15f + inputHeight)

        // Draw misc stats
        if (settings.showMemoryUsage) mem.draw(50f, height - fontSize)
        if (Global.getSettings().isDevMode) devMode.draw(maxX - (50f + devMode.width), height - fontSize)
        curContext.draw(Math.max(150f + mem.width, (width / 2f) - (curContext.width / 2f)), height - fontSize)

        // Draw scrollbar
        glDisable(GL_TEXTURE_2D)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        scrollbar.draw(maxX + 10f, minY, maxY - minY)

        // Draw scrollback bounds
        glLineWidth(1f)
        glColor(Color.GRAY, 0.05f, true)
        glBegin(GL_LINE_LOOP)
        glVertex2f(minX - 1f, minY - 1f)  // LL
        glVertex2f(minX - 1f, maxY + 1f)  // UL
        glVertex2f(maxX + 1f, maxY + 1f)  // UR
        glVertex2f(maxX + 1f, minY - 1f)  // LR
        glEnd()

        // Clear OpenGL flags
        glPopMatrix()
        glMatrixMode(GL_PROJECTION)
        glPopMatrix()
        glPopAttrib()
    }
}
