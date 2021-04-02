package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.ValueDisplayMode;
import org.lazywizard.console.ConsoleSettings.Keystroke;
import org.lazywizard.console.cheatmanager.CheatTarget;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    // TODO: Make Color menu to a submenu of Display (if there's room)
    public static class SettingsDialog implements InteractionDialogPlugin
    {
        private final ConsoleSettings settings = Console.getSettings();
        private InteractionDialogAPI dialog;
        private OptionPanelAPI options;
        private TextPanelAPI text;
        private Menu currentMenu;
        private boolean showBackground, showCommands, showMemory, showExceptions, showIndex, homeStorage, devModeFlags;
        private int red, green, blue, scrollback;
        private float threshold, fontScale;
        private CheatTarget defaultTarget;

        private enum Menu
        {
            MAIN,
            INPUT,
            TEXT,
            OVERLAY,
            MISC,
            CHEAT_CONFIRM,
            LISTEN_KEYSTROKE,
            LISTEN_SEPARATOR
        }

        private enum Option
        {
            SHOW_BACKGROUND,
            SHOW_COMMANDS,
            SHOW_MEMORY,
            SHOW_EXCEPTIONS,
            SHOW_INDEX,
            DEFAULT_TARGET,
            HOME_STORAGE,
            DEVMODE_FLAGS,
            TEST_COLOR,
            DISABLE_CHEATS,
            EXIT
        }

        private enum Selector
        {
            COLOR_R,
            COLOR_G,
            COLOR_B,
            TYPO_THRESHOLD,
            MAX_SCROLLBACK,
            TEXT_SCALE
        }

        protected static String genCheatId()
        {
            return Global.getSector().getSeedString() + "lwc";
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
            fontScale = settings.getFontScaling();
            scrollback = settings.getMaxScrollback();
            threshold = settings.getTypoCorrectionThreshold();
            defaultTarget = settings.getDefaultCombatCheatTarget();
            homeStorage = settings.getUseHomeForStorage();
            devModeFlags = settings.getDevModeTogglesDebugFlags();
            showBackground = settings.getShowBackground();
            showCommands = settings.getShowEnteredCommands();
            showMemory = settings.getShowMemoryUsage();
            showExceptions = settings.getShowExceptionDetails();
            showIndex = settings.getShowCursorIndex();

            goToMenu(Menu.MAIN);
        }

        private static Color getToggleOptionColor(boolean isEnabled)
        {
            return (isEnabled ? Color.GREEN : Color.ORANGE);
        }

        private static <T extends Enum> T cycleEnum(T toCycle)
        {
            final Object[] values = toCycle.getDeclaringClass().getEnumConstants();
            final int index = (toCycle.ordinal() >= values.length - 1 ? 0 : toCycle.ordinal() + 1);
            return (T) values[index];
        }

        private void goToMenu(Menu menu)
        {
            text.clear();
            options.clearOptions();
            dialog.getVisualPanel().showCustomPanel(0f, 0f, new BaseUIPlugin()); // Needed due to vanilla bug
            dialog.hideVisualPanel();
            dialog.setPromptText("");
            final float barWidth = Math.min(Display.getWidth() * 0.7f, 800f);

            switch (menu)
            {
                case MAIN:
                    text.addParagraph(""); // TODO: Description
                    options.addOption("Input settings", Menu.INPUT,
                            "Customize how the console is summoned and how commands are entered.");
                    options.addOption("Overlay settings", Menu.OVERLAY,
                            "Customize the console overlay.");
                    options.addOption("Misc console settings", Menu.MISC,
                            "Customize the behavior of the console itself.");
                    options.addOption("Save and exit", Option.EXIT, "Save settings and exit.");
                    options.setShortcut(Option.EXIT, Keyboard.KEY_ESCAPE, false, false, false, true);
                    break;
                case INPUT:
                    text.addParagraph(""); // TODO: Description
                    options.addOption("Set console overlay key (current: " + settings.getConsoleSummonKey() + ")",
                            Menu.LISTEN_KEYSTROKE, "Sets the key combination used to open the console overlay.");
                    options.addOption("Set command separator (current: " + settings.getCommandSeparator() + ")",
                            Menu.LISTEN_SEPARATOR, "Sets the character used to separate multiple commands.");
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
                case TEXT:
                    text.addParagraph("This menu allows you to customize the text of the console overlay.");

                    // Console overlay font size
                    options.addSelector("Text Scaling Percentage", Selector.TEXT_SCALE, Color.WHITE, barWidth, 150f, 50, 400, ValueDisplayMode.VALUE,
                            "Scale displayed text by this percentage of the base font size.");
                    options.setSelectorValue(Selector.TEXT_SCALE, (int) (fontScale * 100));

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
                    options.addOption("Print current color", Option.TEST_COLOR, "Prints an example of the current color for easier comparisons.");
                    dialog.getVisualPanel().showCustomPanel(50f, 50f, new ColorDisplayPlugin());
                    optionSelected("Print current color", Option.TEST_COLOR); // Show the starting color
                    break;
                case OVERLAY:
                    text.addParagraph(""); // TODO: Description

                    // Misc overlay options
                    options.addOption("Text settings", Menu.TEXT,
                            "Customize the size and color of the console overlay's text.");
                    options.addOption("Show background: " + (showBackground ? "true" : "false"),
                            Option.SHOW_BACKGROUND, getToggleOptionColor(showBackground),
                            "Whether to show the paused game in the background of the overlay.");
                    options.addOption("Show entered commands: " + (showCommands ? "true" : "false"),
                            Option.SHOW_COMMANDS, getToggleOptionColor(showCommands),
                            "Whether to show the commands you've entered in the overlay.");
                    options.addOption("Show memory usage: " + (showMemory ? "true" : "false"),
                            Option.SHOW_MEMORY, getToggleOptionColor(showMemory),
                            "Whether to show Starsector's current memory usage at the top of the console overlay.");
                    options.addOption("Show error stack traces: " + (showExceptions ? "true" : "false"),
                            Option.SHOW_EXCEPTIONS, getToggleOptionColor(showExceptions),
                            "Whether to show exception stack traces when something goes wrong. This can be very spammy!\n" +
                                    "Exceptions are always saved to starsector.log, so this is only useful for developers.");
                    options.addOption("Show cursor index (debug): " + (showIndex ? "true" : "false"),
                            Option.SHOW_INDEX, getToggleOptionColor(showIndex),
                            "Whether to show debug information in the overlay's input text field.");
                    break;
                case MISC:
                    text.addParagraph(""); // TODO: Description

                    // Typo correction sensitivity
                    options.addSelector("Typo detection threshold", Selector.TYPO_THRESHOLD, Color.WHITE, barWidth, 150f, 0.5f, 1f, ValueDisplayMode.PERCENT,
                            "Controls how sensitive typo detection is. A higher value means fewer (but more accurate) suggestions when you enter something wrong.");
                    options.setSelectorValue(Selector.TYPO_THRESHOLD, threshold);

                    // Max scrollback (in characters)
                    options.addSelector("Max scrollback (in characters)", Selector.MAX_SCROLLBACK, Color.WHITE, barWidth, 150f, 0f, 20_000f, ValueDisplayMode.VALUE,
                            "How many characters of output history will be stored in the overlay between uses. A higher value means slightly more RAM used by the console.");
                    options.setSelectorValue(Selector.MAX_SCROLLBACK, scrollback);

                    // Default target for combat cheats
                    options.addOption("Default combat cheat target: " + defaultTarget, Option.DEFAULT_TARGET,
                            "The default target for combat cheat commands when no argument is passed in.");

                    // Use Home market as Storage
                    options.addOption("Always use Home's market for Storage: " + (homeStorage ? "true" : "false"),
                            Option.HOME_STORAGE, getToggleOptionColor(homeStorage),
                            "When enabled, the contents of your Storage will automatically transfer to your Home's storage submarket if it has one.");

                    // Whether the devmode command also resets the flags in DebugFlags
                    options.addOption("Toggle debug flags with DevMode: " + (devModeFlags ? "true" : " false"),
                            Option.DEVMODE_FLAGS, getToggleOptionColor(devModeFlags),
                            "When enabled, toggling devmode will also reset all debug flags (faction control override, etc).");

                    // Disable all cheat commands for the current save
                    options.addOption("Disable cheats", Menu.CHEAT_CONFIRM, "Disables all commands that can be used to gain an unfair advantage.");
                    options.setEnabled(Menu.CHEAT_CONFIRM, settings.getCheatsAllowedForSave());
                    break;
                case CHEAT_CONFIRM:
                    final List<String> legalCommands = CommandStore.getLoadedCommands();
                    legalCommands.removeAll(CommandStore.getCommandsWithTag(CommonStrings.CHEAT_TAG));
                    Collections.sort(legalCommands);

                    text.addParagraph("Warning: this will permanently disable cheat codes for the current save!");
                    text.highlightInLastPara(Color.RED, "permanently");

                    text.addParagraph("\nThe only commands you'll be able to use are the following:");
                    text.highlightFirstInLastPara("only", Color.RED);

                    text.addParagraph(CollectionUtils.implode(legalCommands) +
                            ".\n\nPress 'confirm' to disable all other commands for this save.");
                    text.highlightLastInLastPara("confirm", Color.YELLOW);

                    options.addOption("Confirm", Option.DISABLE_CHEATS, "Warning: this cannot be undone!");
                    break;
            }

            // Add back button
            if (menu == Menu.TEXT)
            {
                options.addOption("Back", Menu.OVERLAY, "Return to the overlay settings menu.");
                options.setShortcut(Menu.OVERLAY, Keyboard.KEY_ESCAPE, false, false, false, true);
            }
            else if (menu == Menu.CHEAT_CONFIRM)
            {
                options.addOption("Cancel", Menu.MISC, "Return to the misc settings menu.");
                options.setShortcut(Menu.MISC, Keyboard.KEY_ESCAPE, false, false, false, true);
            }
            else if (menu != Menu.MAIN)
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
                case SHOW_BACKGROUND:
                    showBackground = !showBackground;
                    goToMenu(Menu.OVERLAY);
                    break;
                case SHOW_COMMANDS:
                    showCommands = !showCommands;
                    goToMenu(Menu.OVERLAY);
                    break;
                case SHOW_MEMORY:
                    showMemory = !showMemory;
                    goToMenu(Menu.OVERLAY);
                    break;
                case SHOW_INDEX:
                    showIndex = !showIndex;
                    goToMenu(Menu.OVERLAY);
                    break;
                case SHOW_EXCEPTIONS:
                    showExceptions = !showExceptions;
                    goToMenu(Menu.OVERLAY);
                    break;
                case DEFAULT_TARGET:
                    defaultTarget = cycleEnum(defaultTarget);
                    goToMenu(Menu.MISC);
                    break;
                case HOME_STORAGE:
                    homeStorage = !homeStorage;
                    goToMenu(Menu.MISC);
                    break;
                case DEVMODE_FLAGS:
                    devModeFlags = !devModeFlags;
                    goToMenu(Menu.MISC);
                    break;
                case TEST_COLOR:
                    text.addParagraph("Here is what the console's output would look like with color {"
                            + red + ", " + green + ", " + blue + "}.", new Color(red, green, blue));
                    break;
                case DISABLE_CHEATS:
                    Global.getSector().getPersistentData().put(genCheatId(), System.nanoTime());
                    goToMenu(Menu.MISC);
                    text.addPara("Cheats have been permanently disabled for this save.\n", Color.RED);
                    break;
                case EXIT:
                    saveOptions();
                    dialog.dismiss();
            }
        }

        private void saveOptions()
        {
            settings.setOutputColor(new Color(red, green, blue));
            settings.setFontScaling(fontScale);
            settings.setTypoCorrectionThreshold(threshold);
            settings.setMaxScrollback(scrollback);
            settings.setDefaultCombatCheatTarget(defaultTarget);
            settings.setUseHomeForStorage(homeStorage);
            settings.setDevModeTogglesDebugFlags(devModeFlags);
            settings.setShowBackground(showBackground);
            settings.setShowEnteredCommands(showCommands);
            settings.setShowMemoryUsage(showMemory);
            settings.setShowCursorIndex(showIndex);
            settings.setShowExceptionDetails(showExceptions);
        }

        @Override
        public void advance(float amount)
        {
            if (currentMenu == Menu.TEXT)
            {
                // Clamp color components to int
                red = (int) options.getSelectorValue(Selector.COLOR_R);
                green = (int) options.getSelectorValue(Selector.COLOR_G);
                blue = (int) options.getSelectorValue(Selector.COLOR_B);
                fontScale = ((int) options.getSelectorValue(Selector.TEXT_SCALE)) / 100f;
                options.setSelectorValue(Selector.COLOR_R, red);
                options.setSelectorValue(Selector.COLOR_G, green);
                options.setSelectorValue(Selector.COLOR_B, blue);
            }
            else if (currentMenu == Menu.MISC)
            {
                scrollback = Math.round(options.getSelectorValue(Selector.MAX_SCROLLBACK) / 100f) * 100;
                options.setSelectorValue(Selector.MAX_SCROLLBACK, scrollback);
                threshold = Math.round(options.getSelectorValue(Selector.TYPO_THRESHOLD) * 100f) * 0.01f;
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
            private final DrawableString testString = Console.getFont().createText(
                    "This is what the console's text would\nlook like with the current settings.",
                    Console.getSettings().getOutputColor());

            @Override
            public void positionChanged(PositionAPI position)
            {
                pos = position;
            }

            @Override
            public void render(float alphaMult)
            {
                if (pos == null) return;

                // FIXME: Track down cause of DisplayString corruption
                testString.setColor(new Color(red, green, blue));
                testString.setFontSize(Console.getFont().getBaseHeight() * fontScale);
                testString.draw(pos.getX(), pos.getY());
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
                        settings.setConsoleSummonKey(new Keystroke(keyCode, event.isCtrlDown(), event.isAltDown(), event.isShiftDown()));
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
            public void renderBelow(float alphaMult)
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
