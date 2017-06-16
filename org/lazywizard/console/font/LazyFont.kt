@file:JvmName("FontLoader")

package org.lazywizard.console.font

import com.fs.starfarer.api.Global
import org.apache.log4j.Logger
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.opengl.GL11.*
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.io.IOException
import java.util.*

private const val METADATA_LENGTH = 51
private const val CHARDATA_LENGTH = 21
private const val KERNDATA_LENGTH = 7

// TODO: Write a proper file parser (though this works fine for now)
private val SPLIT_REGEX = """=|\s+(?=([^"]*"[^"]*")*[^"]*$)""".toRegex()
private val Log: Logger = Logger.getLogger(LazyFont::class.java)

// File format documentation: http://www.angelcode.com/products/bmfont/doc/file_format.html
// FIXME: Slight kerning issues (add a single pixel?)
@Throws(FontException::class)
fun loadFont(fontPath: String): LazyFont {
    // Load the font file contents for later parsing
    var header: String = ""
    val charLines = ArrayList<String>()
    val kernLines = ArrayList<String>()
    try {
        Scanner(Global.getSettings().openStream(fontPath)).use { reader ->
            // Store header with font metadata
            header = "${reader.nextLine()} ${reader.nextLine()} ${reader.nextLine()}"
            while (reader.hasNextLine()) {
                val line = reader.nextLine()
                if (line.startsWith("char ")) { // Character data
                    charLines.add(line)
                } else if (line.startsWith("kerning ")) { // Kerning data
                    kernLines.add(line)
                }
            }
        }
    } catch (ex: IOException) {
        throw RuntimeException("Failed to load font at '$fontPath'", ex)
    }

    // Finally parse the file data we retrieved at the beginning of the constructor
    try {
        // TODO: Parse and store ALL font metadata
        val metadata = header.split(SPLIT_REGEX).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (metadata.size != METADATA_LENGTH) {
            Log.error("Metadata length mismatch: " + metadata.size
                    + " vs expected length of " + METADATA_LENGTH + ".")
            Log.error("Input string: " + header)
            throw FontException("Metadata length mismatch")
        }

        //val fontName = metadata[2].replace("\"", "")
        val baseHeight = java.lang.Float.parseFloat(metadata[27])

        // Get image file path from metadata
        val dirIndex = fontPath.lastIndexOf("/")
        val imgFile = (if (dirIndex == -1)
            fontPath
        else
            fontPath.substring(0, dirIndex + 1)) + metadata[50].replace("\"", "")

        // Load the font image into a texture
        // TODO: Add support for multiple image files; 'pages' in the font file
        val textureId: Int
        val textureWidth: Float
        val textureHeight: Float
        try {
            Global.getSettings().loadTexture(imgFile)
            val texture = Global.getSettings().getSprite(imgFile)
            textureId = texture.textureId
            textureWidth = texture.width
            textureHeight = texture.height
        } catch (ex: IOException) {
            throw RuntimeException("Failed to load texture atlas '$imgFile'", ex)
        }

        val font = LazyFont(textureId, baseHeight, textureWidth, textureHeight)

        // Parse character data and place into a quick lookup table or extended character map
        for (charLine in charLines) {
            val charData = charLine.split(SPLIT_REGEX).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (charData.size != CHARDATA_LENGTH) {
                Log.error("Character data length mismatch: "
                        + charData.size + " vs expected length of "
                        + CHARDATA_LENGTH + ".")
                Log.error("Input string: " + charLine)
                throw FontException("Character data length mismatch")
            }

            font.addChar(id = Integer.parseInt(charData[2]),
                    tx = Integer.parseInt(charData[4]),
                    ty = Integer.parseInt(charData[6]),
                    width = Integer.parseInt(charData[8]),
                    height = Integer.parseInt(charData[10]),
                    xOffset = Integer.parseInt(charData[12]),
                    yOffset = Integer.parseInt(charData[14]),
                    advance = Integer.parseInt(charData[16]))
            //Integer.parseInt(data[18]), // page
            // Integer.parseInt(data[20])); // channel
        }

        // Parse and add kerning data
        for (kernLine in kernLines) {
            val kernData = kernLine.split(SPLIT_REGEX).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (kernData.size != KERNDATA_LENGTH) {
                Log.error("Kerning data length mismatch: "
                        + kernData.size + " vs expected length of "
                        + KERNDATA_LENGTH + ".")
                Log.error("Input string: " + kernLine)
                throw FontException("Kerning data length mismatch")
            }

            val id = Integer.parseInt(kernData[4])
            val otherId = Integer.parseInt(kernData[2])
            val kernAmount = Integer.parseInt(kernData[6])
            font.getChar(id.toChar()).setKerning(otherId, kernAmount)
        }

        return font
    } catch (ex: NumberFormatException) {
        throw FontException("Failed to parse font at '$fontPath'", ex)
    }
}

class LazyFont(val textureId: Int, val baseHeight: Float, val textureWidth: Float, val textureHeight: Float) {
    private val lookupTable: Array<LazyChar?> = arrayOfNulls(224)
    private val extendedChars = HashMap<Char, LazyChar>()

    fun addChar(id: Int, tx: Int, ty: Int, width: Int, height: Int, xOffset: Int, yOffset: Int, advance: Int) {
        val tmp = LazyChar(id, tx, ty, width, height, xOffset, yOffset, advance)
        if (tmp.id in 32..255) lookupTable[tmp.id - 32] = tmp
        else extendedChars[tmp.id.toChar()] = tmp
    }


    fun getChar(character: Char): LazyChar {
        val ch: LazyChar? = if (character.toInt() in 32..255) lookupTable[character.toInt() - 32] else extendedChars[character]
        return ch ?: getChar('?')
    }

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
                // TODO: Don't overwrite if set
                //lastChar = null
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

    fun createText(text: String, color: Color, size: Float = baseHeight, maxWidth: Float = Float.MAX_VALUE,
                   maxHeight: Float = Float.MAX_VALUE): DrawableString = DrawableString(text, size, maxWidth, maxHeight, color)

    inner class LazyChar(val id: Int, tx: Int, ty: Int, val width: Int, val height: Int,
                         val xOffset: Int, val yOffset: Int, val advance: Int) {
        val kernings = HashMap<Int, Int>()
        // Internal texture coordinates
        val tx1: Float = tx / textureWidth
        val tx2: Float = tx1 + (width / textureWidth)
        val ty1: Float = (textureHeight - ty) / textureHeight
        val ty2: Float = ty1 - (height / textureHeight)

        fun setKerning(otherChar: Int, kerning: Int) {
            kernings[otherChar] = kerning
        }

        fun getKerning(otherChar: LazyChar?): Int {
            if (otherChar == null)
                return 0

            return kernings.getOrElse(otherChar.id, { 0 })
        }
    }

    inner class DrawableString(text: String, fontSize: Float, maxWidth: Float, maxHeight: Float, color: Color) {
        private val sb: StringBuilder = StringBuilder(text)
        private val displayListId: Int = glGenLists(1)
        private var needsRebuild = true
        var disposed = false
            private set
        var width: Float = 0f
            private set
        var height: Float = 0f
            private set
        var fontSize: Float = fontSize
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
                appendText(value)
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

class FontException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}