@file:JvmName("FontLoader")

package org.lazywizard.console.overlay

import com.fs.starfarer.api.Global
import org.apache.log4j.Logger
import java.io.IOException
import java.util.*

const val METADATA_LENGTH = 51
const val CHARDATA_LENGTH = 21
const val KERNDATA_LENGTH = 7

// TODO: Write a proper file parser (though this works fine for now)
val SPLIT_REGEX = """=|\s+(?=([^"]*"[^"]*")*[^"]*$)""".toRegex()
val Log: Logger = Logger.getLogger(LazyFont::class.java)

// File format documentation: http://www.angelcode.com/products/bmfont/doc/file_format.html
// TODO: Rewrite parsing code to be more robust and presentable
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

        val fontName = metadata[2].replace("\"", "")
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
            // TODO: See if we need to write our own loader to handle texture parameters
            Global.getSettings().loadTexture(imgFile)
            val texture = Global.getSettings().getSprite(imgFile)
            textureId = texture.textureId
            textureWidth = texture.width
            textureHeight = texture.height
        } catch (ex: IOException) {
            throw RuntimeException("Failed to load texture atlas '$imgFile'", ex)
        }

        val font = LazyFont(fontName, baseHeight,
                textureId, textureWidth, textureHeight)

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

            font.addChar(Integer.parseInt(charData[2]), // id
                    Integer.parseInt(charData[4]), // tx
                    Integer.parseInt(charData[6]), // ty
                    Integer.parseInt(charData[8]), // width
                    Integer.parseInt(charData[10]), // height
                    Integer.parseInt(charData[12]), // xOffset
                    Integer.parseInt(charData[14]), // yOffset
                    Integer.parseInt(charData[16])) // advance
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
            font.getChar(id.toChar()).addKerning(otherId, kernAmount)
        }

        return font
    } catch (ex: NumberFormatException) {
        throw FontException("Failed to parse font at '$fontPath'", ex)
    }
}

class FontException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}