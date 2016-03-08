package org.lazywizard.console.commands;

import java.awt.Color;
import java.nio.ByteBuffer;
import org.apache.log4j.Logger;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lazywizard.console.util.FontException;
import org.lazywizard.console.util.LazyFont;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

/**
 *
 * @author LazyWizard
 */
public class TestNewConsole implements BaseCommand
{
    private static final Logger Log = Logger.getLogger(TestNewConsole.class);

    static void testConsole(LazyFont font, CommandContext context)
    {
        // Create buffer sized to hold the entire screen's pixel data
        final DisplayMode displayMode = Display.getDisplayMode();
        final int width = displayMode.getWidth(), height = displayMode.getHeight();
        final ByteBuffer buffer = BufferUtils.createByteBuffer(
                width * height * displayMode.getBitsPerPixel());

        // Save current screen image to buffer
        glReadBuffer(GL_BACK);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        // Bind buffer to (deliberately low-quality) texture
        final int textureId = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA2, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        buffer.clear();

        // DEBUG: Random test string using various supported characters
        final int dimX = 200, dimY = 100;
        final char[] supportedChars = new char[]
        {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '-', '=', '_', '+', '[', ']', '{', '}', '`', ',', '.', '/', '\\'
        };
        final StringBuilder sb = new StringBuilder(dimX * dimY);
        for (int i = 0; i < dimY; i++)
        {
            for (int j = 0; j < dimX; j++)
            {
                sb.append(supportedChars[(int) (supportedChars.length * Math.random())]);
            }

            sb.append('\n');
        }
        final String testString = sb.toString();

        // Poll for input until escape is pressed
        while (!Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)
                && !Keyboard.isKeyDown(Keyboard.KEY_SPACE))
        {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glClearColor(0f, 0f, 0f, 1f);

            // Set up OpenGL flags
            glPushAttrib(GL_ALL_ATTRIB_BITS);
            glMatrixMode(GL_PROJECTION);
            glPushMatrix();
            glLoadIdentity();
            glViewport(0, 0, width, height);
            glOrtho(0, width, 0, height, -1, 1);
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();
            glTranslatef(0.01f, 0.01f, 0);

            glEnable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureId);
            glPushMatrix();
            glBegin(GL_QUADS);
            glColor4f(0.3f, 0.3f, 0.3f, 1f);
            glTexCoord2f(0f, 0f);
            glVertex2f(0f, 0f);
            glTexCoord2f(1f, 0f);
            glVertex2f(width, 0f);
            glTexCoord2f(1f, 1f);
            glVertex2f(width, height);
            glTexCoord2f(0f, 1f);
            glVertex2f(0f, height);
            glEnd();
            glPopMatrix();

            //font.draw(CommonStrings.INPUT_QUERY, Mouse.getX(), Mouse.getY() + 50f, Color.WHITE);
            //font.draw("Test1\nTest2\nTest3", Mouse.getX(), Mouse.getY() - 50f, Color.WHITE);
            font.draw(testString, Mouse.getX(), height, Color.WHITE);

            // Clear OpenGL flags
            glPopMatrix();
            glMatrixMode(GL_PROJECTION);
            glPopMatrix();
            glPopAttrib();

            Display.update();
            // DEBUG: Disabled for FPS benchmarking
            //Display.sync(60);
        }

        // Release native resources
        glDeleteTextures(textureId);
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        try
        {
            testConsole(LazyFont.getFont("insignia15LTaa"), context);
        }
        catch (FontException ex)
        {
            Console.showException("Failed to load/parse font", ex);
            return CommandResult.ERROR;
        }

        return CommandResult.SUCCESS;
    }
}
