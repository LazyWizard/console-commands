package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import java.awt.Color;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.console.LazyLibTemp.JSONUtils;
import org.lazywizard.console.LazyLibTemp.StringUtils;
//import org.lazywizard.lazylib.JSONUtils;
//import org.lazywizard.lazylib.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

public class Console
{
    // The LWJGL constant of the key that summons the console
    private static int CONSOLE_KEY;
    // The String (usually a single character) that separates multiple commands
    private static String COMMAND_SEPARATOR;
    // The color of the console's output text
    private static Color OUTPUT_COLOR;
    // How many characters before the output is line-wrapped
    private static int OUTPUT_LINE_LENGTH;
    // Here to fix a LWJGL input bug that will probably never be fixed
    private static boolean isPressed = false;
    // Stores the output of the console until it can be displayed
    private static final StringBuilder output = new StringBuilder();

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
        output.append(message);
        if (!message.endsWith("\n"))
        {
            output.append("\n");
        }

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
            stackTrace.append("at ").append(ste.toString()).append("\n");
        }

        showMessage(stackTrace.toString());
    }

    public static void reloadSettings() throws IOException, JSONException
    {
        JSONObject settings = Global.getSettings().loadJSON(
                "data/console/console_settings.json");
        CONSOLE_KEY = settings.getInt("consoleKey");
        COMMAND_SEPARATOR = Pattern.quote(settings.getString("commandSeparator"));
        OUTPUT_COLOR = JSONUtils.toColor(settings.getJSONArray("outputColor"));
        OUTPUT_LINE_LENGTH = settings.getInt("maxOutputLineLength");

        // What level to log console output at
        Level logLevel = Level.toLevel(settings.getString("consoleLogLevel"), Level.WARN);
        Global.getLogger(Console.class).setLevel(logLevel);
        Global.getLogger(CommandStore.class).setLevel(logLevel);
        Global.getLogger(ConsoleCampaignListener.class).setLevel(logLevel);
        Global.getLogger(ConsoleCombatListener.class).setLevel(logLevel);

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
            showMessage("Failed to execute command \"" + input
                    + "\" in context " + context, Level.ERROR);
        }
    }

    private static void checkInput(CommandContext context)
    {
        String rawInput = JOptionPane.showInputDialog(null,
                "Enter command, or 'help' for a list of valid commands.");

        if (rawInput == null)
        {
            return;
        }

        // Hopefully the ONLY hardcoded command support I'll add to this mod...
        if (rawInput.length() >= 7 && rawInput.substring(0, 7).equalsIgnoreCase("runcode"))
        {
            runCommand(rawInput, context);
        }
        else
        {
            for (String input : rawInput.split(COMMAND_SEPARATOR))
            {
                input = input.trim();
                if (!input.isEmpty())
                {
                    runCommand(input, context);
                }
            }
        }
    }

    private static void checkShowOutput(CommandContext context)
    {
        if (output.length() > 0)
        {
            if (context == CommandContext.CAMPAIGN)
            {
                for (String message : StringUtils.wrapString(output.toString(),
                        OUTPUT_LINE_LENGTH).split("\n"))
                {
                    Global.getSector().getCampaignUI().addMessage(message, OUTPUT_COLOR);
                }

                output.setLength(0);
            }
            else
            {
                CombatEngineAPI engine = Global.getCombatEngine();
                ShipAPI player = engine.getPlayerShip();

                if (player == null || !engine.isEntityInPlay(player))
                {
                    // Print output later, once there's a player on-screen
                    return;
                }

                // TODO: the values here are kind of arbitrary, need to be worked out properly
                // TODO: add per-frame offset variable so multiple commands while paused don't overlap
                String[] messages = StringUtils.wrapString(output.toString(),
                        OUTPUT_LINE_LENGTH).split("\n");
                for (int x = 0; x < messages.length; x++)
                {
                    engine.addFloatingText(Vector2f.add(
                            new Vector2f(-output.length() / 20f,
                                    -(player.getCollisionRadius() + 50 + (x * 25))),
                            player.getLocation(), null),
                            messages[x], 25f, OUTPUT_COLOR, player, 0f, 0f);
                }

                output.setLength(0);
            }
        }
    }

    static void advance(CommandContext context)
    {
        if (!isPressed)
        {
            if (Keyboard.isKeyDown(CONSOLE_KEY))
            {
                isPressed = true;
            }
        }
        else
        {
            if (!Keyboard.isKeyDown(CONSOLE_KEY))
            {
                isPressed = false;
                checkInput(context);
            }
        }

        checkShowOutput(context);
    }

    private Console()
    {
    }
}
