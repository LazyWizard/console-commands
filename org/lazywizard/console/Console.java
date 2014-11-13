package org.lazywizard.console;

import java.awt.Color;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import javax.swing.UIManager;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.lazylib.JSONUtils;
import org.lazywizard.lazylib.StringUtils;

/**
 * The main class of the console mod. Most of its methods aren't publicly
 * accessible, so this is mainly used to display messages to the player.
 * <p>
 * @author LazyWizard
 * @since 2.0
 */
public class Console
{
    private static ConsoleSettings settings;
    // Stores the output of the console until it can be displayed
    private static StringBuilder output = new StringBuilder();
    private static String lastCommand;

    private static Map<CommandResult, String> parseSoundOptions(JSONObject settings) throws JSONException
    {
        Map<CommandResult, String> sounds = new EnumMap<>(CommandResult.class);
        JSONObject json = settings.getJSONObject("playSoundOnResult");

        for (CommandResult result : CommandResult.values())
        {
            String resultId = result.name();
            if (json.has(resultId))
            {
                String soundId = json.getString(resultId);
                if (soundId != null && !soundId.isEmpty())
                {
                    sounds.put(result, soundId);
                }
            }
        }

        return sounds;
    }

    /**
     * Forces the console to reload its settings from the settings file.
     *
     * @throws IOException   if the JSON file at
     *                       {@link CommonStrings#SETTINGS_PATH} does not exist
     *                       or can't be opened.
     * @throws JSONException if the JSON is malformed or missing entries.
     * @since 2.0
     */
    public static void reloadSettings() throws IOException, JSONException
    {
        JSONObject settingsFile = Global.getSettings().loadJSON(CommonStrings.SETTINGS_PATH);
        settings = new ConsoleSettings(settingsFile.getInt("consoleKey"),
                settingsFile.getBoolean("requireShift"),
                settingsFile.getBoolean("requireControl"),
                settingsFile.getBoolean("requireAlt"),
                Pattern.quote(settingsFile.getString("commandSeparator")),
                settingsFile.getBoolean("showEnteredCommands"),
                JSONUtils.toColor(settingsFile.getJSONArray("outputColor")),
                settingsFile.getInt("maxOutputLineLength"),
                parseSoundOptions(settingsFile));

        // Set command persistence between battles
        //PersistentCommandManager.setCommandPersistence(
        //        settingsFile.getBoolean("persistentCombatCommands"));
        // What level to log console output at
        Level logLevel = Level.toLevel(settingsFile.getString("consoleLogLevel"), Level.WARN);
        Global.getLogger(Console.class).setLevel(logLevel);
        Global.getLogger(CommandStore.class).setLevel(logLevel);

        // Console pop-up appearance settings (temporary)
        Color color = JSONUtils.toColor(settingsFile.getJSONArray("backgroundColor"));
        UIManager.put("Panel.background", color);
        UIManager.put("OptionPane.background", color);
        UIManager.put("TextArea.background", color);
        UIManager.put("TextField.background", color);
        UIManager.put("Button.background", color);
        UIManager.put("SplitPane.background", color);

        color = JSONUtils.toColor(settingsFile.getJSONArray("foregroundColor"));
        UIManager.put("OptionPane.messageForeground", color);

        color = JSONUtils.toColor(settingsFile.getJSONArray("textColor"));
        UIManager.put("TextArea.foreground", color);
        UIManager.put("TextField.foreground", color);
        UIManager.put("TextField.caretForeground", color);

        color = JSONUtils.toColor(settingsFile.getJSONArray("buttonColor"));
        UIManager.put("Button.foreground", color);
        UIManager.put("SplitPane.foreground", color);
    }

    public static ConsoleSettings getSettings()
    {
        return settings;
    }

    static String getLastCommand()
    {
        return lastCommand;
    }

    //<editor-fold defaultstate="collapsed" desc="showMessage variants">
    /**
     * Displays a message to the user. The message will be formatted and shown
     * to the player when they reach a section of the game where it can be
     * displayed properly (combat/campaign map).
     * <p>
     * @param message  The message to show.
     * @param logLevel If this is equal to/higher than the "consoleLogLevel"
     *                 setting, this message will be logged in Starsector.log.
     * <p>
     * @since 2.0
     */
    public static void showMessage(String message, Level logLevel)
    {
        // Paste directly to log
        Global.getLogger(Console.class).log(logLevel, message);

        // Word-wrap message and add it to the output queue
        message = StringUtils.wrapString(message, settings.getMaxOutputLineLength());
        output.append(message);
        if (!message.endsWith("\n"))
        {
            output.append('\n');
        }
    }

    /**
     * Displays a message to the user. The message will be formatted and shown
     * to the player when they reach a section of the game where it can be
     * displayed properly (combat/campaign map).
     * <p>
     * @param message The message to show.
     * <p>
     * @since 2.0
     */
    public static void showMessage(String message)
    {
        showMessage(message, Level.INFO);
    }

    /**
     * Displays the stack trace of a {@link Throwable}.
     * <p>
     * @param message An optional message to show before the stack trace. Can be
     *                {@code null}.
     * @param ex      The {@link Throwable} whose stack trace will be shown.
     * <p>
     * @since 2.0
     */
    public static void showException(String message, Throwable ex)
    {
        StringBuilder stackTrace = new StringBuilder(256);

        // Add message if one was entered
        if (message != null)
        {
            stackTrace.append(message);
            if (!message.endsWith("\n"))
            {
                stackTrace.append("\n");
            }
        }

        // Add stack trace of Throwable
        stackTrace.append(ex.toString()).append("\n");
        for (StackTraceElement ste : ex.getStackTrace())
        {
            stackTrace.append("   at ").append(ste.toString()).append("\n");
        }

        showMessage(stackTrace.toString(), Level.ERROR);
    }

    public static void showDialogOnClose(InteractionDialogPlugin dialog,
            SectorEntityToken token)
    {
        Global.getSector().addScript(new ShowDialogOnCloseScript(dialog, null));
    }

    public static void showDialogOnClose(SectorEntityToken token)
    {
        Global.getSector().addScript(new ShowDialogOnCloseScript(null, token));
    }
    //</editor-fold>

    private static CommandResult runCommand(String input, CommandContext context)
    {
        String[] tmp = input.split(" ", 2);
        String com = tmp[0].toLowerCase();
        String args = (tmp.length > 1 ? tmp[1] : "");
        CommandResult result;

        try
        {
            StoredCommand stored = CommandStore.retrieveCommand(com);
            if (stored == null)
            {
                showMessage("No such command \"" + com + "\" registered!", Level.ERROR);
                return CommandResult.ERROR;
            }

            if (settings.getShouldShowEnteredCommands())
            {
                showMessage("Running command \"" + input + "\"");
            }

            BaseCommand command = stored.getCommandClass().newInstance();
            result = command.runCommand(args, context);

            if (result == CommandResult.BAD_SYNTAX
                    && !stored.getSyntax().isEmpty())
            {
                showMessage("Syntax: " + stored.getSyntax());
            }

        }
        catch (Exception ex)
        {
            showException("Failed to execute command \"" + input
                    + "\" in context " + context, ex);
            return CommandResult.ERROR;
        }

        return result;
    }

    static void parseInput(String rawInput, CommandContext context)
    {
        if (rawInput == null)
        {
            return;
        }

        lastCommand = rawInput;

        // Runcode ignores separators
        // Hopefully the ONLY hardcoded command support I'll add to this mod...
        CommandResult worstResult;
        if (rawInput.length() >= 7 && rawInput.substring(0, 7).equalsIgnoreCase("runcode"))
        {
            worstResult = runCommand(rawInput, context);
        }
        else
        {
            // Split the raw input up into the individual commands
            // The command separator is used to separate multiple commands
            Set<CommandResult> results = new HashSet<>();
            worstResult = CommandResult.SUCCESS;
            for (String input : rawInput.split(settings.getCommandSeparator()))
            {
                input = input.trim();
                if (!input.isEmpty())
                {
                    results.add(runCommand(input, context));
                }
            }

            // Find 'worst' result of executed commands
            for (CommandResult tmp : results)
            {
                if (tmp.ordinal() > worstResult.ordinal())
                {
                    worstResult = tmp;
                }
            }
        }

        // Play a sound based on worst error type
        String sound = settings.getSoundForResult(worstResult);
        if (sound != null)
        {
            Global.getSoundPlayer().playUISound(sound, 1f, 1f);
        }
    }

    private static void showOutput(ConsoleListener listener)
    {
        if (output.length() > 0)
        {
            listener.showOutput(output.toString());
            output = new StringBuilder();
        }
    }

    static void advance(float amount, ConsoleListener listener)
    {
        // Just check the output queue for now
        //PersistentCommandManager.advance(amount, listener);
        showOutput(listener);
    }

    private static class ShowDialogOnCloseScript implements EveryFrameScript
    {
        private final SectorEntityToken token;
        private final InteractionDialogPlugin dialog;
        private boolean isDone = false;

        private ShowDialogOnCloseScript(InteractionDialogPlugin dialog,
                SectorEntityToken token)
        {
            this.dialog = dialog;
            this.token = token;
        }

        @Override
        public boolean isDone()
        {
            return isDone;
        }

        @Override
        public boolean runWhilePaused()
        {
            return false;
        }

        @Override
        public void advance(float amount)
        {
            if (!isDone)
            {
                isDone = true;

                try
                {
                    CampaignUIAPI ui = Global.getSector().getCampaignUI();
                    if (dialog == null)
                    {
                        ui.showInteractionDialog(token);
                    }
                    else
                    {
                        ui.showInteractionDialog(dialog, token);
                    }
                }
                catch (Exception ex)
                {
                    Console.showException("Failed to open dialog "
                            + dialog.getClass().getCanonicalName(), ex);
                    Global.getSector().getCampaignUI().getCurrentInteractionDialog().dismiss();
                }
            }
        }
    }

    private Console()
    {
    }
}
