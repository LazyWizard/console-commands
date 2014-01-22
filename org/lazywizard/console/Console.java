package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import java.awt.Color;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.lazylib.JSONUtils;
import org.lazywizard.lazylib.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

public class Console
{
    private static int CONSOLE_KEY;
    private static Color OUTPUT_COLOR;
    private static int OUTPUT_LINE_LENGTH;
    private static boolean isPressed = false;
    private static final StringBuilder output = new StringBuilder();

    private static void checkInput(CommandContext context)
    {
        String input = JOptionPane.showInputDialog(null,
                "Enter command, or 'help' for a list of valid commands.");
        if (input == null)
        {
            return;
        }

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

    public static void showMessage(String message, Level logLevel)
    {
        output.append(message);
        if (!message.endsWith("\n"))
        {
            output.append("\n");
        }

        Global.getLogger(Console.class).log(logLevel, message);
    }

    public static void showMessage(String message)
    {
        showMessage(message, Level.INFO);
    }

    public static void showException(String message, Throwable ex)
    {
        if (message == null)
        {
            message = "Error: ";
        }
        else if (!message.endsWith(" "))
        {
            message += " ";
        }

        StringBuilder stackTrace = new StringBuilder(message).append(ex.toString()).append("\n");
        
        for (StackTraceElement ste : ex.getStackTrace())
        {
            stackTrace.append("at ").append(ste.toString()).append("\n");
        }

        showMessage(stackTrace.toString());
    }

    public static void showException(Throwable ex)
    {
        showException(null, ex);
    }

    public static void reloadSettings() throws IOException, JSONException
    {
        JSONObject settings = Global.getSettings().loadJSON(
                "data/console/console_settings.json");
        CONSOLE_KEY = settings.getInt("consoleKey");
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

        // TODO: Extract this into its own method
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

                // TODO: add font size setting and modify this block to use it
                // TODO: add per-frame offset variable so multiple commands
                //       while paused don't overlap
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

    private Console()
    {
    }
}
