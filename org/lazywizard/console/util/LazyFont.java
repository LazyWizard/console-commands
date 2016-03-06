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
import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author LazyWizard
 * @since 3.0
 */
// TODO: Rewrite to use buffers, have draw() return a redrawable font string
public class LazyFont
{
    private static final Logger Log = Logger.getLogger(LazyFont.class);
    private static final String FONT_PATH_PREFIX = "graphics/fonts/";
    private static final String SPLIT_REGEX = "\\s+|=";
    private static final Map<String, LazyFont> fonts = new HashMap<>();
    private final Map<Integer, LazyChar> chars;
    private final int textureId, lineHeight;
    private final float textureWidth, textureHeight;

    /**
     * Retrieves a font from the font cache, or loads and caches it if it hasn't
     * been requested before.
     *
     * @param fontName The font's name, same as the filename minus the extension
     *                 and the directories leading up to it.
     *
     * @return A representation of the font ready for rendering.
     */
    public static LazyFont getFont(String fontName)
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

    // File format documentation: http://www.angelcode.com/products/bmfont/doc/file_format.html
    private LazyFont(String fontName)
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

        // Load the font image into a texture
        // TODO: The filename should be taken from the font's metadata
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
            // TODO: Parse font metadata
            final String[] metadata = header.split(SPLIT_REGEX);
            if (metadata.length != 51)
            {
                Log.error("Metadata length mismatch: " + metadata.length
                        + " vs expected length of 51.");
                Log.error("Input string: " + header);
                throw new FontException("Metadata length mismatch");
            }

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

                final int id = Integer.parseInt(kernData[2]),
                        otherId = Integer.parseInt(kernData[4]),
                        kernAmount = Integer.parseInt(kernData[6]);
                chars.get(id).addKerning(otherId, kernAmount);
            }
        }
        catch (NumberFormatException | FontException ex)
        {
            throw new RuntimeException("Failed to parse font '" + fontName + "'", ex);
        }
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

    public void draw(String text, float x, float y, Color color)
    {
        glColor(color);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glEnable(GL_TEXTURE_2D);
        LazyChar lastChar = null;
        float offset = 0f;
        for (char tmp : text.toCharArray())
        {
            if (tmp == '\n')
            {
                y -= lineHeight;
                offset = 0f;
                continue;
            }

            final LazyChar ch = chars.get((int) tmp);
            final int kerning = ch.getKerning(lastChar);
            ch.draw(x + offset + kerning, y);
            offset += kerning + ch.advance;
            lastChar = ch;
        }
        glDisable(GL_TEXTURE_2D);
    }

    @Override
    public String toString()
    {
        return "LazyFont{" + "texture=" + textureId + ", chars=" + chars + '}';
    }

    private class LazyChar
    {
        private final int id, tx, ty, width, height, xOffset, yOffset, advance; //page, channel;
        private final Map<Integer, Integer> kernings = new HashMap<>();

        private LazyChar(int id, int tx, int ty, int width, int height,
                int xOffset, int yOffset, int advance) //int page, int channel)
        {
            this.id = id;
            this.tx = tx;
            this.ty = ty;
            this.width = width;
            this.height = height;
            this.xOffset = xOffset;
            this.yOffset = -yOffset;
            this.advance = advance;
            //page = Integer.parseInt(data[18]);
            //channel = Integer.parseInt(data[20]);
        }

        private void draw(float x, float y)
        {
            final float iX = tx / textureWidth,
                    iY = (textureHeight - ty) / textureHeight,
                    iW = (width / textureWidth),
                    iH = (height / textureHeight);

            glPushMatrix();
            glTranslatef(xOffset, yOffset, 0f);
            glEnable(GL_TEXTURE_2D);
            glBegin(GL_QUADS);
            glColor4f(0f, 1f, 1f, 1f);
            glTexCoord2f(iX, iY);
            glVertex2f(x, y);
            glTexCoord2f(iX, iY - iH);
            glVertex2f(x, y - height);
            glTexCoord2f(iX + iW, iY - iH);
            glVertex2f(x + width, y - height);
            glTexCoord2f(iX + iW, iY);
            glVertex2f(x + width, y);
            glEnd();
            glPopMatrix();
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

        /*@Override
        public String toString()
        {
            return "LazyChar{" + "id=" + id + ", x=" + tx + ", y=" + ty
                    + ", width=" + width + ", height=" + height
                    + ", xOffset=" + xOffset + ", yOffset=" + yOffset
                    + ", advance=" + advance + ", kernings=" + kernings + '}';
        }*/
    }

    private static class FontException extends Exception
    {
        private FontException(String message)
        {
            super(message);
        }
    }
}
