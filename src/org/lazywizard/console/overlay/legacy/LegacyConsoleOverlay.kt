@file:JvmName("LegacyConsoleOverlay")


//LEGACY CONSOLE


/**
 * This overlay seems to be an exercise in how many features I can cram in before
 * I'm forced to write proper UI and layout classes. I apologize in advance to
 * anyone who has to read this mess.
 */

package org.lazywizard.console.overlay.legacy

import com.fs.starfarer.api.Global
import org.lazywizard.console.*
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.console.ext.GPUInfo
import org.lazywizard.console.ext.getGPUInfo
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.BufferUtils
import org.lwjgl.Sys
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL
import org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL30.GL_INVALID_FRAMEBUFFER_OPERATION
import org.lwjgl.opengl.GL30.glGenerateMipmap
import java.awt.Color
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage
import java.text.DecimalFormat

private val Log = Global.getLogger(Console::class.java)
private var history = ""
private const val CURSOR_BLINK_SPEED = 0.7f
internal const val HORIZONTAL_MARGIN = 30f // Don't go below 30; TODO: scale minor UI elements using this setting
private var overlay: ConsoleOverlayInternal? = null

fun show(context: CommandContext) = with(
    ConsoleOverlayInternal(
        context,
        ConsoleSettings.outputColor,
        ConsoleSettings.outputColor.darker()
    )
)
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

internal fun getErrorString(err: Int) = when (err) {
    GL_NO_ERROR -> "GL_NO_ERROR"
    GL_INVALID_ENUM -> "GL_INVALID_ENUM"
    GL_INVALID_VALUE -> "GL_INVALID_VALUE"
    GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
    GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
    GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
    GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW"
    GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW"

    else -> {
        "unknown error"
    }
}

// TODO: This uses a lot of hardcoded numbers; need to refactor these into constants at some point
// TODO: Move UI element instantiation into initializer to make size/position details clearer
internal class ConsoleOverlayInternal(private val context: CommandContext, mainColor: Color, secondaryColor: Color) :
    ConsoleListener {
    private val settings = Console.getSettings()
    private val bgTextureId = if (ConsoleSettings.showBackground) glGenTextures() else 0
    private val byteFormat = DecimalFormat("#,##0.#")
    private val memory = ManagementFactory.getMemoryMXBean()
    private val gpuInfo = getGPUInfo()
    private val font = Console.getFont()
    private val width = Display.getWidth() * Display.getPixelScaleFactor()
    private val height = Display.getHeight() * Display.getPixelScaleFactor()
    private val fontSize = Console.getFontSize()
    private val minX = HORIZONTAL_MARGIN
    private val maxX = minX + Console.getScrollbackWidth()
    private val minY = 50f + fontSize
    private val maxY = height - 80f
    private val scrollback =
        font.createText(text = history.trimStart(), size = fontSize, baseColor = mainColor, maxWidth = maxX - minX)
    private val query =
        font.createText(text = CommonStrings.INPUT_QUERY, baseColor = secondaryColor, maxWidth = width, maxHeight = 30f)
    private val prompt = font.createText(text = "> ", baseColor = secondaryColor, maxWidth = width, maxHeight = 30f)
    private val input = font.createText(
        text = "",
        baseColor = mainColor,
        maxWidth = width - (prompt.width + 60f),
        maxHeight = fontSize * 30
    )
    private val ramText = font.createText(text = getRAMText(), baseColor = Color.LIGHT_GRAY)
    private val vramText = font.createText(text = getVRAMText(), baseColor = Color.LIGHT_GRAY)
    private val curContext = font.createText(text = context.name, baseColor = secondaryColor)
    private val curTarget = font.createText(text = getCurrentTarget(), baseColor = secondaryColor)
    private val devMode = font.createText(text = "DEVMODE", baseColor = Color.LIGHT_GRAY)
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
    private var firstFrame = true

    companion object {
        @JvmStatic
        public var lastCommand: String? = null
    }

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

    // TODO: Remove after LazyLib 3.0 reverts 2.8b's default blendSrc changes
    init {
        scrollback.blendSrc = GL_ONE
        query.blendSrc = GL_ONE
        prompt.blendSrc = GL_ONE
        input.blendSrc = GL_ONE
        ramText.blendSrc = GL_ONE
        vramText.blendSrc = GL_ONE
        curContext.blendSrc = GL_ONE
        curTarget.blendSrc = GL_ONE
        devMode.blendSrc = GL_ONE
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
        if (ConsoleSettings.showBackground) {
            glGetError() // Clear existing error flag, if any
            val buffer = BufferUtils.createByteBuffer(width.toInt() * height.toInt() * 3)
            glReadPixels(0, 0, width.toInt(), height.toInt(), GL_RGB, GL_UNSIGNED_BYTE, buffer)
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, bgTextureId)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glGenerateMipmap(GL_TEXTURE_2D)
            glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_R3_G3_B2,
                width.toInt(),
                height.toInt(),
                0,
                GL_RGB,
                GL_UNSIGNED_BYTE,
                buffer
            )

            // Fallback in case generating background fails: free memory and disable until manually re-enabled
            val err = glGetError();
            if (err != GL_NO_ERROR) {

                glDeleteTextures(bgTextureId)
                ConsoleSettings.showBackground = false
                Console.showMessage("Failed to size buffer for background image! Disabling console background (can be re-enabled with Settings command)...")
                Console.showMessage("Error id: " + getErrorString(err))
            }
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
        if (ConsoleSettings.showBackground) glDeleteTextures(bgTextureId)
        while (Keyboard.next()) Keyboard.poll()
        while (Mouse.next()) Mouse.poll()
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
        ramText.dispose()
        vramText.dispose()
        curContext.dispose()
        curTarget.dispose()
        devMode.dispose()
    }

    // TODO: Add explicit indentation support
    override fun showOutput(output: String): Boolean {
        if (!isOpen) return false

        scrollback.append(output)
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

    // TODO: Add setting to toggle non-heap memory usage visibility (default false) )
    private fun getRAMText(): String =
        "Memory used: ${asString(memory.heapMemoryUsage)}"
    //"  |  Non-heap: ${asString(memory.nonHeapMemoryUsage)}"

    private fun getRAMColor(usage: MemoryUsage): Color = with(usage) {
        val total = Math.max(max, committed)
        val portion = used / total
        val remaining = total - used
        return when {
            (remaining < (1024 * 1024 * 200) || portion > 0.9) -> Color.RED
            (remaining < (1024 * 1024 * 400) || portion > 0.8) -> Color.YELLOW
            else -> Color.GREEN
        }
    }

    private fun getVRAMText(): String = "  Free VRAM: ${asString(gpuInfo.getFreeVRAM())}"

    private fun getVRAMColor(usage: GPUInfo): Color = with(usage)
    {
        val remaining = getFreeVRAM()
        return when {
            remaining <= 0 -> Color.RED
            remaining < 1024 * 1024 * 50 -> Color.ORANGE
            remaining < 1024 * 1024 * 100 -> Color.YELLOW
            else -> Color.GREEN
        }
    }

    private fun getCurrentTarget(): String {
        if (context.isInCampaign)
            return "Target: " + (context.entityInteractedWith?.name ?: "none")

        return ""
    }

    private fun checkInput() {
        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
            isOpen = false
            return
        }

        // Mouse wheel scrolling
        val scrollY = Mouse.getDWheel()
        scrollOffset -= scrollY * .2f

        val ctrlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
                Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
        val shiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ||
                Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
        //val altDown = isKeyDown(KEY_LMETA) || isKeyDown(KEY_RMETA)
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
                if (keyPressed == Keyboard.KEY_UP && lastCommand != null) {
                    lastInput = currentInput.toString()
                    lastIndex = currentIndex
                    currentInput.replace(0, currentInput.length, lastCommand)
                    currentIndex = currentInput.length
                    continue
                }

                // Down restores previous command overwritten by up
                if (keyPressed == Keyboard.KEY_DOWN && lastInput != null) {
                    currentInput.replace(0, currentInput.length, lastInput)
                    currentIndex = lastIndex
                    lastInput = null
                    lastIndex = currentInput.length
                    continue
                }

                // Tab auto-completes the current command
                if (keyPressed == Keyboard.KEY_TAB) {
                    // Get just the current command (separator support complicates things)
                    val startIndex = currentInput.lastIndexOf(ConsoleSettings.commandSeparator, currentIndex) + 1
                    val tmp = currentInput.indexOf(ConsoleSettings.commandSeparator, startIndex)
                    val endIndex = if (tmp < 0) currentInput.length else tmp
                    val toIndex = currentInput.substring(startIndex, Math.max(startIndex, currentIndex))
                    val fullCommand = currentInput.substring(startIndex, endIndex)

                    // Only auto-complete if arguments haven't been entered
                    if (' ' in fullCommand || '\n' in fullCommand) {
                        currentInput.insert(currentIndex, '\t')
                        currentIndex++
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
                                || (!shiftDown && command.compareTo(fullCommand, true) > 0)
                            ) {
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
                if (keyPressed == Keyboard.KEY_LEFT) {
                    currentIndex = Math.max(0, currentIndex - 1)
                    continue
                } else if (keyPressed == Keyboard.KEY_RIGHT) {
                    currentIndex = Math.min(currentInput.length, currentIndex + 1)
                    continue
                } else if (keyPressed == Keyboard.KEY_HOME) {
                    currentIndex = 0
                    continue
                } else if (keyPressed == Keyboard.KEY_END) {
                    currentIndex = currentInput.length
                    continue
                }

                // PageUp/Down; scroll an entire page at once
                if (keyPressed == Keyboard.KEY_PRIOR) {
                    if (ctrlDown || shiftDown)
                        scrollOffset = minScroll
                    else
                        scrollOffset -= height - (80 + fontSize)
                } else if (keyPressed == Keyboard.KEY_NEXT) {
                    if (ctrlDown || shiftDown)
                        scrollOffset = 0f
                    else
                        scrollOffset += height - (80 + fontSize)
                }

                // Backspace handling; imitates vanilla text inputs
                if (keyPressed == Keyboard.KEY_BACK && currentIndex > 0) {
                    // Control+backspace, delete last word
                    if (ctrlDown) {
                        // Positional editing support
                        val lastSpace = currentInput.substring(0, currentIndex)
                            .lastIndexOfAny(listOf(" ", ConsoleSettings.commandSeparator))
                        if (lastSpace == -1) currentInput.delete(0, currentIndex)
                        else currentInput.delete(lastSpace, currentIndex)
                    } else { // Regular backspace, delete last character
                        currentInput.deleteCharAt(currentIndex - 1)
                    }
                } // Delete key handling
                else if (keyPressed == Keyboard.KEY_DELETE && currentIndex < currentInput.length) {
                    currentInput.deleteCharAt(currentIndex)
                    currentIndex++
                }
                // Return key handling
                else if (keyPressed == Keyboard.KEY_RETURN) {
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
                else if (keyPressed == Keyboard.KEY_V && ctrlDown && Sys.getClipboard() != null) {
                    val pasted = Sys.getClipboard() ?: continue
                    currentInput.insert(currentIndex, pasted)
                }
                // Normal typing
                else {
                    val char = Keyboard.getEventCharacter()
                    if (char.code in 0x20..0x7e) {
                        currentInput.insert(currentIndex, char)
                    }
                }

                // Update cursor index based on what changed since last input
                currentIndex += currentInput.length - previousLength
                currentIndex = Math.min(Math.max(0, currentIndex), currentInput.length)
            } catch (ex: ArrayIndexOutOfBoundsException) {
                Console.showMessage("Something went wrong with the input parser!" + "\nPlease send a copy of starsector.log to LazyWizard.")
                Log.error(
                    "Input dump:\n - Current input: $currentInput | Index:" +
                            " $currentIndex/${currentInput.length}\n - Last input: ${lastInput ?: "null"}" +
                            " | Index: $lastIndex/${lastInput?.length}", ex
                )
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
        minScroll = (-scrollback.height + (maxY - minY)).coerceAtMost(0f)
        scrollOffset = scrollOffset.coerceIn(minScroll, 0f)

        // Only update our DrawableStrings when there's actually been a change
        if (needsTextUpdate) {
            needsTextUpdate = false
            val cursor = if (showCursor) "|" else " "

            // Show blinking cursor in the proper position
            if (currentIndex == currentInput.length) input.text = "$currentInput$cursor"
            else input.text = "${currentInput.substring(0, currentIndex)}$cursor${currentInput.substring(currentIndex)}"

            if (ConsoleSettings.showCursorIndex) input.append(" | Index: $currentIndex/${currentInput.length}")
            if (ConsoleSettings.showMemoryUsage) {
                ramText.text = getRAMText()
                ramText.baseColor = getRAMColor(memory.heapMemoryUsage)
                vramText.text = getVRAMText()
                vramText.baseColor = getVRAMColor(gpuInfo)
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
        if (ConsoleSettings.showBackground) {
            // If the game crashes drawing the background, disable it on future runs
            if (firstFrame)
                ConsoleSettings.showBackground = false

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

            // No crash on first frame, undo crash fallback
            if (firstFrame) {
                firstFrame = false
                ConsoleSettings.showBackground = true
            }
        }

        val inputHeight = Math.max(fontSize, input.height)
        val memWidth = Math.max(ramText.width, vramText.width)

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
        if (ConsoleSettings.showMemoryUsage) {
            ramText.draw(50f, height - fontSize)
            vramText.draw(50f, height - fontSize * 2)
        }
        if (Global.getSettings().isDevMode) devMode.draw(maxX - (50f + devMode.width), height - fontSize)
        curContext.draw(Math.max(150f + memWidth, (width / 2f) - (curContext.width / 2f)), height - fontSize)
        curTarget.draw(Math.max(150f + memWidth, (width / 2f) - (curTarget.width / 2f)), height - fontSize * 2)

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
