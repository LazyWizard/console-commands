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
    private final SpriteAPI texture;

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

    private LazyFont(String fontName)
    {
        // Load the font file for parsing
        final List<String> charLines = new ArrayList<>(), kernLines = new ArrayList<>();
        try (final Scanner reader = new Scanner(Global.getSettings().openStream(
                FONT_PATH_PREFIX + fontName + ".fnt")))
        {
            // Skip the metadata we won't be using
            // TODO: Implement these later, not required for SS's default font
            reader.nextLine(); // Info
            reader.nextLine(); // Common
            reader.nextLine(); // Page

            // Save data for later parsing
            while (reader.hasNextLine())
            {
                final String line = reader.nextLine();
                if (line.startsWith("char "))
                {
                    charLines.add(line);
                }
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

        // Load the font image into a SpriteAPI
        final String imgFile = fontName + "_0.png";
        try
        {
            // TODO: The filename should be taken from the font's metadata
            // TODO: Will probably do all drawing manually, can just hang on to texture id
            Global.getSettings().loadTexture(FONT_PATH_PREFIX + imgFile);
            texture = Global.getSettings().getSprite(FONT_PATH_PREFIX + imgFile);
            System.out.println("Spritesheet id: " + texture.getTextureId());
        }
        catch (IOException ex)
        {
            throw new RuntimeException("Failed to load texture atlas '" + imgFile + "'", ex);
        }

        // Parse font data into individual characters
        try
        {
            // Parse character data into a map of LazyChars
            chars = new HashMap<>();
            for (String charLine : charLines)
            {
                final LazyChar tmp = new LazyChar(charLine);
                chars.put(tmp.id, tmp);
            }

            // Parse and add kerning data
            for (String kernLine : kernLines)
            {
                final String[] data = kernLine.split(SPLIT_REGEX);
                if (data.length != 7)
                {
                    Log.error("Kerning data length mismatch: "
                            + data.length + " vs expected length of 7.");
                    Log.error("Input string: " + kernLine);
                    throw new FontException("Kerning data length mismatch");
                }

                final int id = Integer.parseInt(data[2]),
                        otherId = Integer.parseInt(data[4]),
                        kernAmount = Integer.parseInt(data[6]);
                chars.get(id).addKerning(otherId, kernAmount);
            }
        }
        catch (NumberFormatException | FontException ex)
        {
            throw new RuntimeException("Failed to parse font '" + fontName + "'", ex);
        }
    }

    public void draw(String text, float x, float y, Color color)
    {
        texture.bindTexture();
        glColor(color);
        glEnable(GL_TEXTURE_2D);
        LazyChar lastChar = null;
        for (char tmp : text.toCharArray())
        {
            final LazyChar ch = chars.get((int) tmp);
            final int kerning = ch.getKerning(lastChar);
            ch.draw(x + kerning, y);
            x += kerning + ch.advance;
            lastChar = ch;
        }
        glDisable(GL_TEXTURE_2D);
    }

    @Override
    public String toString()
    {
        return "LazyFont{" + "texture=" + texture.getTextureId() + ", chars=" + chars + '}';
    }

    private class LazyChar
    {
        private final int id, tx, ty, width, height, xOffset, yOffset, advance; //page, channel;
        private final Map<Integer, Integer> kernings = new HashMap<>();

        private LazyChar(String fontLine) throws FontException
        {
            final String[] data = fontLine.split(SPLIT_REGEX);
            if (data.length != 21)
            {
                Log.error("Character data length mismatch: "
                        + data.length + " vs expected length of 21.");
                Log.error("Input string: " + fontLine);
                throw new FontException("Character data length mismatch");
            }

            id = Integer.parseInt(data[2]);
            tx = Integer.parseInt(data[4]);
            ty = Integer.parseInt(data[6]);
            width = Integer.parseInt(data[8]);
            height = Integer.parseInt(data[10]);
            xOffset = Integer.parseInt(data[12]);
            yOffset = Integer.parseInt(data[14]);
            advance = Integer.parseInt(data[16]);
            //page = Integer.parseInt(data[18]);
            //channel = Integer.parseInt(data[20]);
        }

        private void draw(float x, float y)
        {
            // TODO: Write own draw code to avoid SpriteAPI's... over-enthusiastic binding
            //System.out.println("Drawing " + this + " at " + x + "," + y);
            /*glDisable(GL_TEXTURE_2D);
            glLineWidth(0.5f);
            glColor4f(1f, 1f, 1f, 1f);
            glBegin(GL_QUADS);
            glVertex2f(x, y);
            glVertex2f(x + width, y);
            glVertex2f(x + width, y + height);
            glVertex2f(x, y + height);
            glEnd();*/

            //texture.renderRegion(x+xOffset, y+yOffset,
            //        0f,0f,1f,1f);
            final float iX = tx / texture.getWidth(),
                    iY = (texture.getHeight() - ty) / texture.getHeight(),
                    iW = (width / texture.getWidth()),
                    iH = (height / texture.getHeight());

            x += xOffset;
            y -= yOffset;

            glPushMatrix();
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

            //return kernings.getOrDefault(otherChar, 0);
            final Integer kerning = kernings.get(otherChar);
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
