package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
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
        private Menu currentMenu;
        private boolean showCommands, showExceptions, showIndex;
        private int r, g, b, scrollback;
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
            SHOW_EXCEPTIONS,
            SHOW_INDEX,
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
            options = dialog.getOptionPanel();
            dialog.getTextPanel().addParagraph(""); // TODO: Provide instructions

            final Color outputColor = settings.getOutputColor();
            r = outputColor.getRed();
            g = outputColor.getGreen();
            b = outputColor.getBlue();
            scrollback = settings.getMaxScrollback();
            threshold = settings.getTypoCorrectionThreshold();
            showCommands = settings.getShouldShowEnteredCommands();
            showExceptions = settings.getShouldShowExceptionDetails();
            showIndex = settings.getShouldShowCursorIndex();

            goToMenu(Menu.MAIN);
        }

        private void goToMenu(Menu menu)
        {
            options.clearOptions();
            dialog.getVisualPanel().fadeVisualOut();
            dialog.setPromptText("");
            final float barWidth = 510f;

            // TODO: Add tooltips where applicable
            switch (menu)
            {
                case MAIN:
                    options.addOption("Input settings", Menu.INPUT);
                    options.addOption("Color settings", Menu.COLOR);
                    options.addOption("Display settings", Menu.DISPLAY);
                    options.addOption("Misc settings", Menu.MISC);
                    options.addOption("Save and exit", Option.EXIT);
                    options.setShortcut(Option.EXIT, Keyboard.KEY_ESCAPE, false, false, false, true);
                    break;
                case INPUT:
                    options.addOption("Set console overlay key", Menu.LISTEN_KEYSTROKE, null);
                    options.addOption("Set command separator", Menu.LISTEN_SEPARATOR, null);
                    break;
                case LISTEN_KEYSTROKE:
                    dialog.getTextPanel().addParagraph("Press the key combination you would like to summon the console with." +
                            " Currently the console is summoned with " + settings.getConsoleSummonKey() + ".");
                    dialog.getVisualPanel().showCustomPanel(0f, 0f, new KeyListenerPlugin());
                    break;
                case LISTEN_SEPARATOR:
                    dialog.getTextPanel().addParagraph("Enter the character you would like to separate multiple commands with." +
                            " Currently multiple commands are separated with '" + settings.getCommandSeparator() + "'.");
                    dialog.getVisualPanel().showCustomPanel(0f, 0f, new KeyListenerPlugin());
                    break;
                case COLOR:
                    // Console overlay font color
                    options.addSelector("Output Color (red)", Selector.COLOR_R, Color.RED, barWidth, 150f, 0f, 255f, ValueDisplayMode.X_OVER_Y_NO_SPACES, null);
                    options.setSelectorValue(Selector.COLOR_R, r);
                    options.addSelector("Output Color (green)", Selector.COLOR_G, Color.GREEN, barWidth, 150f, 0f, 255f, ValueDisplayMode.X_OVER_Y_NO_SPACES, null);
                    options.setSelectorValue(Selector.COLOR_G, g);
                    options.addSelector("Output Color (blue)", Selector.COLOR_B, Color.BLUE, barWidth, 150f, 0f, 255f, ValueDisplayMode.X_OVER_Y_NO_SPACES, null);
                    options.setSelectorValue(Selector.COLOR_B, b);
                    dialog.getVisualPanel().showCustomPanel(50f, 50f, new ColorDisplayPlugin());
                    break;
                case DISPLAY:
                    // Misc display options
                    options.addOption("Show entered commands: " + (showCommands ? "true" : "false"), Option.SHOW_COMMANDS);
                    options.addOption("Show error stack traces: " + (showExceptions ? "true" : "false"), Option.SHOW_EXCEPTIONS);
                    options.addOption("Show cursor index: " + (showIndex ? "true" : "false"), Option.SHOW_INDEX);
                    break;
                case MISC:
                    // Typo correction sensitivity
                    options.addSelector("Typo correction threshold", Selector.TYPO_THRESHOLD, Color.WHITE, barWidth, 150f, 0f, 1f, ValueDisplayMode.PERCENT, null);
                    options.setSelectorValue(Selector.TYPO_THRESHOLD, threshold);

                    // Max scrollback (in characters)
                    options.addSelector("Max scrollback (in characters)", Selector.MAX_SCROLLBACK, Color.WHITE, barWidth, 150f, 0f, 20_000f, ValueDisplayMode.VALUE, null);
                    options.setSelectorValue(Selector.MAX_SCROLLBACK, scrollback);
                    break;
            }

            if (menu != Menu.MAIN)
            {
                options.addOption("Back", Menu.MAIN);
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
                case SHOW_INDEX:
                    showIndex = !showIndex;
                    goToMenu(Menu.DISPLAY);
                    break;
                case SHOW_EXCEPTIONS:
                    showExceptions = !showExceptions;
                    goToMenu(Menu.DISPLAY);
                    break;
                case EXIT:
                    saveOptions();
                    dialog.dismiss();
            }
        }

        private void saveOptions()
        {
            settings.setOutputColor(new Color(r, g, b));
            settings.setTypoCorrectionThreshold(threshold);
            settings.setMaxScrollback(scrollback);
            settings.setShouldShowCursorIndex(showIndex);
            settings.setShouldShowEnteredCommands(showCommands);
            settings.setShouldShowExceptionDetails(showExceptions);
        }

        @Override
        public void advance(float amount)
        {
            if (currentMenu == Menu.COLOR)
            {
                // Clamp color components to int
                r = (int) options.getSelectorValue(Selector.COLOR_R);
                g = (int) options.getSelectorValue(Selector.COLOR_G);
                b = (int) options.getSelectorValue(Selector.COLOR_B);
                options.setSelectorValue(Selector.COLOR_R, r);
                options.setSelectorValue(Selector.COLOR_G, g);
                options.setSelectorValue(Selector.COLOR_B, b);
            }
            else if (currentMenu == Menu.MISC)
            {
                scrollback = ((int) (options.getSelectorValue(Selector.MAX_SCROLLBACK) / 100f)) * 100;
                options.setSelectorValue(Selector.MAX_SCROLLBACK, scrollback);
                threshold = options.getSelectorValue(Selector.TYPO_THRESHOLD);
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

        private class ColorDisplayPlugin implements CustomUIPanelPlugin
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
                glColor3ub((byte) (int) options.getSelectorValue(Selector.COLOR_R),
                        (byte) (int) options.getSelectorValue(Selector.COLOR_G),
                        (byte) (int) options.getSelectorValue(Selector.COLOR_B));
                glBegin(GL_QUADS);
                glVertex2f(pos.getX(), pos.getY());
                glVertex2f(pos.getX() + pos.getWidth(), pos.getY());
                glVertex2f(pos.getX() + pos.getWidth(), pos.getY() + pos.getHeight());
                glVertex2f(pos.getX(), pos.getY() + pos.getHeight());
                glEnd();
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

        private class KeyListenerPlugin implements CustomUIPanelPlugin
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
                        dialog.getTextPanel().addParagraph("Console summon key set to " + settings.getConsoleSummonKey());
                    }
                    else if (currentMenu == Menu.LISTEN_SEPARATOR)
                    {
                        final char keyChar = event.getEventChar();
                        if (Character.isLetterOrDigit(keyChar)) continue;

                        settings.setCommandSeparator(String.valueOf(keyChar));
                        dialog.getTextPanel().addParagraph("Command separator set to " + settings.getCommandSeparator());
                    }

                    event.consume();
                    goToMenu(Menu.INPUT);
                    return;
                }
            }
        }
    }
}
