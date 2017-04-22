package org.lazywizard.console;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.text.Normalizer;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.util.FontLoader;
import org.lazywizard.console.util.FontLoader.FontException;
import org.lazywizard.console.util.LazyFont;
import org.lazywizard.console.util.LazyFont.DrawableString;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.vector.Vector2f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;

/**
 *
 * @author LazyWizard
 */
// TODO: Make package-private when test command is no longer needed
// TODO: Fix borked instance, rewrite to be all static again
public class ConsoleOverlay
{
    private static final Logger Log = Logger.getLogger(ConsoleOverlay.class);
    private static ConsoleOverlay instance;
    private static LazyFont font;
    private final int width, height;
    private final DrawableString history, input, query, testString;
    private boolean shouldShow = false;

    public static void main(String[] args)
    {
        //<editor-fold defaultstate="collapsed" desc="TEST TEXT">
        final String TEST_TEXT = "Folloŵȇŕs 4Salȇ ‏@franqui1017 Bùy Fоllоwȇrs and Likȇs orčpžsíáýd Tĥïŝ ĩš â fůňķŷ Šťŕĭńġ\n"
                + "\"Ït's lïke ä päir öf ëyes. Yöu're löoking ät thë umläut, änd ït's löoking ät yoü.\"\n"
                + "— Dävid St. Hübbins, This Is Spinal Tap\n"
                + "\n"
                + "Ëvërÿthïng's mörë mëtäl wïth ümläüts. Maybe it's becäuse they can make änything look vaguely Germänic, and everything söunds scarier in German. Or maybe it's jüst because they look cöol, especially whën they're printed in a Göthic typeface. Either way, the diaeresis has becöme the text equivalent of giving ä Devil-hörned salute. Despite the title, however, the Heävy Mëtal Ümlaut is sometimes used in music genres besides metäl.\n"
                + "\n"
                + "Othèr űnnæcessåry diácrîtiçal mârks, Faцx Cyяillic, and gratuitøus slashed ø's alsø shów up occâsioñally in mûsic, althøugh theý're Иot as pøpular or icônic of metäl as the ümläüt. Excessive use of this trope becomes £33†.\n"
                + "\n"
                + "Üsed very frequently in parödies, where band names can even have Um̈lauẗs over con̈son̈an̈ẗs; in fact it's well on its way to Discredited Tropedom if it's not there already.\n"
                + "\n"
                + "It must be nöted that this tröpe is about the gratuitoüs usage of umlauts, not \"any usage of umlauts\". Some artists from countries where umlauts are used in the local language have a genuine ümlaut in their band or personal names. Einstürzende Neubauten and Björk are therefore not examples of this trope.\n"
                + "\n"
                + "Incidentally, the only letters in German that include umlauts are ä, ö and ü. They are pronounced, respectively, as: the e in bed (like a combination of a & e); a French \"eu\", which happens occasionally in English such as the i in sir (o + e); and a French u, made by forming the letter o with your lips, and voicing \"eeeee\" (u + e). The bands should therefore be pronounced \"Mo-tuhrr-head\" and \"Blue Uhy-ster cult\". (If you have a non-rhotic accent, the first two sounds are changed to \"air\" and \"ur\".) Ironically, the idea that a heavy rock band could make itself look hard and tough by adding umlauts is one that provokes amusement among many native German speakers, who apparently associate the letter \"ö\" with \"cute\", \"sweet\", \"cuddly\"...\n"
                + "\n"
                + "In common Metal parlance, however, gratuitous umlauts are not pronounced, but this hasn't stopped fans of Queensrÿche asking about the Ÿ.\n"
                + "\n"
                + "Not to be confused with other uses of diaereses (also called trema), in which diacritic marks identical to umlauts can appear in some English words. A diaeresis was traditionally used in vowel pairings where the second vowel is pronounced in a separate syllable, hence they are found in archaic spellings of words such as coöperate, preëmptive or Zodiäc. This usage is largely obsolete, though it is still part of the house style of The New Yorker magazine and MIT Technology Review, but survives in words like naïve which are borrowed from languages which do use diaereses to varying degrees. In modern English, umlaut is used in one special case, over \"e\" at the end of the word, where it denotes a pronounced \"e\" instead of silent \"e\", such as the Brontë siblings.\n"
                + "\n"
                + "Gratuitous umlauts usually cause unnecessary embarrassment amongst the native speakers of those languages, whose ortography does use umlauts. An umlaut usually denotes the vowel is pronounced as frontal. Ä denotes a frontal a, like \"cat\", while A without umlauts is the back vowel, like \"car\". Likewise, Ö denotes a frontal o phoneme [usually denoted in English as ir or ur ], not unlike \"sir\", while O without umlaut is back vowel O, like \"dog\". Languages which use umlaut vowel shift are German, Swedish, Finnish, Skolt Sami, Karelian, Estonian, Hungarian, Luxembourgish, North Frisian, Saterlandic, Emiliano-Romagnolo, Rotuman, Slovak, Turkish, Tatar, and Turkmen. Often Ä and Ö are treated as completely separate letters from A and O, appearing at the end of the alphabet beyond Z. "
                + "\n |[]{}()'`~;:'\".>,<\\";
        //</editor-fold>
        System.out.println(stripAccents(TEST_TEXT));
    }

    // TODO: Find fast solution that only removes characters not supported by font
    private static final Pattern p = Pattern.compile("\\pM");

    private static String stripAccents(String text)
    {
        return p.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
    }

    public static void reloadFont() throws FontException
    {
        if (font != null)
        {
            font.discard();
            font = null;
        }

        final String fontName = Console.getSettings().getFont();
        font = FontLoader.loadFont(fontName);
    }

    // TODO: Make package-private when test command is no longer needed
    public static void show(CommandContext context)
    {
        if (instance == null)
        {
            instance = new ConsoleOverlay();
        }

        instance.showInternal(context);
    }

    // TODO: Make package-private when test command is no longer needed
    public static void hide()
    {
        instance.hideInternal();
    }

    private ConsoleOverlay()
    {
        // Create buffer sized to hold the entire screen's pixel data
        final DisplayMode displayMode = Display.getDisplayMode();
        width = displayMode.getWidth();
        height = displayMode.getHeight();

        history = font.createText(""/*Console.getHistory().toString()*/, 15f,
                width - 60f, height - 60f, Console.getSettings().getOutputColor());
        query = font.createText(CommonStrings.INPUT_QUERY, 30f,
                width, Console.getSettings().getOutputColor().darker());
        input = font.createText("", 15f, width, 15f * 3f,
                Console.getSettings().getOutputColor());

        //<editor-fold defaultstate="collapsed" desc="Test string">
        // DEBUG: Random test string using various supported characters
        final int dimX = 700, dimY = 200;
        final char[] supportedChars = new char[]
        {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '-', '=', '_', '+', '[', ']', '{', '}', '`', ',', '.', '/', '\\'
        };
        final StringBuilder sb = new StringBuilder((dimX + 1) * dimY);
        for (int i = 0; i < dimY; i++)
        {
            for (int j = 0; j < dimX; j++)
            {
                sb.append(supportedChars[MathUtils.getRandom().nextInt(supportedChars.length)]);
            }

            //sb.append('\n');
        }
        testString = font.createText("This text should have a box drawn around\n"
                + "it showing its dimensions.\nTest1\nTest2\nTest3",
                15f, width, height, Color.WHITE);
        //</editor-fold>
    }

    private void showInternal(CommandContext context)
    {
        // Save current screen image to texture
        final ByteBuffer buffer = BufferUtils.createByteBuffer(
                width * height * Display.getDisplayMode().getBitsPerPixel());
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        final int bgTextureId = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, bgTextureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        buffer.clear();

        // Poll for input until escape is pressed
        shouldShow = true;
        while (shouldShow)
        {
            instance.checkInput();
            instance.render(bgTextureId);

            Display.update();
            Display.sync(60);
        }

        // Release native resources
        glDeleteTextures(bgTextureId);
        testString.dispose();
    }

    private void hideInternal()
    {
        shouldShow = false;
    }

    private void checkInput()
    {
        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)
                || Keyboard.isKeyDown(Keyboard.KEY_SPACE))
        {
            hideInternal();
            return;
        }

        // TODO: Port campaign input code
    }

    private void render(int bgTextureId)
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
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, bgTextureId);
        glPushMatrix();
        glBegin(GL_QUADS);
        glColor4f(0.2f, 0.2f, 0.2f, 1f);
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
        final Vector2f mousePos = new Vector2f(Mouse.getX(), Mouse.getY() - 50f);
        //font.drawText(testString, 0f, height, 25f, new Color(1f,1f,1f,.1f));
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        testString.draw(mousePos);
        glDisable(GL_TEXTURE_2D);
        glLineWidth(1f);
        glColor4f(0f, 1f, 1f, 1f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(mousePos.x - 1f, mousePos.y + 1f);
        glVertex2f(mousePos.x - 1f, mousePos.y - 1f - testString.getStringHeight());
        glVertex2f(mousePos.x + 1f + testString.getStringWidth(), mousePos.y - 1f - testString.getStringHeight());
        glVertex2f(mousePos.x + 1f + testString.getStringWidth(), mousePos.y + 1f);
        glEnd();

        // Clear OpenGL flags
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glPopAttrib();
    }
}
