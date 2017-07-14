package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.graphics.PositionAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.ValueDisplayMode;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.ConsoleSettings;
import org.lazywizard.console.ConsoleSettings.Keystroke;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import java.awt.*;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class ShowSettings implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if ("reset".equalsIgnoreCase(args))
        {
            Console.getSettings().resetToDefaults();
            Console.showMessage("Settings reset to defaults (requires an application restart to take effect).");
            return CommandResult.SUCCESS;
        }

        Console.showDialogOnClose(new SettingsDialog(), Global.getSector().getPlayerFleet());
        Console.showMessage("The settings dialog will be shown when you close the console overlay.");
        return CommandResult.SUCCESS;
    }

    public static class SettingsDialog implements InteractionDialogPlugin
    {
        private final ConsoleSettings settings = Console.getSettings();
        private InteractionDialogAPI dialog;
        private OptionPanelAPI options;
        private TextPanelAPI text;
        private Menu currentMenu;
        private boolean showCommands, showMemory, showExceptions, showIndex, homeStorage;
        private int red, green, blue, scrollback;
        private float threshold;

        private enum Menu
        {
            MAIN,
            INPUT,
            COLOR,
            DISPLAY,
            MISC,
            LISTEN_KEYSTROKE,
            LISTEN_SEPARATOR
        }

        private enum Option
        {
            SHOW_COMMANDS,
            SHOW_MEMORY,
            SHOW_EXCEPTIONS,
            SHOW_INDEX,
            HOME_STORAGE,
            TEST_COLOR,
            EXIT
        }

        private enum Selector
        {
            COLOR_R,
            COLOR_G,
            COLOR_B,
            TYPO_THRESHOLD,
            MAX_SCROLLBACK
        }

        @Override
        public void init(InteractionDialogAPI dialog)
        {
            this.dialog = dialog;
            text = dialog.getTextPanel();
            options = dialog.getOptionPanel();

            final Color outputColor = settings.getOutputColor();
            red = outputColor.getRed();
            green = outputColor.getGreen();
            blue = outputColor.getBlue();
            scrollback = settings.getMaxScrollback();
            threshold = settings.getTypoCorrectionThreshold();
            homeStorage = settings.getShouldTransferStorageToHome();
            showCommands = settings.getShouldShowEnteredCommands();
            showMemory = settings.getShouldShowMemoryUsage();
            showExceptions = settings.getShouldShowExceptionDetails();
            showIndex = settings.getShouldShowCursorIndex();

            goToMenu(Menu.MAIN);
        }

        private void goToMenu(Menu menu)
        {
            text.clear();
            options.clearOptions();
            dialog.hideVisualPanel();
            dialog.setPromptText("");
            final float barWidth = Math.min(Display.getWidth() * 0.7f, 800f);

            switch (menu)
            {
                case MAIN:
                    text.addParagraph(""); // TODO: Description
                    options.addOption("Input settings", Menu.INPUT, ""); // TODO: Tooltip
                    options.addOption("Color settings", Menu.COLOR, ""); // TODO: Tooltip
                    options.addOption("Display settings", Menu.DISPLAY, ""); // TODO: Tooltip
                    options.addOption("Misc settings", Menu.MISC, ""); // TODO: Tooltip
                    options.addOption("Save and exit", Option.EXIT, ""); // TODO: Tooltip
                    options.setShortcut(Option.EXIT, Keyboard.KEY_ESCAPE, false, false, false, true);
                    break;
                case INPUT:
                    text.addParagraph(""); // TODO: Description
                    options.addOption("Set console overlay key", Menu.LISTEN_KEYSTROKE,
                            "Sets the key combination used to open the console overlay.");
                    options.addOption("Set command separator", Menu.LISTEN_SEPARATOR,
                            "Sets the character used to separate multiple commands.");
                    break;
                case LISTEN_KEYSTROKE:
                    text.addParagraph("Press the key combination you would like to summon the console with." +
                            " Currently the console is summoned with " + settings.getConsoleSummonKey() + ".");
                    dialog.getVisualPanel().showCustomPanel(0f, 0f, new KeyListenerPlugin());
                    break;
                case LISTEN_SEPARATOR:
                    text.addParagraph("Enter the character you would like to separate multiple commands with." +
                            " Currently multiple commands are separated with '" + settings.getCommandSeparator() + "'.");
                    dialog.getVisualPanel().showCustomPanel(0f, 0f, new KeyListenerPlugin());
                    break;
                case COLOR:
                    text.addParagraph(""); // TODO: Description

                    // Console overlay font color
                    options.addSelector("Output Color (red)", Selector.COLOR_R, Color.RED, barWidth, 150f, 0f, 255f, ValueDisplayMode.X_OVER_Y_NO_SPACES,
                            "The red component of the overlay's text color.");
                    options.setSelectorValue(Selector.COLOR_R, red);
                    options.addSelector("Output Color (green)", Selector.COLOR_G, Color.GREEN, barWidth, 150f, 0f, 255f, ValueDisplayMode.X_OVER_Y_NO_SPACES,
                            "The blue component of the overlay's text color.");
                    options.setSelectorValue(Selector.COLOR_G, green);
                    options.addSelector("Output Color (blue)", Selector.COLOR_B, Color.BLUE, barWidth, 150f, 0f, 255f, ValueDisplayMode.X_OVER_Y_NO_SPACES,
                            "The green component of the overlay's text color.");
                    options.setSelectorValue(Selector.COLOR_B, blue);
                    options.addOption("Test current color", Option.TEST_COLOR, "Prints an example of the current color.");
                    dialog.getVisualPanel().showCustomPanel(50f, 50f, new ColorDisplayPlugin());
                    break;
                case DISPLAY:
                    text.addParagraph(""); // TODO: Description

                    // Misc display options
                    options.addOption("Show entered commands: " + (showCommands ? "true" : "false"), Option.SHOW_COMMANDS,
                            "Whether to show the commands you've entered in the overlay.");
                    options.addOption("Show memory usage: " + (showMemory ? "true" : "false"), Option.SHOW_MEMORY,
                            "Whether to show Starsector's current memory usage at the top of the console overlay.");
                    options.addOption("Show error stack traces: " + (showExceptions ? "true" : "false"), Option.SHOW_EXCEPTIONS,
                            "Whether to show exception stack traces when something goes wrong. Very spammy!\n" +
                                    "Exceptions are always saved starsector.log, so this is only useful for developers.");
                    options.addOption("Show cursor index (debug): " + (showIndex ? "true" : "false"), Option.SHOW_INDEX,
                            "Whether to show debug information in the overlay's input text field.");
                    break;
                case MISC:
                    text.addParagraph(""); // TODO: Description

                    // Typo correction sensitivity
                    options.addSelector("Typo detection threshold", Selector.TYPO_THRESHOLD, Color.WHITE, barWidth, 150f, 0f, 1f, ValueDisplayMode.PERCENT,
                            "Controls how sensitive typo detection is. A higher value means fewer (but more accurate) suggestions when you enter something wrong.");
                    options.setSelectorValue(Selector.TYPO_THRESHOLD, threshold);

                    // Max scrollback (in characters)
                    options.addSelector("Max scrollback (in characters)", Selector.MAX_SCROLLBACK, Color.WHITE, barWidth, 150f, 0f, 20_000f, ValueDisplayMode.VALUE,
                            "How many characters of output history will be stored in the overlay between uses. A higher value means slightly more RAM used by the console.");
                    options.setSelectorValue(Selector.MAX_SCROLLBACK, scrollback);

                    // Use Home market as Storage
                    options.addOption("Always use Home's market for Storage: " + (homeStorage ? "true" : "false"), Option.HOME_STORAGE,
                            "When enabled, the contents of your Storage will automatically transfer to your Home's storage submarket if it has one.");
                    options.setEnabled(Option.HOME_STORAGE, false); // TODO
                    break;
            }

            if (menu != Menu.MAIN)
            {
                options.addOption("Back", Menu.MAIN, "Return to the main settings menu.");
                options.setShortcut(Menu.MAIN, Keyboard.KEY_ESCAPE, false, false, false, true);
            }

            currentMenu = menu;
        }

        @Override
        public void optionSelected(String optionText, Object optionData)
        {
            if (optionData instanceof Menu)
            {
                goToMenu((Menu) optionData);
                return;
            }

            switch ((Option) optionData)
            {
                case SHOW_COMMANDS:
                    showCommands = !showCommands;
                    goToMenu(Menu.DISPLAY);
                    break;
                case SHOW_MEMORY:
                    showMemory = !showMemory;
                    goToMenu(Menu.DISPLAY);
                    break;
                case SHOW_INDEX:
                    showIndex = !showIndex;
                    goToMenu(Menu.DISPLAY);
                    break;
                case SHOW_EXCEPTIONS:
                    showExceptions = !showExceptions;
                    goToMenu(Menu.DISPLAY);
                    break;
                case HOME_STORAGE:
                    homeStorage = !homeStorage;
                    goToMenu(Menu.MISC);
                    break;
                case TEST_COLOR:
                    text.addParagraph("Here is what the console's output would look like with color {"
                            + red + ", " + green + ", " + blue + "}.", new Color(red, green, blue));
                    break;
                case EXIT:
                    saveOptions();
                    dialog.dismiss();
            }
        }

        private void saveOptions()
        {
            settings.setOutputColor(new Color(red, green, blue));
            settings.setTypoCorrectionThreshold(threshold);
            settings.setMaxScrollback(scrollback);
            settings.setShouldTransferStorageToHome(homeStorage);
            settings.setShouldShowEnteredCommands(showCommands);
            settings.setShouldShowMemoryUsage(showMemory);
            settings.setShouldShowCursorIndex(showIndex);
            settings.setShouldShowExceptionDetails(showExceptions);
        }

        @Override
        public void advance(float amount)
        {
            if (currentMenu == Menu.COLOR)
            {
                // Clamp color components to int
                red = (int) options.getSelectorValue(Selector.COLOR_R);
                green = (int) options.getSelectorValue(Selector.COLOR_G);
                blue = (int) options.getSelectorValue(Selector.COLOR_B);
                options.setSelectorValue(Selector.COLOR_R, red);
                options.setSelectorValue(Selector.COLOR_G, green);
                options.setSelectorValue(Selector.COLOR_B, blue);
            }
            else if (currentMenu == Menu.MISC)
            {
                scrollback = Math.round(options.getSelectorValue(Selector.MAX_SCROLLBACK) / 100f) * 100;
                options.setSelectorValue(Selector.MAX_SCROLLBACK, scrollback);
                threshold = Math.round(options.getSelectorValue(Selector.TYPO_THRESHOLD) * 100f) / 100f;
                options.setSelectorValue(Selector.TYPO_THRESHOLD, threshold);
            }
        }

        @Override
        public void optionMousedOver(String optionText, Object optionData)
        {
        }

        @Override
        public void backFromEngagement(EngagementResultAPI battleResult)
        {
        }

        @Override
        public Object getContext()
        {
            return null;
        }

        @Override
        public Map<String, MemoryAPI> getMemoryMap()
        {
            return null;
        }

        private class ColorDisplayPlugin extends BaseUIPlugin
        {
            private PositionAPI pos;

            @Override
            public void positionChanged(PositionAPI position)
            {
                pos = position;
            }

            @Override
            public void render(float alphaMult)
            {
                if (pos == null) return;

                // TODO: Replace with sprite text
                glDisable(GL_TEXTURE_2D);
                glEnable(GL_BLEND);
                glColor3ub((byte) red, (byte) green, (byte) blue);
                glBegin(GL_QUADS);
                glVertex2f(pos.getX(), pos.getY());
                glVertex2f(pos.getX() + pos.getWidth(), pos.getY());
                glVertex2f(pos.getX() + pos.getWidth(), pos.getY() + pos.getHeight());
                glVertex2f(pos.getX(), pos.getY() + pos.getHeight());
                glEnd();
            }
        }

        private class KeyListenerPlugin extends BaseUIPlugin
        {
            @Override
            public void processInput(List<InputEventAPI> events)
            {
                for (InputEventAPI event : events)
                {
                    if (event.isConsumed() || !event.isKeyDownEvent()) continue;

                    final int keyCode = event.getEventValue();
                    if (keyCode == Keyboard.KEY_ESCAPE)
                    {
                        goToMenu(Menu.INPUT);
                        event.consume();
                        return;
                    }

                    // Block certain keys from being used to summon the console
                    if (keyCode == Keyboard.KEY_LSHIFT || keyCode == Keyboard.KEY_RSHIFT
                            || keyCode == Keyboard.KEY_LMETA || keyCode == Keyboard.KEY_RMETA
                            || keyCode == Keyboard.KEY_LMENU || keyCode == Keyboard.KEY_RMENU
                            || keyCode == Keyboard.KEY_LCONTROL || keyCode == Keyboard.KEY_RCONTROL
                            || keyCode == Keyboard.KEY_RETURN)
                    {
                        continue;
                    }

                    if (currentMenu == Menu.LISTEN_KEYSTROKE)
                    {
                        settings.setConsoleSummonKey(new Keystroke(keyCode, event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                        goToMenu(Menu.INPUT);
                        text.addParagraph("Console summon key set to " + settings.getConsoleSummonKey());
                    }
                    else if (currentMenu == Menu.LISTEN_SEPARATOR)
                    {
                        final char keyChar = event.getEventChar();
                        if (Character.isLetterOrDigit(keyChar)) continue;

                        settings.setCommandSeparator(String.valueOf(keyChar));
                        goToMenu(Menu.INPUT);
                        text.addParagraph("Command separator set to " + settings.getCommandSeparator());
                    }

                    event.consume();
                    return;
                }
            }
        }

        private class BaseUIPlugin implements CustomUIPanelPlugin
        {
            @Override
            public void positionChanged(PositionAPI position)
            {
            }

            @Override
            public void render(float alphaMult)
            {
            }

            @Override
            public void advance(float amount)
            {
            }

            @Override
            public void processInput(List<InputEventAPI> events)
            {
            }
        }
    }
}
