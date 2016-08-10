package org.lazywizard.console.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.apache.log4j.Logger;

/**
 * Responsible for loading .fnt files and parsing them into usable
 * {@link LazyFont}s.
 *
 * @author LazyWizard
 * @since 3.0
 */
public class FontLoader
{
    private static final Logger Log = Logger.getLogger(FontLoader.class);
    private static final String FONT_PATH_PREFIX = "graphics/fonts/";
    private static final String SPLIT_REGEX = "=|\\s+(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)";

    // File format documentation: http://www.angelcode.com/products/bmfont/doc/file_format.html
    // TODO: Rewrite parsing code to be more robust and presentable
    public static LazyFont loadFont(String fontPath) throws FontException
    {
        // Load the font file contents for later parsing
        final String header;
        final List<String> charLines = new ArrayList<>(), kernLines = new ArrayList<>();
        try (final Scanner reader = new Scanner(Global.getSettings().openStream(fontPath)))
        //FONT_PATH_PREFIX + fontName + ".fnt")))
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
            throw new RuntimeException("Failed to load font at '" + fontPath + "'", ex);
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

            final String fontName = metadata[2].replace("\"", "");
            final float baseHeight = Float.parseFloat(metadata[27]);
            final String imgFile = metadata[50].replace("\"", "");

            // Load the font image into a texture
            // TODO: Add support for multiple image files; 'pages' in the font file
            final int textureId;
            final float textureWidth, textureHeight;
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

            final LazyFont font = new LazyFont(fontName, baseHeight,
                    textureId, textureWidth, textureHeight);

            // Parse character data and place into a quick lookup table or extended character map
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

                font.addChar(Integer.parseInt(charData[2]), // id
                        Integer.parseInt(charData[4]), // tx
                        Integer.parseInt(charData[6]), // ty
                        Integer.parseInt(charData[8]), // width
                        Integer.parseInt(charData[10]), // height
                        Integer.parseInt(charData[12]), // xOffset
                        Integer.parseInt(charData[14]), // yOffset
                        Integer.parseInt(charData[16])); // advance
                //Ingeger.parseInt(data[18]), // page
                // Integer.parseInt(data[20])); // channel
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
                font.getChar((char) id).addKerning(otherId, kernAmount);
            }

            return font;
        }
        catch (NumberFormatException ex)
        {
            throw new FontException("Failed to parse font at '" + fontPath + "'", ex);
        }
    }

    private FontLoader()
    {
    }

    /**
     * Thrown when something has gone wrong while retrieving or loading a font.
     *
     * @author LazyWizard
     * @since 3.0
     */
    public static class FontException extends Exception
    {
        public FontException(String message)
        {
            super(message);
        }

        public FontException(String message, Throwable cause)
        {
            super(message, cause);
        }

        public FontException(Throwable cause)
        {
            super(cause);
        }
    }
}
