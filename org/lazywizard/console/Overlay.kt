@file:JvmName("ConsoleOverlay")

package org.lazywizard.console

import org.apache.log4j.Logger
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.console.overlay.FontException
import org.lazywizard.console.overlay.LazyFont
import org.lazywizard.console.overlay.loadFont
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

private val Log: Logger = Logger.getLogger(LazyFont::class.java)
private var font = loadFont(Console.getSettings().font)
private val width = Display.getDisplayMode().width.toFloat()
private val height = Display.getDisplayMode().height.toFloat()

@Throws(FontException::class)
fun reloadFont() {
    // FIXME: This breaks Starsector when enabled
    //font.discard()
    //font = loadFont(Console.getSettings().font)
}

fun show(context: CommandContext) {
    ConsoleOverlayInternal(context).show()
}

private class ConsoleOverlayInternal(val context: CommandContext) {
    private val bgTextureId = glGenTextures()
    private val history = font.createText("", font.baseHeight, width - 60f, height - 60f, Console.getSettings().outputColor)
    private val query = font.createText(CommonStrings.INPUT_QUERY, font.baseHeight, width, 30f, Console.getSettings().outputColor.darker())
    private val input = font.createText("", font.baseHeight, width, 45f, Console.getSettings().outputColor)
    private val currentInput: StringBuilder = StringBuilder()
    private var lastInput: String? = null
    private var currentIndex = 0
    private var lastIndex = 0
    private var timeOpen = 0f
    private var shouldShow = true

    fun show() {
        // Save current screen image to texture
        val buffer = BufferUtils.createByteBuffer(width.toInt() * height.toInt() * Display.getDisplayMode().bitsPerPixel / 8)
        glReadPixels(0, 0, width.toInt(), height.toInt(), GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, bgTextureId)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width.toInt(), height.toInt(), 0,
                GL_RGBA, GL_UNSIGNED_BYTE, buffer)
        buffer.clear()

        shouldShow = true
        while (shouldShow) {
            Display.update()
            checkInput()
            render()
            Display.sync(60)
        }

        glDeleteTextures(bgTextureId)
        Keyboard.poll() // Clear any remaining input events
        Keyboard.destroy()
        Keyboard.create()
    }

    private fun checkInput() {
        if (Keyboard.isKeyDown(KEY_ESCAPE)) {
            shouldShow = false
            return
        }

        val ctrlDown = Keyboard.isKeyDown(KEY_LCONTROL) || Keyboard.isKeyDown(KEY_RCONTROL)
        val shiftDown = Keyboard.isKeyDown(KEY_LSHIFT) || Keyboard.isKeyDown(KEY_RSHIFT)
        val altDown = Keyboard.isKeyDown(KEY_LMETA) || Keyboard.isKeyDown(KEY_RMETA)
        val previousLength = currentInput.length
        while (Keyboard.next()) {
            try {
                // Only pay attention to key down events
                if (!Keyboard.getEventKeyState()) continue

                val keyPressed = Keyboard.getEventKey()
                timeOpen = 0f

                // Load last command when user presses up on keyboard
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

                // TODO: Copy TAB support (skipping cause lazy)

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
                        val lastSpace = currentInput.substring(0, currentIndex).lastIndexOf(' ')
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
                    Console.parseInput(command, context)
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
                        + "Please send a copy of starsector.log to LazyWizard.")
                Log.error("Input dump:\n - Current input: $currentInput | Index:" +
                        " $currentIndex/${currentInput.length}\n - Last input: ${lastInput ?: "null"}" +
                        " | Index: $lastIndex/${lastInput?.length}", ex)
                currentIndex = currentInput.length
                lastIndex = 0
            }
        }
    }

    private fun render() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
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

        query.draw(Mouse.getX().toFloat(), Mouse.getY() + 50f)
        input.text = currentInput.toString()
        input.draw(Mouse.getX().toFloat(), Mouse.getY() - 50f)

        // Clear OpenGL flags
        glPopMatrix()
        glMatrixMode(GL_PROJECTION)
        glPopMatrix()
        glPopAttrib()
    }
}