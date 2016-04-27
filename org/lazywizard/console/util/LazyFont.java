package org.lazywizard.console.util;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;
import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;

/**
 * Contains methods to load and draw bitmap fonts. Not thread safe.
 *
 * @author LazyWizard
 * @since 3.0
 */
// Current limitations:
// - Doesn't support multiple font pages
// - Doesn't use most font metadata (not needed for default SS font)
// - Doesn't have multichannel support (not needed for bundled SS fonts)
public class LazyFont
{
    private static final Logger Log = Logger.getLogger(LazyFont.class);
    private static final String FONT_PATH_PREFIX = "graphics/fonts/";
    private static final String SPLIT_REGEX = "=|\\s+(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)";
    private static final Map<String, LazyFont> fonts = new HashMap<>();
    private final String name;
    private final LazyChar[] lookupTable;
    private final Map<Character, LazyChar> extendedChars;
    private final int textureId;
    private final float lineHeight, textureWidth, textureHeight;

    /**
     * Retrieves a font from the font cache, or loads and caches it if it hasn't
     * been requested before.
     *
     * @param fontName The font's name, same as the filename minus the extension
     *                 and the directories leading up to it. Remember that filenames are
     *                 case-sensitive on non-Windows systems.
     *
     * @return A representation of the font ready for rendering.
     *
     * @throws FontException if something goes wrong while loading or parsing
     *                       the font file.
     * @since 3.0
     */
    // TODO: Add consistent way of retrieving the same font (not using filename!)
    public static LazyFont getFont(String fontName) throws FontException
    {
        // Only load a font once, then cache it for subsequent queries
        if (!fonts.containsKey(fontName))
        {
            LazyFont font = new LazyFont(fontName);
            fonts.put(fontName, font);
            return font;
        }

        return fonts.get(fontName);
    }

    // TODO
    public static void discardFont(LazyFont font)
    {
        fonts.remove(font.name);
        glDeleteTextures(font.textureId);
    }

    // File format documentation: http://www.angelcode.com/products/bmfont/doc/file_format.html
    private LazyFont(String fontName) throws FontException
    {
        // Load the font file contents for later parsing
        final String header;
        final List<String> charLines = new ArrayList<>(), kernLines = new ArrayList<>();
        try (final Scanner reader = new Scanner(Global.getSettings().openStream(
                FONT_PATH_PREFIX + fontName + ".fnt")))
        {
            // Store header with font metadata
            header = reader.nextLine() + " " + reader.nextLine() + " " + reader.nextLine();
            while (reader.hasNextLine())
            {
                final String line = reader.nextLine();
                // Character data
                if (line.startsWith("char "))
                {
                    charLines.add(line);
                }
                // Kerning data
                else if (line.startsWith("kerning "))
                {
                    kernLines.add(line);
                }
            }
        }
        catch (IOException ex)
        {
            throw new RuntimeException("Failed to load font '" + fontName + "'", ex);
        }

        // Finally parse the file data we retrieved at the beginning of the constructor
        try
        {
            // TODO: Parse and store ALL font metadata
            final String[] metadata = header.split(SPLIT_REGEX);
            if (metadata.length != 51)
            {
                Log.error("Metadata length mismatch: " + metadata.length
                        + " vs expected length of 51.");
                Log.error("Input string: " + header);
                throw new FontException("Metadata length mismatch");
            }

            name = metadata[2].replace("\"", "");
            lineHeight = Float.parseFloat(metadata[27]);
            final String imgFile = metadata[50].replace("\"", "");

            // Load the font image into a texture
            // TODO: Add support for multiple image files; 'pages' in the font file
            try
            {
                // TODO: See if we need to write our own loader to handle texture parameters
                Global.getSettings().loadTexture(FONT_PATH_PREFIX + imgFile);
                final SpriteAPI texture = Global.getSettings().getSprite(FONT_PATH_PREFIX + imgFile);
                textureId = texture.getTextureId();
                textureWidth = texture.getWidth();
                textureHeight = texture.getHeight();
            }
            catch (IOException ex)
            {
                throw new RuntimeException("Failed to load texture atlas '" + imgFile + "'", ex);
            }

            // Parse character data and place into a quick lookup table or extended character map
            lookupTable = new LazyChar[224];
            extendedChars = new HashMap<>();
            for (String charLine : charLines)
            {
                final String[] charData = charLine.split(SPLIT_REGEX);
                if (charData.length != 21)
                {
                    Log.error("Character data length mismatch: "
                            + charData.length + " vs expected length of 21.");
                    Log.error("Input string: " + charLine);
                    throw new FontException("Character data length mismatch");
                }

                final LazyChar tmp = new LazyChar(
                        Integer.parseInt(charData[2]), // id
                        Integer.parseInt(charData[4]), // tx
                        Integer.parseInt(charData[6]), // ty
                        Integer.parseInt(charData[8]), // width
                        Integer.parseInt(charData[10]), // height
                        Integer.parseInt(charData[12]), // xOffset
                        Integer.parseInt(charData[14]), // yOffset
                        Integer.parseInt(charData[16])); // advance
                //Ingeger.parseInt(data[18]), // page
                // Integer.parseInt(data[20])); // channel

                // Put in the lookup table, or in a map if it's not within standard ASCII range
                if (tmp.id >= 32 && tmp.id <= 255)
                {
                    lookupTable[tmp.id - 32] = tmp;
                }
                else
                {
                    extendedChars.put((char) tmp.id, tmp);
                }
            }

            // Parse and add kerning data
            for (String kernLine : kernLines)
            {
                final String[] kernData = kernLine.split(SPLIT_REGEX);
                if (kernData.length != 7)
                {
                    Log.error("Kerning data length mismatch: "
                            + kernData.length + " vs expected length of 7.");
                    Log.error("Input string: " + kernLine);
                    throw new FontException("Kerning data length mismatch");
                }

                final int id = Integer.parseInt(kernData[4]),
                        otherId = Integer.parseInt(kernData[2]),
                        kernAmount = Integer.parseInt(kernData[6]);
                getChar((char) id).addKerning(otherId, kernAmount);
            }
        }
        catch (NumberFormatException ex)
        {
            throw new FontException("Failed to parse font '" + fontName + "'", ex);
        }
    }

    /**
     * Returns the base height of characters in this font. Characters will look
     * best when drawn at a size evenly divisible by this number.
     *
     * @return The base height of characters in this font, in pixels.
     *
     * @since 3.0
     */
    public float getBaseHeight()
    {
        return lineHeight;
    }

    // TODO: Javadoc this
    public String getFontName()
    {
        return name;
    }

    private LazyChar getChar(char character)
    {
        final LazyChar ch;
        if (character >= 32 && character <= 255)
        {
            ch = lookupTable[character - 32];
        }
        else
        {
            ch = extendedChars.get(character);
        }

        // Let's just hope nobody ever tries a font without question mark support...
        return ((ch == null) ? getChar('?') : ch);
    }

    // Returns dimensions of drawn text
    private Vector2f drawText(String text, float x, float y, float size,
            float maxWidth, float maxHeight, Color color)
    {
        if (text == null || text.isEmpty())
        {
            return new Vector2f(0f, 0f);
        }

        glColor(color);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glEnable(GL_TEXTURE_2D);
        glPushMatrix();
        glTranslatef(x, y, 0f);
        glBegin(GL_QUADS);

        LazyChar lastChar = null;
        final float scaleFactor = size / lineHeight;
        float xOffset = 0f, yOffset = 0f, sizeX = 0f, sizeY = size;

        // TODO: Colored substring support
        for (final char tmp : text.toCharArray())
        {
            if (tmp == '\n')
            {
                if (-yOffset + size > maxHeight)
                {
                    break;
                }

                yOffset -= size;
                sizeY += size;
                sizeX = Math.max(sizeX, xOffset);
                xOffset = 0f;
                lastChar = null;
                continue;
            }

            final LazyChar ch = getChar(tmp);
            final float kerning = ch.getKerning(lastChar) * scaleFactor,
                    advance = kerning + (ch.advance * scaleFactor),
                    chWidth = ch.width * scaleFactor,
                    chHeight = ch.height * scaleFactor;

            if (xOffset + advance > maxWidth)
            {
                if (-yOffset + size > maxHeight)
                {
                    return new Vector2f(sizeX, sizeY);
                }

                yOffset -= size;
                sizeY += size;
                sizeX = Math.max(sizeX, xOffset);
                xOffset = -kerning;
                lastChar = null;
            }

            final float localX = xOffset + kerning + (ch.xOffset * scaleFactor),
                    localY = yOffset - (ch.yOffset * scaleFactor);

            glTexCoord2f(ch.tx1, ch.ty1);
            glVertex2f(localX, localY);
            glTexCoord2f(ch.tx1, ch.ty2);
            glVertex2f(localX, localY - chHeight);
            glTexCoord2f(ch.tx2, ch.ty2);
            glVertex2f(localX + chWidth, localY - chHeight);
            glTexCoord2f(ch.tx2, ch.ty1);
            glVertex2f(localX + chWidth, localY);

            xOffset += advance;
            lastChar = ch;
        }

        glEnd();
        glPopMatrix();
        glDisable(GL_TEXTURE_2D);

        sizeX = Math.max(sizeX, xOffset);
        return new Vector2f(sizeX, sizeY);
    }

    public DrawableString createText(String text, float size,
            float maxWidth, float maxHeight, Color color)
    {
        return new DrawableString(text, size, maxWidth, maxHeight, color);
    }

    public DrawableString createText(String text, float size, float maxWidth, Color color)
    {
        return createText(text, size, maxWidth, Float.MAX_VALUE, color);
    }

    public DrawableString createText(String text, float size, Color color)
    {
        return createText(text, size, Float.MAX_VALUE, Float.MAX_VALUE, color);
    }

    @Override
    public String toString()
    {
        return "LazyFont{" + "lookupTable=" + lookupTable
                + ", extendedChars=" + extendedChars + ", textureId=" + textureId
                + ", lineHeight=" + lineHeight + ", textureWidth=" + textureWidth
                + ", textureHeight=" + textureHeight + '}';
    }

    private class LazyChar
    {
        private final int id, width, height, xOffset, yOffset, advance; //page, channel;
        private final float tx1, ty1, tx2, ty2;
        private final Map<Integer, Integer> kernings = new HashMap<>();

        private LazyChar(int id, int tx, int ty, int width, int height,
                int xOffset, int yOffset, int advance) //int page, int channel)
        {
            this.id = id;
            this.width = width;
            this.height = height;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.advance = advance;
            //this.page = page;
            //this.channel = channel;

            // Calculate internal texture coordinates
            tx1 = tx / textureWidth;
            ty1 = (textureHeight - ty) / textureHeight;
            tx2 = tx1 + (width / textureWidth);
            ty2 = ty1 - (height / textureHeight);
        }

        private void addKerning(int otherChar, int kerning)
        {
            kernings.put(otherChar, kerning);
        }

        private int getKerning(LazyChar otherChar)
        {
            if (otherChar == null)
            {
                return 0;
            }

            //return kernings.getOrDefault(otherChar.id, 0);
            final Integer kerning = kernings.get(otherChar.id);
            if (kerning == null)
            {
                return 0;
            }

            return kerning;
        }

        @Override
        public String toString()
        {
            return "LazyChar{" + "id=" + id + ", width=" + width + ", height=" + height
                    + ", xOffset=" + xOffset + ", yOffset=" + yOffset
                    + ", advance=" + advance + ", tx1=" + tx1 + ", ty1=" + ty1
                    + ", tx2=" + tx2 + ", ty2=" + ty2 + ", kernings=" + kernings + '}';
        }

    }

    // TODO: Rewrite to be editable, recreate list when drawn after updating
    public class DrawableString
    {
        private final StringBuilder sb;
        private final int displayListId;
        private float size, width, maxWidth, height, maxHeight;
        private Color color;
        private boolean needsRebuild, disposed;

        private DrawableString(String text, float size, float maxWidth,
                float maxHeight, Color color)
        {
            this();

            this.size = size;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.color = color;

            sb.append(text);
        }

        private DrawableString()
        {
            sb = new StringBuilder();
            displayListId = glGenLists(1);
            needsRebuild = true;
            disposed = false;
        }

        public void appendText(String text)
        {
            sb.append(text);
            needsRebuild = true;
        }

        public void setText(String text)
        {
            sb.setLength(0);
            sb.append(text);
            needsRebuild = true;
        }

        public String getText()
        {
            return sb.toString();
        }

        public void setFontSize(float size)
        {
            if (size != this.size)
            {
                this.size = size;
                needsRebuild = true;
            }
        }

        public float getFontSize()
        {
            return size;
        }

        public void setMaxWidth(float maxWidth)
        {
            this.maxWidth = maxWidth;
            needsRebuild = true;
        }

        public void setMaxHeight(float maxHeight)
        {
            this.maxHeight = maxHeight;
            needsRebuild = true;
        }

        public void setColor(Color color)
        {
            needsRebuild = true;
        }

        public Color getColor()
        {
            return color;
        }

        private void buildString()
        {
            glNewList(displayListId, GL_COMPILE);
            final Vector2f tmp = drawText(sb.toString(), 0.01f, 0.01f, size, maxWidth, maxHeight, color);
            glEndList();

            width = tmp.x;
            height = tmp.y;
            needsRebuild = false;
        }

        public void draw(float x, float y)
        {
            if (disposed)
            {
                throw new RuntimeException("Tried to draw using disposed DrawableString!");
            }

            if (needsRebuild)
            {
                buildString();
            }

            glPushMatrix();
            glTranslatef(x, y, 0f);
            glCallList(displayListId);
            glPopMatrix();
        }

        public void draw(Vector2f location)
        {
            draw(location.x, location.y);
        }

        private void releaseResources()
        {
            glDeleteLists(displayListId, 1);
        }

        public void dispose()
        {
            if (!disposed)
            {
                releaseResources();
                FinalizeHelper.suppressFinalize(this);
            }
        }

        public float getStringWidth()
        {
            return width;
        }

        public float getStringHeight()
        {
            return height;
        }

        public boolean isDisposed()
        {
            return disposed;
        }

        @Override
        protected void finalize()
        {
            if (!disposed)
            {
                Log.warn("DrawableString was not disposed of properly!");
                releaseResources();
            }
        }
    }
}
