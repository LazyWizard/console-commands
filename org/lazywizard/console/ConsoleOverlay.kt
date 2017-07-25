@file:JvmName("ConsoleOverlay")

package org.lazywizard.console

import com.fs.starfarer.api.Global
import org.apache.log4j.Logger
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.lazylib.StringUtils
import org.lwjgl.BufferUtils
import org.lwjgl.Sys
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Keyboard.*
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
import java.util.*

private val Log = Logger.getLogger(Console::class.java)
private var history = ""
private const val CURSOR_BLINK_SPEED = 0.8f

fun show(context: CommandContext) = with(ConsoleOverlayInternal(context,
        Console.getSettings().outputColor, Console.getSettings().outputColor.darker())) { show(); dispose() }

private class ConsoleOverlayInternal(private val context: CommandContext, mainColor: Color, secondaryColor: Color) : ConsoleListener {
    private val settings = Console.getSettings()
    private val bgTextureId = glGenTextures()
    private val BYTE_FORMAT = DecimalFormat("#,##0.#")
    private val memory = ManagementFactory.getMemoryMXBean()
    private val font = Console.getFont()
    private val width = Display.getWidth() * Display.getPixelScaleFactor()
    private val height = Display.getHeight() * Display.getPixelScaleFactor()
    private val scrollback = font.createText(text = history, color = mainColor, maxWidth = width - 60f)
    private val query = font.createText(text = CommonStrings.INPUT_QUERY, color = secondaryColor, maxWidth = width, maxHeight = 30f)
    private val prompt = font.createText(text = "> ", color = secondaryColor, maxWidth = width, maxHeight = 30f)
    private val input = font.createText(text = "", color = mainColor, maxWidth = width - prompt.width, maxHeight = 45f)
    private val mem = font.createText(text = getMemText(), color = secondaryColor)
    private val devMode = font.createText(text = "DEVMODE", color = secondaryColor)
    private val currentInput = StringBuilder()
    private var lastInput: String? = null
    private var currentIndex = 0
    private var lastIndex = 0
    private var nextBlink = CURSOR_BLINK_SPEED
    private var showCursor = true
    private var needsTextUpdate = true
    private var lastUpdate = Sys.getTime()
    private var isOpen = false

    fun show() {
        // Calculates time elapsed since last frame
        fun calcDelta(): Float {
            val curTime = Sys.getTime()
            val delta = (curTime - lastUpdate).toFloat() / Sys.getTimerResolution()
            lastUpdate = curTime
            return delta
        }

        // Save current screen image to texture
        val buffer = BufferUtils.createByteBuffer(width.toInt() * height.toInt() * 3)
        glReadPixels(0, 0, width.toInt(), height.toInt(), GL_RGB, GL_UNSIGNED_BYTE, buffer)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, bgTextureId)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R3_G3_B2, width.toInt(), height.toInt(), 0, GL_RGB, GL_UNSIGNED_BYTE, buffer)
        buffer.clear()

        // Show overlay until closed by player
        // FIXME: Alt+F4 only closes overlay, not game
        isOpen = true
        lastUpdate = Sys.getTime()
        while (isOpen && !Display.isCloseRequested()) {
            Display.update()
            checkInput()
            advance(calcDelta())
            render()
            Display.sync(40)
        }

        // Clean up texture and clear any remaining input events
        glDeleteTextures(bgTextureId)
        Keyboard.destroy()
        Keyboard.create()
    }

    fun dispose() {
        // Clean up native resources, limit memory used by console scrollback history
        history = scrollback.text.takeLast(ConsoleSettings.maxScrollback)
        scrollback.dispose()
        query.dispose()
        prompt.dispose()
        input.dispose()
        mem.dispose()
        devMode.dispose()
    }

    override fun showOutput(output: String): Boolean {
        if (!isOpen) return false

        scrollback.appendText(StringUtils.wrapString(output, (width / font.baseHeight * 1.8f).toInt()))
        return true
    }

    override fun getContext() = context

    // Based on this StackOverflow answer: https://stackoverflow.com/a/5599842
    private fun asString(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val digits = size / Math.pow(1024.0, digitGroups.toDouble())
        return "${BYTE_FORMAT.format(digits)} ${units[digitGroups]}"
    }

    private fun asString(usage: MemoryUsage): String = with(usage) {
        val percent = DecimalFormat.getPercentInstance().format(used / Math.max(max, committed).toDouble())
        "$percent (${asString(used)}/${asString(Math.max(max, committed))})"
    }

    private fun getMemText(): String {
        return "Memory used: ${asString(memory.heapMemoryUsage)}   |   Non-heap: ${asString(memory.nonHeapMemoryUsage)}"
    }

    private fun checkInput() {
        if (Keyboard.isKeyDown(KEY_ESCAPE)) {
            isOpen = false
            return
        }

        // TODO: Implement scrollback handling using mousewheel, page up/down

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
                nextBlink = CURSOR_BLINK_SPEED

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

                // Tab auto-completes the command
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
                    val commands = CommandStore.getLoadedCommands()
                    Collections.sort(commands)

                    // Reverse order when shift is held down
                    if (shiftDown) Collections.reverse(commands)

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
                    val command = currentInput.toString()
                    if (command.toLowerCase() == "clear") scrollback.text = ""
                    else Console.parseInput(command, context)
                    currentInput.setLength(0)
                    currentIndex = 0
                    lastInput = null
                    lastIndex = 0
                }
                // Paste handling
                else if (keyPressed == KEY_V && ctrlDown && Sys.getClipboard() != null) {
                    currentInput.insert(currentIndex, Sys.getClipboard().replace('\n', ' '))
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
        nextBlink -= amount
        if (nextBlink <= 0f) {
            showCursor = !showCursor
            nextBlink = CURSOR_BLINK_SPEED
            needsTextUpdate = true
        }

        if (needsTextUpdate) {
            val cursor = if (showCursor) "|" else " "

            if (currentIndex == currentInput.length) input.text = "$currentInput$cursor"
            else input.text = "${currentInput.substring(0, currentIndex)}$cursor${currentInput.substring(currentIndex)}"

            if (settings.shouldShowCursorIndex) input.appendText(" | Index: $currentIndex/${currentInput.length}")
            if (settings.shouldShowMemoryUsage) mem.text = getMemText()
        }

        Console.advance(amount, this)
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
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)

        // Draw background
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, bgTextureId)
        glPushMatrix()
        glBegin(GL_QUADS)
        glColor4f(0.2f, 0.2f, 0.2f, 1f)
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

        // Draw scrollback
        // TODO: Add scrollbar, scrolling
        glEnable(GL_STENCIL_TEST)
        glColorMask(false, false, false, false)
        glStencilFunc(GL_ALWAYS, 1, 1)
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE)
        glBegin(GL_QUADS)
        glVertex2f(30f, 50f + font.baseHeight)          // LL
        glVertex2f(30f, height - 30f)                   // UL
        glVertex2f(width - 30f, height - 30f)           // UR
        glVertex2f(width - 30f, 50f + font.baseHeight)  // LR
        glEnd()
        glColorMask(true, true, true, true)
        glStencilFunc(GL_EQUAL, 1, 1)
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
        scrollback.draw(30f, 50f + font.baseHeight + scrollback.height)
        glDisable(GL_STENCIL_TEST)

        // Draw input prompt
        query.draw(30f, 50f)
        prompt.draw(30f, 30f)
        input.draw(30f + prompt.width, 30f)

        // Draw misc stats
        if (settings.shouldShowMemoryUsage) mem.draw(30f, height - font.baseHeight)
        if (Global.getSettings().isDevMode) devMode.draw(width - (30f + devMode.width), height - font.baseHeight)

        // Clear OpenGL flags
        glPopMatrix()
        glMatrixMode(GL_PROJECTION)
        glPopMatrix()
        glPopAttrib()
    }
}