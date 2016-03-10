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
public class LazyFont
{
    private static final Logger Log = Logger.getLogger(LazyFont.class);
    private static final String FONT_PATH_PREFIX = "graphics/fonts/";
    private static final String SPLIT_REGEX = "\\s+|=";
    private static final Map<String, LazyFont> fonts = new HashMap<>();
    private final String id, name;
    private final Map<Integer, LazyChar> chars;
    private final int textureId, lineHeight;
    private final float textureWidth, textureHeight;

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
        font.chars.clear();
        glDeleteTextures(font.textureId);
    }

    // File format documentation: http://www.angelcode.com/products/bmfont/doc/file_format.html
    private LazyFont(String fontName) throws FontException
    {
        id = fontName;

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

        // Load the font image into a texture
        // TODO: The filename should be taken from the font's metadata
        // TODO: Add support for multiple image files; 'pages' in the font file
        final String imgFile = fontName + "_0.png";
        try
        {
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

        // Finally parse the file data we retrieved at the beginning of the constructor
        try
        {
            // TODO: Parse and store ALL font metadata
            // FIXME: Write _proper_ line parser that can handle key="word1 word2" correctly
            final String[] metadata = header.split(SPLIT_REGEX);
            if (metadata.length != 51)
            {
                Log.error("Metadata length mismatch: " + metadata.length
                        + " vs expected length of 51.");
                Log.error("Input string: " + header);
                throw new FontException("Metadata length mismatch");
            }

            name = metadata[2]; // TODO
            lineHeight = Integer.parseInt(metadata[27]);

            // Parse character data into a map of LazyChars
            chars = new HashMap<>();
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
                chars.put(tmp.id, tmp);
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
                chars.get(id).addKerning(otherId, kernAmount);
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

    public static void main(String[] args)
    {
        final String header = "info face=\"InsigniaLT\" size=15 bold=0 italic=0 charset=\"\" unicode=1 stretchH=100 smooth=1 aa=4 padding=0,0,0,0 spacing=1,1 outline=0\n"
                + " common lineHeight=15 base=12 scaleW=256 scaleH=256 pages=1 packed=0 alphaChnl=1 redChnl=0 greenChnl=0 blueChnl=0\n"
                + " page id=0 file=\"insignia15LTaa_0.png\"";
        final String[] metadata = header.split(SPLIT_REGEX);
        for (int i = 0; i < metadata.length; i++)
        {
            System.out.println(i + ": " + metadata[i]);
        }
        //System.out.println(metadata.length + ": " + CollectionUtils.implode(Arrays.asList(metadata)));
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
        final float scaleFactor = (size / (float) lineHeight);
        float xOffset = 0f, yOffset = 0f, sizeX = 0f, sizeY = lineHeight * scaleFactor;

        // TODO: Rewrite to reuse same buffer
        for (final char tmp : text.toCharArray())
        {
            if (tmp == '\n')
            {
                if (-yOffset + (lineHeight * scaleFactor) > maxHeight)
                {
                    return new Vector2f(sizeX, sizeY);
                }

                yOffset -= lineHeight * scaleFactor;
                sizeY += lineHeight * scaleFactor;
                sizeX = Math.max(sizeX, xOffset);
                xOffset = 0f;
                lastChar = null;
                continue;
            }

            final LazyChar ch = chars.get((int) tmp);
            final int kerning = ch.getKerning(lastChar);
            final float advance = (ch.advance + kerning) * scaleFactor;

            if (xOffset + advance > maxWidth)
            {
                if (-yOffset + (lineHeight * scaleFactor) > maxHeight)
                {
                    return new Vector2f(sizeX, sizeY);
                }

                yOffset -= lineHeight * scaleFactor;
                sizeY += lineHeight * scaleFactor;
                sizeX = Math.max(sizeX, xOffset);
                xOffset = -kerning;
                lastChar = null;
            }

            final float localX = xOffset + ((ch.xOffset + kerning) * scaleFactor),
                    localY = yOffset - (ch.yOffset * scaleFactor);

            glTexCoord2f(ch.tx1, ch.ty1);
            glVertex2f(localX, localY);
            glTexCoord2f(ch.tx1, ch.ty2);
            glVertex2f(localX, localY - (ch.height * scaleFactor));
            glTexCoord2f(ch.tx2, ch.ty2);
            glVertex2f(localX + (ch.width * scaleFactor), localY - (ch.height * scaleFactor));
            glTexCoord2f(ch.tx2, ch.ty1);
            glVertex2f(localX + (ch.width * scaleFactor), localY);

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
        return "LazyFont{" + "chars=" + chars + ", textureId=" + textureId
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

    public class DrawableString
    {
        private final int displayListId;
        private final float width, height;
        private boolean disposed = false;

        private DrawableString(String text, float size, float maxWidth,
                float maxHeight, Color color)
        {
            displayListId = glGenLists(1);
            glNewList(displayListId, GL_COMPILE);
            final Vector2f tmp = drawText(text, 0.01f, 0.01f, size, maxWidth, maxHeight, color);
            glEndList();

            width = tmp.x;
            height = tmp.y;
        }

        public void draw(float x, float y)
        {
            if (disposed)
            {
                throw new RuntimeException("Tried to draw using disposed DrawableString!");
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

        public void dispose()
        {
            if (!disposed)
            {
                glDeleteLists(displayListId, 1);
                disposed = true;
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
                dispose();
            }
        }
    }
}
