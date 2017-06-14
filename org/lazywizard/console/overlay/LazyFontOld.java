package org.lazywizard.console.overlay;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.lwjgl.util.vector.Vector2f;
import static org.lazywizard.lazylib.opengl.ColorUtils.*;
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
public class LazyFontOld
{
    private static final Logger Log = Logger.getLogger(LazyFontOld.class);
    private final String fontName;
    private final LazyChar[] lookupTable;
    private final Map<Character, LazyChar> extendedChars;
    private final int textureId;
    private final float baseHeight, textureWidth, textureHeight;

    // TODO
    public void discard()
    {
        glDeleteTextures(textureId);
    }

    // TODO: Extract font parsing to own class
    LazyFontOld(String fontName, float baseHeight, int textureId,
            float textureWidth, float textureHeight)
    {
        this.fontName = fontName;
        this.baseHeight = baseHeight;
        this.textureId = textureId;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;

        lookupTable = new LazyChar[224];
        extendedChars = new HashMap<>();
    }

    void addChar(int id, int tx, int ty, int width, int height,
            int xOffset, int yOffset, int advance) //int page, int channel)
    {
        final LazyChar tmp = new LazyChar(id, tx, ty, width, height,
                xOffset, yOffset, advance);

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
        return baseHeight;
    }

    // TODO: Javadoc this
    public String getFontName()
    {
        return fontName;
    }

    public LazyChar getChar(char character)
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
        final float scaleFactor = size / baseHeight;
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
                + ", lineHeight=" + baseHeight + ", textureWidth=" + textureWidth
                + ", textureHeight=" + textureHeight + '}';
    }

    public class LazyChar
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

        void addKerning(int otherChar, int kerning)
        {
            kernings.put(otherChar, kerning);
        }

        public int getKerning(LazyChar otherChar)
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
