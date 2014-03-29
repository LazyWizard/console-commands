package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import java.awt.Color;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.swing.UIManager;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.lazylib.JSONUtils;
import org.lazywizard.lazylib.StringUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 * The main class of the console mod. Most of its methods aren't publicly
 * accessible, so this is mainly used to display messages to the player.
 * <p>
 * @author LazyWizard
 * @since 2.0
 */
public class Console
{
    private static ConsoleSettings SETTINGS;
    // Stores the output of the console until it can be displayed
    private static StringBuilder output = new StringBuilder();

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
        JSONObject settings = Global.getSettings().loadJSON(CommonStrings.SETTINGS_PATH);
        SETTINGS = new ConsoleSettings(new KeyStroke(settings.getInt("consoleKey"),
                settings.getBoolean("requireShift"),
                settings.getBoolean("requireControl"),
                settings.getBoolean("requireAlt")),
                Pattern.quote(settings.getString("commandSeparator")),
                JSONUtils.toColor(settings.getJSONArray("outputColor")),
                settings.getInt("maxOutputLineLength"));

        ConsoleCombatListener.setCommandPersistence(
                settings.getBoolean("persistentCombatCommands"));

        // What level to log console output at
        Level logLevel = Level.toLevel(settings.getString("consoleLogLevel"), Level.WARN);
        Global.getLogger(Console.class).setLevel(logLevel);
        Global.getLogger(CommandStore.class).setLevel(logLevel);

        // Console pop-up appearance settings (temporary)
        Color color = JSONUtils.toColor(settings.getJSONArray("backgroundColor"));
        UIManager.put("Panel.background", color);
        UIManager.put("OptionPane.background", color);
        UIManager.put("TextArea.background", color);
        UIManager.put("TextField.background", color);
        UIManager.put("Button.background", color);
        UIManager.put("SplitPane.background", color);

        color = JSONUtils.toColor(settings.getJSONArray("foregroundColor"));
        UIManager.put("OptionPane.messageForeground", color);

        color = JSONUtils.toColor(settings.getJSONArray("textColor"));
        UIManager.put("TextArea.foreground", color);
        UIManager.put("TextField.foreground", color);
        UIManager.put("TextField.caretForeground", color);

        color = JSONUtils.toColor(settings.getJSONArray("buttonColor"));
        UIManager.put("Button.foreground", color);
        UIManager.put("SplitPane.foreground", color);
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
        output.append(StringUtils.wrapString(message, SETTINGS.OUTPUT_MAX_LINE_LENGTH));
        Global.getLogger(Console.class).log(logLevel, message);
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
            stackTrace.append(message).append("\n");
        }

        // Add stack trace of Throwable
        stackTrace.append(ex.toString()).append("\n");
        for (StackTraceElement ste : ex.getStackTrace())
        {
            stackTrace.append("\tat ").append(ste.toString()).append("\n");
        }

        showMessage(stackTrace.toString(), Level.ERROR);
    }
    //</editor-fold>

    public static ConsoleSettings getSettings()
    {
        return SETTINGS;
    }

    private static void runCommand(String input, CommandContext context)
    {
        String[] tmp = input.split(" ", 2);
        String com = tmp[0].toLowerCase();
        String args = (tmp.length > 1 ? tmp[1] : "");

        try
        {
            StoredCommand stored = CommandStore.retrieveCommand(com);
            if (stored == null)
            {
                showMessage("No such command \"" + com + "\" registered!", Level.ERROR);
                return;
            }

            showMessage("Running command \"" + input + "\"");
            BaseCommand command = stored.getCommandClass().newInstance();
            CommandResult result = command.runCommand(args, context);

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
        }
    }

    static void parseInput(String rawInput, CommandContext context)
    {
        if (rawInput == null)
        {
            return;
        }

        // Runcode ignores separators
        // Hopefully the ONLY hardcoded command support I'll add to this mod...
        if (rawInput.length() >= 7 && rawInput.substring(0, 7).equalsIgnoreCase("runcode"))
        {
            runCommand(rawInput, context);
        }
        else
        {
            // Split the raw input up into the individual commands
            // The command separator is used to separate multiple commands
            for (String input : rawInput.split(SETTINGS.COMMAND_SEPARATOR))
            {
                input = input.trim();
                if (!input.isEmpty())
                {
                    runCommand(input, context);
                }
            }
        }
    }

    private static void showOutput(BaseConsoleListener listener)
    {
        if (output.length() > 0)
        {
            listener.showOutput(output.toString());
            output = new StringBuilder();
        }
    }

    static void advance(BaseConsoleListener listener)
    {
        // Just check the output queue for now
        showOutput(listener);
    }

    private Console()
    {
    }

    public static class ConsoleSettings
    {
        // The key stroke that summons the console pop-up
        private final KeyStroke CONSOLE_SUMMON_KEY;
        // The String (usually a single character) that separates multiple commands
        private final String COMMAND_SEPARATOR;
        // The color of the console's output text
        private final Color OUTPUT_COLOR;
        // How many characters before the output is line-wrapped
        private final int OUTPUT_MAX_LINE_LENGTH;

        private ConsoleSettings(KeyStroke consoleSummonKey, String commandSeparator,
                Color outputColor, int outputMaxLineLength)
        {
            CONSOLE_SUMMON_KEY = consoleSummonKey;
            COMMAND_SEPARATOR = commandSeparator;
            OUTPUT_COLOR = outputColor;
            OUTPUT_MAX_LINE_LENGTH = outputMaxLineLength;
        }

        public KeyStroke getConsoleSummonKey()
        {
            return CONSOLE_SUMMON_KEY;
        }

        public String getCommandSeparator()
        {
            return COMMAND_SEPARATOR;
        }

        public Color getOutputColor()
        {
            return OUTPUT_COLOR;
        }

        public int getMaxOutputLineLength()
        {
            return OUTPUT_MAX_LINE_LENGTH;
        }
    }
}
