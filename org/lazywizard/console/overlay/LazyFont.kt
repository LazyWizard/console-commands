package org.lazywizard.console.overlay

import org.apache.log4j.Logger
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.opengl.GL11.*
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

class LazyFont(val fontName: String, val baseHeight: Float, val textureId: Int,
               val textureWidth: Float, val textureHeight: Float) {
    private val Log: Logger = Logger.getLogger(LazyFont::class.java)
    private val lookupTable: Array<LazyChar?> = arrayOfNulls(224)
    private val extendedChars = HashMap<Char, LazyChar>()

    fun addChar(id: Int, tx: Int, ty: Int, width: Int, height: Int, xOffset: Int, yOffset: Int, advance: Int) {
        val tmp = LazyChar(id, tx, ty, width, height, xOffset, yOffset, advance)
        if (tmp.id in 32..255)
            lookupTable[tmp.id - 32] = tmp
        else
            extendedChars[tmp.id.toChar()] = tmp
    }


    fun getChar(character: Char): LazyChar {
        val ch: LazyChar? = if (character.toInt() in 32..255) lookupTable[character.toInt() - 32] else extendedChars[character]
        return ch ?: getChar('?')
    }

    fun discard() = glDeleteTextures(textureId)

    private fun drawText(text: String?, x: Float, y: Float, size: Float,
                         maxWidth: Float, maxHeight: Float, color: Color): Vector2f {
        if (text == null || text.isEmpty()) {
            return Vector2f(0f, 0f)
        }

        glColor(color)
        glBindTexture(GL_TEXTURE_2D, textureId)
        glEnable(GL_TEXTURE_2D)
        glPushMatrix()
        glTranslatef(x, y, 0f)
        glBegin(GL_QUADS)

        var lastChar: LazyChar? = null
        val scaleFactor = size / baseHeight
        var xOffset = 0f
        var yOffset = 0f
        var sizeX = 0f
        var sizeY = size

        // TODO: Colored substring support
        for (tmp in text.toCharArray()) {
            if (tmp == '\n') {
                if (-yOffset + size > maxHeight) {
                    break
                }

                yOffset -= size
                sizeY += size
                sizeX = Math.max(sizeX, xOffset)
                xOffset = 0f
                lastChar = null
                continue
            }

            val ch = getChar(tmp)
            val kerning = ch.getKerning(lastChar) * scaleFactor
            val advance = kerning + ch.advance * scaleFactor
            val chWidth = ch.width * scaleFactor
            val chHeight = ch.height * scaleFactor

            if (xOffset + advance > maxWidth) {
                if (-yOffset + size > maxHeight) {
                    return Vector2f(sizeX, sizeY)
                }

                yOffset -= size
                sizeY += size
                sizeX = Math.max(sizeX, xOffset)
                xOffset = -kerning
                lastChar = null
            }

            val localX = xOffset + kerning + ch.xOffset * scaleFactor
            val localY = yOffset - ch.yOffset * scaleFactor

            glTexCoord2f(ch.tx1, ch.ty1)
            glVertex2f(localX, localY)
            glTexCoord2f(ch.tx1, ch.ty2)
            glVertex2f(localX, localY - chHeight)
            glTexCoord2f(ch.tx2, ch.ty2)
            glVertex2f(localX + chWidth, localY - chHeight)
            glTexCoord2f(ch.tx2, ch.ty1)
            glVertex2f(localX + chWidth, localY)

            xOffset += advance
            lastChar = ch
        }

        glEnd()
        glPopMatrix()
        glDisable(GL_TEXTURE_2D)

        sizeX = Math.max(sizeX, xOffset)
        return Vector2f(sizeX, sizeY)
    }

    fun createText(text: String, size: Float, maxWidth: Float = Float.MAX_VALUE, maxHeight: Float = Float.MAX_VALUE,
                   color: Color): DrawableString = DrawableString(text, size, maxWidth, maxHeight, color)

    inner class LazyChar(val id: Int, tx: Int, ty: Int, val width: Int, val height: Int,
                         val xOffset: Int, val yOffset: Int, val advance: Int) {
        val kernings = HashMap<Int, Int>()
        // Internal texture coordinates
        val tx1: Float = tx / textureWidth
        val tx2: Float = tx1 + (width / textureWidth)
        val ty1: Float = (textureHeight - ty) / textureHeight
        val ty2: Float = ty1 - (height / textureHeight)

        fun setKerning(otherChar: Int, kerning: Int) = {
            kernings[otherChar] = kerning
        }

        fun getKerning(otherChar: LazyChar?): Int {
            if (otherChar == null)
                return 0

            return kernings.getOrElse(otherChar.id, { 0 })
        }
    }

    inner class DrawableString(text: String, size: Float, maxWidth: Float, maxHeight: Float, color: Color) {
        private val sb: StringBuilder = StringBuilder(text)
        private val displayListId: Int = glGenLists(1)
        private var needsRebuild = true
        var disposed = false
            private set
        var width: Float = 0f
            private set
        var height: Float = 0f
            private set
        var fontSize: Float = size
            set(value) {
                if (value != field) {
                    field = value
                    needsRebuild = true
                }
            }
        var maxWidth: Float = maxWidth
            set(value) {
                field = value
                needsRebuild = true
            }
        var maxHeight: Float = maxHeight
            set(value) {
                field = value
                needsRebuild = true
            }
        var color: Color = color
            set(value) {
                field = value
                needsRebuild = true
            }
        var text: String
            get() = sb.toString()
            set(value) {
                sb.setLength(0)
                appendText(text)
            }

        fun appendText(text: String) {
            sb.append(text)
            needsRebuild = true
        }

        private fun buildString() {
            glNewList(displayListId, GL_COMPILE)
            val tmp: Vector2f = drawText(text, 0.01f, 0.01f, fontSize, maxWidth, maxHeight, color)
            glEndList()

            width = tmp.x
            height = tmp.y
            needsRebuild = false
        }

        fun draw(x: Float, y: Float) {
            if (disposed) throw RuntimeException("Tried to draw using a disposed DrawableString!")
            if (needsRebuild) buildString()

            glPushMatrix()
            glTranslatef(x, y, 0f)
            glCallList(displayListId)
            glPopMatrix()
        }

        fun draw(location: Vector2f) = draw(location.x, location.y)

        private fun releaseResources() = glDeleteLists(displayListId, 1)

        fun dispose() {
            if (!disposed) releaseResources()
        }

        fun finalize() {
            if (!disposed) {
                Log.warn("DrawableString was not disposed of properly!")
                releaseResources()
            }
        }
    }
}