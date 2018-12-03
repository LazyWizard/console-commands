package org.lazywizard.console.testing;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.input.Mouse;

import java.awt.*;

public class Test implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        Global.getCombatEngine().addPlugin(new LazyFontExample());
        return CommandResult.SUCCESS;
    }

    public class LazyFontExample extends BaseEveryFrameCombatPlugin
    {
        private LazyFont.DrawableString toDraw;

        // Set up the font and the DrawableString; only has to be done once
        @Override
        public void init(CombatEngineAPI engine)
        {
            // Load the chosen .fnt file
            // Fonts are cached globally, so it's acceptable for each class using the same
            // font to request their own copy of it - they will all share the underlying data
            final LazyFont font;
            try
            {
                font = LazyFont.loadFont("graphics/fonts/insignia15LTaa.fnt");
            }
            // FontException is thrown if the .fnt file does not exist or has malformed data
            catch (FontException ex)
            {
                Global.getLogger(this.getClass()).error("Failed to load font", ex);
                engine.removePlugin(this);
                return;
            }

            // Create a renderable block of text (in this case, will be yellow with font size 15)
            toDraw = font.createText("This is some sample text.", Color.YELLOW, 15f);

            // Enable line wrapping when text reaches 400 pixels wide
            toDraw.setMaxWidth(400f);

            // If you need to add text to the DrawableString, do so like this:
            toDraw.appendText("\nThis is a second line of sample text.");
            toDraw.appendText("\nThis is a third line of sample text that shows off the automatic" +
                    " word wrapping when a line of text reaches the maximum width you've chosen.");
            //toDraw.append("\nRed", Color.RED).append("Blue", Color.BLUE).append("Green", Color.GREEN)
            //        .append("r\ne\nd\n", Color.RED).append("b\nl\nu\ne\n", Color.BLUE).append("g\nr\ne\ne\nn\n");
        }

        @Override
        public void renderInUICoords(ViewportAPI view)
        {
            // Call draw() once per frame to render the text
            // In this case, draw the text slightly below the mouse cursor
            // The draw point is the top left corner of the textbox, so we adjust the X
            // position to center the text horizontally below the mouse cursor
            toDraw.draw(Mouse.getX() - (toDraw.getWidth() / 2f), Mouse.getY() - 30f);
        }
    }

}
