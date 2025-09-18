package org.lazywizard.console.overlay.v2.font

import com.fs.starfarer.api.Global
import org.apache.log4j.Logger
import org.lazywizard.lazylib.LazyLib
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.opengl.ColorUtils.glColor
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.io.IOException
import java.net.URI
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

//Copy to fix issue with coloring

internal class ConsoleFont private constructor(
    val fontName: String,
    val textureId: Int,
    val baseHeight: Float,
    val textureWidth: Float,
    val textureHeight: Float
) {
    // Quick array lookup for characters 32-255 (standard ASCII range)
    private val lookupTable: Array<LazyChar?> = arrayOfNulls(224)

    // Much slower map-based lookup for international characters (256+ in Unicode)
    private val extendedChars = HashMap<Char, LazyChar>()

    // Fallback character for when a character isn't defined in the font
    private lateinit var fallbackChar: LazyChar

    // File format documentation: http://www.angelcode.com/products/bmfont/doc/file_format.html
    companion object FontLoader {
        // These are used for validating read data
        private const val METADATA_LENGTH = 51
        private const val CHARDATA_LENGTH = 21
        private const val KERNDATA_LENGTH = 7

        // TODO: Write a proper file parser (this works fine for now, but requires an unmaintainable mess of magic offset numbers)
        private val SPLIT_REGEX = """=|\s+(?=([^"]*"[^"]*")*[^"]*$)""".toRegex()
        private val Log: Logger = Logger.getLogger(ConsoleFont::class.java)
        private val fontCache = HashMap<String, ConsoleFont>()

        @JvmStatic
        @Throws(FontException::class)
        fun loadFont(fontPath: String): ConsoleFont {
            val canonPath = URI(fontPath).normalize().path
            if (!canonPath.equals(fontPath)) Log.debug("Converted font path: $fontPath -> $canonPath")
            if (fontCache.contains(canonPath)) return fontCache.getValue(canonPath)

            // Load the font file contents for later parsing
            var header: String
            val charLines = ArrayList<String>()
            val kernLines = ArrayList<String>()
            try {
                Scanner(Global.getSettings().openStream(canonPath)).use { reader ->
                    // Store header with font metadata
                    header = "${reader.nextLine()} ${reader.nextLine()} ${reader.nextLine()}"

                    // Read raw font data
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
                throw FontException("Failed to load font at '$canonPath'", ex)
            }

            // Parse the file data we retrieved earlier and convert it into something usable
            try {
                // TODO: Parse and store ALL font metadata
                val metadata = header.split(SPLIT_REGEX).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (metadata.size != METADATA_LENGTH) {
                    Log.error("Metadata length mismatch: ${metadata.size} vs expected length of $METADATA_LENGTH.")
                    Log.error("Input string: $header")
                    throw FontException("Metadata length mismatch in '$canonPath'")
                }

                val fontName = metadata[2].replace("\"", "")
                val baseHeight = java.lang.Float.parseFloat(metadata[27])

                // Get image file path from metadata
                val dirIndex = canonPath.lastIndexOf("/")
                val imgFile = (if (dirIndex == -1)
                    canonPath
                else
                    canonPath.substring(0, dirIndex + 1)) + metadata[50].replace("\"", "")

                // Load the font image into a texture
                // TODO: Add support for multiple image files; 'pages' in the font file
                // (this is a low priority as no vanilla font uses multiple pages)
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
                    throw FontException("Failed to load texture atlas '$imgFile'", ex)
                }

                val scaleW = java.lang.Float.parseFloat(metadata[31])
                val scaleH = java.lang.Float.parseFloat(metadata[33])
                val font = ConsoleFont(fontName, textureId, baseHeight, scaleW, scaleH)
                Log.debug("Created empty font ${font.fontName} from $fontPath, preparing to add character data")

                // Parse character data and place into a quick lookup table or extended character map
                for (charLine in charLines) {
                    val charData = charLine.split(SPLIT_REGEX).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (charData.size != CHARDATA_LENGTH) {
                        Log.error("Character data length mismatch: ${charData.size} vs expected length of $CHARDATA_LENGTH.")
                        Log.error("Input string: $charLine")
                        throw FontException("Character data length mismatch in '$canonPath'")
                    }

                    // Register each character with the parent font
                    font.addChar(
                        // What character this is in Unicode
                        id = Integer.parseInt(charData[2]),
                        // The leftmost position of the character in the underlying texture
                        tx = Integer.parseInt(charData[4]),
                        // The topmost position of the character in the underlying texture
                        ty = Integer.parseInt(charData[6]),
                        // The width of the character in the underlying texture
                        width = Integer.parseInt(charData[8]),
                        // The height of the character in the underlying texture
                        height = Integer.parseInt(charData[10]),
                        // Extra empty space before character when displaying it
                        xOffset = Integer.parseInt(charData[12]),
                        // Extra empty space above character when displaying it
                        yOffset = Integer.parseInt(charData[14]),
                        // How much horizontal space this character takes up, including offset and padding
                        advance = Integer.parseInt(charData[16]) + 1
                        // Which subtexture this character's image is stored on (NOT IMPLEMENTED; ASSUMES 0)
                        //page = Integer.parseInt(data[18]),
                        // Which texture channel this character is encoded in (NOT IMPLEMENTED; ASSUMES 15)
                        //channel = Integer.parseInt(data[20]));
                    )
                }

                // If the font data does not define a space character, define it manually
                // We take the bottom right pixel and create a 1x1 character followed by a lengthy advance
                // TODO: TEST THIS!
                if (font.lookupTable[' '.code - 32] == null) {
                    Log.warn("Font ${font.fontName} does not define a space character! Creating one manually...")
                    font.addChar(
                        id = 32,
                        tx = textureWidth.toInt() - 1,
                        ty = textureHeight.toInt() - 1,
                        width = 1,
                        height = font.baseHeight.toInt() - 1,
                        xOffset = 0,
                        yOffset = 0,
                        // Use average advance to support monospaced fonts
                        // TODO: test that average advance looks right on regular fonts
                        advance = font.lookupTable.mapNotNull { it?.advance }.average().toInt()
                        //advance = (font.baseHeight / 2f).toInt() + 1) // FIXME: Not correct for monospaced fonts
                    )
                }

                // Displayed in place of any character that isn't defined in this font
                font.fallbackChar = (font.lookupTable['?'.code - 32] ?: font.lookupTable[' '.code - 32])!!

                // Parse and add kerning data
                for (kernLine in kernLines) {
                    val kernData = kernLine.split(SPLIT_REGEX).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (kernData.size != KERNDATA_LENGTH) {
                        Log.error("Kerning data length mismatch: ${kernData.size} vs expected length of $KERNDATA_LENGTH.")
                        Log.error("Input string: $kernLine")
                        throw FontException("Kerning data length mismatch in '$canonPath'")
                    }

                    val id = Integer.parseInt(kernData[4])
                    val otherId = Integer.parseInt(kernData[2])
                    val kernAmount = Integer.parseInt(kernData[6])
                    font.getChar(id.toChar()).setKerning(otherId, kernAmount)
                }

                Log.debug("Finished initializing character data for font ${font.fontName}")

                fontCache[canonPath] = font
                return font
            } catch (ex: NumberFormatException) {
                throw FontException("Failed to parse font at '$canonPath'", ex)
            }
        }
    }

    // Needed because GPU buffer cleanup must happen in the main thread
    private object MemoryHandler {
        private val toClean = Collections.synchronizedList(ArrayList<Int>())

        // Called by the garbage collection thread when collected
        fun registerForRemoval(id: Int) {
            Log.debug("Marked DrawableString $id for removal")
            toClean.add(id)
        }

        // Called by the main thread periodically to free up old buffers
        fun checkClean() {
            if (toClean.isNotEmpty()) {
                synchronized(toClean) {
                    for (id in toClean) {
                        Log.debug("Deleting buffer $id automatically")
                        glDeleteBuffers(id)
                    }

                    toClean.clear()
                }
            }
        }
    }

    private fun addChar(id: Int, tx: Int, ty: Int, width: Int, height: Int, xOffset: Int, yOffset: Int, advance: Int) {
        val tmp = LazyChar(id, tx, ty, width, height, xOffset, yOffset, advance)
        if (tmp.id in 32..255) lookupTable[tmp.id - 32] = tmp
        else extendedChars[tmp.id.toChar()] = tmp
    }

    private val warnedChars: MutableSet<Char> = mutableSetOf()
    fun getChar(character: Char): LazyChar {
        val ch: LazyChar? =
            if (character.code in 32..255) lookupTable[character.code - 32] else extendedChars[character]
        if (ch != null) return ch

        // Replace Unicode smart characters with their basic equivalents
        // DO NOT put basic characters here or you could create an infinite loop in getChar()!
        when (character) {
            // Smart single quotes
            '\u2018', '\u2019', '\u201A', '\u201B' -> return getChar('\'')
            // Smart double quotes
            '\u201c', '\u201d', '\u201e', '\u201f' -> return getChar('"')
        }

        // Only warn once per character (fixes log spam when modders create a new DrawableString each frame)
        if (!character.isWhitespace() && character !in warnedChars) {
            Log.warn("Character '$character' is not defined in font data")
            warnedChars.add(character)
        }

        // If the character isn't in the defined set, return a question mark (or a
        // blank space if the font doesn't define a question mark, either)
        return fallbackChar
    }

    override fun toString() = "$fontName (texture id: $textureId)"

    private fun calcTabWidth(curWidth: Float, fontSize: Float) =
        (ceil((curWidth + 0.001f) / (fontSize * 2f)) * (fontSize * 2f)) - curWidth

    fun calcWidth(rawLine: String, fontSize: Float): Float {
        val scaleFactor = fontSize / baseHeight
        var lastChar: LazyChar? = null
        var curWidth = 0f
        for (tmp in rawLine) {
            if (tmp == '\t') {
                curWidth += calcTabWidth(curWidth, fontSize)
                lastChar = null
                continue
            }

            val ch = getChar(tmp)
            val kerning = if (lastChar != null) ch.getKerning(lastChar.id).toFloat() else 0f
            curWidth += (kerning + ch.advance) * scaleFactor
            lastChar = ch
        }

        return curWidth
    }

    fun buildUntilLimit(rawLine: String, fontSize: Float, maxWidth: Float): String {
        if (rawLine.isBlank() || maxWidth <= 0f) return ""

        val scaleFactor = fontSize / baseHeight
        var lastChar: LazyChar? = null
        var curChar = 0
        var curWidth = 0f
        for (tmp in rawLine) {
            if (tmp == '\t') {
                curWidth += calcTabWidth(curWidth, fontSize)
                lastChar = null
                continue
            }

            val ch = getChar(tmp)
            val kerning = if (lastChar != null) ch.getKerning(lastChar.id).toFloat() else 0f
            val width = (kerning + ch.advance) * scaleFactor

            if (curWidth + width > maxWidth) {
                return rawLine.take(curChar)
            }

            curWidth += width
            curChar++
            lastChar = ch
        }

        // Entire line fits
        return rawLine
    }

    @JvmOverloads
    fun wrapString(
        toWrap: String,
        fontSize: Float,
        maxWidth: Float,
        maxHeight: Float = Float.MAX_VALUE,
        indent: Int = 0
    ): String {
        val maxLines = (maxHeight / fontSize).toInt()
        val wrappedString = StringBuilder((toWrap.length * 1.1).toInt())
        var numLines = 0

        // Indentation support
        val indentWith: String = "".padStart(indent)
        val maxWidthWithIndent = maxWidth - calcWidth(indentWith, fontSize)
        if (maxWidthWithIndent <= 0f) return ""

        outer@
        for (rawLine in toWrap.split('\n')) {
            // Can't write to loop parameters in Kotlin, so we need to do this in a separate loop
            var line = rawLine

            if (line.isBlank()) {
                wrappedString.append('\n')
                numLines++
                continue@outer
            }

            inner@
            while (line.isNotBlank()) {
                // We've reached our limit
                if (numLines >= maxLines) break

                // Calculate the longest substring that will fit maxWidth
                val tmp = buildUntilLimit(line, fontSize, maxWidthWithIndent)

                // Entire line fits within maxWidth
                if (tmp.length == line.length) {
                    wrappedString.append(indentWith).append(tmp).append('\n')
                    numLines++
                    break@inner
                }
                // String needs to be broken up - here's where things get complicated
                else {
                    // If there's whitespace, break at the last occurrence
                    val lastSpace = tmp.lastIndexOf(' ')
                    if (lastSpace != -1) {
                        wrappedString.append(indentWith).append(line.take(lastSpace)).append('\n')
                        line = if (line.length > lastSpace) line.substring(lastSpace + 1) else ""
                        numLines++
                    }
                    // No whitespace means we need to break up the word with a dash
                    else {
                        while (true) {
                            val splitIndex =
                                (buildUntilLimit("-$line", fontSize, maxWidthWithIndent).length - 1)
                                    .coerceAtLeast(0)
                            if (splitIndex < line.length) {
                                wrappedString.append(indentWith).append(line.take(splitIndex)).append("-\n")
                                line = line.substring(splitIndex)
                                numLines++
                                continue@inner
                            } else {
                                wrappedString.append(indentWith).append(line).append('\n')
                                numLines++
                                break@inner
                            }
                        }
                    }
                }
            }
        }

        // Return the wrapped string, minus the extra newline we added at the end
        return wrappedString.substring(0, wrappedString.length - 1)
    }

    @JvmOverloads
    fun createText(
        text: String = "", baseColor: Color = Color.WHITE, size: Float = baseHeight, maxWidth: Float = Float.MAX_VALUE,
        maxHeight: Float = Float.MAX_VALUE
    ): DrawableString = DrawableString(text, size, maxWidth, maxHeight, baseColor)

    inner class LazyChar(
        val id: Int, tx: Int, ty: Int, val width: Int, val height: Int,
        val xOffset: Int, val yOffset: Int, val advance: Int
    ) {
        val kernings: MutableMap<Int, Int> = HashMap()

        // Internal texture coordinates
        val tx1: Float = tx / textureWidth
        val tx2: Float = tx1 + (width / textureWidth)
        val ty1: Float = (textureHeight - ty) / textureHeight
        val ty2: Float = ty1 - (height / textureHeight)

        fun setKerning(otherCharId: Int, kerning: Int) {
            kernings[otherCharId] = kerning
        }

        fun getKerning(otherCharId: Int): Int {
            return kernings.getOrElse(otherCharId) { 0 }
        }

        fun setKerning(otherChar: Char, kerning: Int) {
            kernings[otherChar.code] = kerning
        }

        fun getKerning(otherChar: Char): Int {
            return kernings.getOrElse(otherChar.code) { 0 }
        }

        override fun toString() = id.toChar().toString()
    }

    enum class TextAnchor {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    enum class TextAlignment { LEFT, CENTER, RIGHT }

    // FIXME: Wrapped strings with a hyphen will offset colored substrings by one
    inner class DrawableString(text: String, fontSize: Float, maxWidth: Float, maxHeight: Float, baseColor: Color) {
        private val sb: StringBuilder = StringBuilder(text)
        private val substringColorData = HashMap<Int, FloatArray>()
        private val bufferId = glGenBuffers()
        private var len = 0
        val font: ConsoleFont get() = this@ConsoleFont
        var anchor: TextAnchor = TextAnchor.TOP_LEFT
        var renderDebugBounds = false
        var blendSrc = GL_SRC_ALPHA
        var blendDest = GL_ONE_MINUS_SRC_ALPHA
        var isRebuildNeeded = true
            private set
        var isDisposed = false
            private set
        var alignment: TextAlignment = TextAlignment.LEFT
            set(value) {
                if (value != field) {
                    field = value
                    isRebuildNeeded = true
                }
            }
        var width: Float = 0f
            get() {
                triggerRebuildIfNeeded()
                return field
            }
            private set
        var height: Float = 0f
            get() {
                triggerRebuildIfNeeded()
                return field
            }
            private set
        var fontSize: Float = fontSize
            set(value) {
                if (value != field) {
                    field = value
                    isRebuildNeeded = true
                }
            }
        var maxWidth: Float = maxWidth
            set(value) {
                if (value != field) {
                    field = value
                    isRebuildNeeded = true
                }
            }
        var maxHeight: Float = maxHeight
            set(value) {
                if (value != field) {
                    field = value
                    isRebuildNeeded = true
                }
            }
        var text: String
            get() = sb.toString()
            set(value) {
                sb.setLength(0)
                substringColorData.clear()
                append(value)
            }
        var baseColor: Color = baseColor
            set(value) {
                if (value != field) {
                    field = value
                    if (substringColorData.isNotEmpty()) isRebuildNeeded = true
                }
            }

        // TODO: Remove deprecated method after next Starsector update
        @Deprecated("Use baseColor instead", level = DeprecationLevel.HIDDEN)
        var color: Color
            @Deprecated("Use baseColor instead", level = DeprecationLevel.HIDDEN)
            set(value) {
                LazyLib.onDeprecatedMethodUsage()
                baseColor = value
            }
            @Deprecated("Use baseColor instead", level = DeprecationLevel.HIDDEN)
            get() {
                LazyLib.onDeprecatedMethodUsage()
                return baseColor
            }

        init {
            MemoryHandler.checkClean()
            Log.debug("Buffer $bufferId created")
        }

        fun append(text: Any): DrawableString {
            sb.append(text)
            isRebuildNeeded = true
            return this
        }

        fun append(text: Any, color: Color): DrawableString {
            // Set indices for color data
            // TODO: Handle invisible characters that mess up length
            substringColorData[sb.length+1] = color.getRGBComponents(null)
            append(text)
            substringColorData[sb.length+1] = this.baseColor.getRGBComponents(null)
            return this
        }

        fun appendIndented(text: Any, indent: Int): DrawableString =
            append(wrapString(text.toString(), fontSize, maxWidth, maxHeight, indent))

        fun appendIndented(text: Any, color: Color, indent: Int): DrawableString =
            append(wrapString(text.toString(), fontSize, maxWidth, maxHeight, indent), color)

        @Deprecated(
            "Use triggerRebuildIfNeeded() instead",
            ReplaceWith("triggerRebuildIfNeeded()"),
            DeprecationLevel.HIDDEN
        )
        fun checkRebuild(): Boolean = triggerRebuildIfNeeded()

        fun triggerRebuildIfNeeded(): Boolean {
            if (isDisposed) throw RuntimeException("Tried to draw using a disposed of DrawableString!")
            if (!isRebuildNeeded) return false

            width = 0f
            height = 0f
            if (text.isBlank() || maxHeight < fontSize) {
                isRebuildNeeded = false
                return true
            }

            var lastChar: LazyChar? // For kerning purposes
            val scaleFactor = fontSize / baseHeight
            var xOffset = 0f
            var yOffset = 0f
            var sizeX = 0f
            var sizeY = fontSize
            val toDraw = wrapString(text, fontSize, maxWidth, maxHeight)

            // Don't store per-vertex color data if the entire string is the same color!
            val useColorData = substringColorData.isNotEmpty()
            val buffer = if (useColorData) {
                BufferUtils.createFloatBuffer(toDraw.length * 32)
            } else
                BufferUtils.createFloatBuffer(toDraw.length * 16)

            len = 0 // Length ignoring whitespace; used for vertex data
            var colLen = 0 // Length including whitespace; used for coloring substrings
            var colorBytes = baseColor.getRGBComponents(null)
            var firstLine = true

            // Used for proper placement of center/right-justified text
            val maxWidthForOffset =
                if (alignment == TextAlignment.LEFT) maxWidth
                else calcWidth(toDraw, fontSize)

            outer@
            for (line in toDraw.split('\n')) {
                if (!firstLine) {
                    if (-yOffset + fontSize > maxHeight) {
                        break@outer
                    }

                    yOffset -= fontSize
                    sizeY += fontSize
                    sizeX = max(sizeX, xOffset)
                }

                // Calculate alignment adjustment for the text on this line
                // TODO: Update Javadoc to mention how maxWidth is handled with non-left alignment
                xOffset = when (alignment) {
                    TextAlignment.LEFT -> 0f
                    TextAlignment.CENTER -> (maxWidthForOffset - calcWidth(line, fontSize)) / 2f
                    TextAlignment.RIGHT -> maxWidthForOffset - calcWidth(line, fontSize)
                }
                lastChar = null
                colLen++
                firstLine = false

                inner@
                for (char in line) {
                    // Ignore carriage returns and other unsupported whitespace
                    if (char.isWhitespace() && char != '\n' && char != ' ' && char != '\t')
                        continue@inner

                    // Tab support
                    if (char == '\t') {
                        xOffset += calcTabWidth(xOffset, fontSize)
                        continue@inner
                    }

                    val ch = getChar(char)
                    val kerning = if (lastChar == null) 0f else ch.getKerning(lastChar.id) * scaleFactor
                    val advance = kerning + ch.advance * scaleFactor
                    val chWidth = ch.width * scaleFactor
                    val chHeight = ch.height * scaleFactor

                    val localX = xOffset + kerning + ch.xOffset * scaleFactor
                    val localY = yOffset - ch.yOffset * scaleFactor

                    // Colored substring support
                    if (useColorData && substringColorData.containsKey(colLen))
                        colorBytes = substringColorData[colLen]

                    // Individual puts are faster, but lack bounds checking
                    buffer.put(ch.tx1).put(ch.ty1)
                    buffer.put(localX).put(localY)
                    if (useColorData) buffer.put(colorBytes)
                    buffer.put(ch.tx1).put(ch.ty2)
                    buffer.put(localX).put(localY - chHeight)
                    if (useColorData) buffer.put(colorBytes)
                    buffer.put(ch.tx2).put(ch.ty2)
                    buffer.put(localX + chWidth).put(localY - chHeight)
                    if (useColorData) buffer.put(colorBytes)
                    buffer.put(ch.tx2).put(ch.ty1)
                    buffer.put(localX + chWidth).put(localY)
                    if (useColorData) buffer.put(colorBytes)

                    xOffset += advance
                    lastChar = ch
                    len++
                    colLen++
                }
            }

            // Send vertex, texture coordinate and color data (if any) to GPU
            buffer.flip()
            glBindBuffer(GL_ARRAY_BUFFER, bufferId)
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)

            // Release buffer binding
            glBindBuffer(GL_ARRAY_BUFFER, 0)

            sizeX = max(sizeX, xOffset)
            width = sizeX
            height = sizeY
            isRebuildNeeded = false
            return true
        }

        private fun drawInternal(x: Float, y: Float, angle: Float = 0f) {
            triggerRebuildIfNeeded()

            val useColorData = substringColorData.isNotEmpty()

            glPushAttrib(GL_ALL_ATTRIB_BITS)
            glPushClientAttrib(GL_ALL_CLIENT_ATTRIB_BITS)
            glEnable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(blendSrc, blendDest)
            glEnableClientState(GL_VERTEX_ARRAY)
            glEnableClientState(GL_TEXTURE_COORD_ARRAY)
            if (useColorData) glEnableClientState(GL_COLOR_ARRAY)

            glBindTexture(GL_TEXTURE_2D, textureId)
            glBindBuffer(GL_ARRAY_BUFFER, bufferId)

            // Color data doubles the size of the buffer, so we don't store it
            // unless absolutely necessary (string contains colored substrings)
            if (useColorData) {
                glTexCoordPointer(2, GL_FLOAT, 32, 0)
                glVertexPointer(2, GL_FLOAT, 32, 8)
                glColorPointer(4, GL_FLOAT, 32, 16)
            } else {
                glColor(baseColor)
                glTexCoordPointer(2, GL_FLOAT, 16, 0)
                glVertexPointer(2, GL_FLOAT, 16, 8)
            }

            glBindBuffer(GL_ARRAY_BUFFER, 0)

            // Anchor position (which point to consider the origin when drawing)
            val offset = when (anchor) {
                TextAnchor.TOP_LEFT -> Vector2f(0f, 0f)
                TextAnchor.TOP_CENTER -> Vector2f(-width / 2f, 0f)
                TextAnchor.TOP_RIGHT -> Vector2f(-width, 0f)
                TextAnchor.CENTER_LEFT -> Vector2f(0f, height / 2f)
                TextAnchor.CENTER -> Vector2f(-width / 2f, height / 2f)
                TextAnchor.CENTER_RIGHT -> Vector2f(-width, height / 2f)
                TextAnchor.BOTTOM_LEFT -> Vector2f(0f, height)
                TextAnchor.BOTTOM_CENTER -> Vector2f(-width / 2f, height)
                TextAnchor.BOTTOM_RIGHT -> Vector2f(-width, height)
            }

            glPushMatrix()
            glTranslatef(x + 0.01f, y + 0.01f, 0.01f)
            if (angle != 0f) glRotatef(MathUtils.clampAngle(angle), 0f, 0f, 1f)
            glTranslatef(offset.x, offset.y, 0f)
            glDrawArrays(GL_QUADS, 0, len * 4)

            // Render visual bounds if the debug flag is set
            if (renderDebugBounds) {
                // Draw height/width
                glDisable(GL_TEXTURE_2D)
                glLineWidth(1f)
                glColor(baseColor, 0.3f)
                glBegin(GL_LINE_LOOP)
                glVertex2f(0f, 0f)
                glVertex2f(width, 0f)
                glVertex2f(width, -height)
                glVertex2f(0f, -height)
                glEnd()

                // Draw maxWidth/maxHeight if at least one has been set
                if (maxWidth != Float.MAX_VALUE || maxHeight != Float.MAX_VALUE) {
                    val displayedMaxWidth =
                        if (maxWidth == Float.MAX_VALUE) width
                        else maxWidth
                    val displayedMaxHeight =
                        if (maxHeight == Float.MAX_VALUE) height
                        else maxHeight

                    glColor(baseColor.darker(), 0.5f)
                    glBegin(GL_LINE_LOOP)
                    glVertex2f(0f, 0f)
                    glVertex2f(displayedMaxWidth, 0f)
                    glVertex2f(displayedMaxWidth, -displayedMaxHeight)
                    glVertex2f(0f, -displayedMaxHeight)
                    glEnd()
                }

                // Draw anchor (origin of rendering)
                glColor(Color.RED)
                glBegin(GL_LINES)
                glVertex2f(-offset.x - 3f, -offset.y - 3f)
                glVertex2f(-offset.x + 3f, -offset.y + 3f)
                glVertex2f(-offset.x - 3f, -offset.y + 3f)
                glVertex2f(-offset.x + 3f, -offset.y - 3f)
                glEnd()
            }

            glPopMatrix()
            glPopClientAttrib()
            glPopAttrib()
        }

        fun draw(x: Float, y: Float) = drawInternal(x, y)

        fun draw(location: Vector2f) = drawInternal(location.x, location.y)

        fun drawAtAngle(x: Float, y: Float, angle: Float) = drawInternal(x, y, angle)

        fun drawAtAngle(location: Vector2f, angle: Float) = drawInternal(location.x, location.y, angle)

        fun dispose() {
            if (!isDisposed) {
                Log.debug("Deleting buffer $bufferId manually")
                glDeleteBuffers(bufferId)
            }

            isDisposed = true
        }

        // TODO: Create JRE-dependent subclasses that use sun.misc.Cleaner or JRE9+'s Cleaner to avoid second GC cycle
        // (phantom references don't avoid a second GC cycle before JRE9, so need to use undocumented classes in JRE7)
        protected fun finalize() {
            if (!isDisposed) MemoryHandler.registerForRemoval(bufferId)
        }

        override fun toString() = sb.toString()
    }
}

class FontException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}
