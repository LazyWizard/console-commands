package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import java.awt.Color;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.lazylib.JSONUtils;
import org.lwjgl.input.Keyboard;

public class Console
{
    private static int CONSOLE_KEY;
    private static final long MILLISECONDS_BETWEEN_INPUT = 1_500l;
    private static long lastInput = Long.MIN_VALUE;

    private static boolean checkInput(CommandContext context)
    {
        String input = JOptionPane.showInputDialog(null,
                "Enter command, or 'help' for a list of valid commands.");
        if (input == null)
        {
            return false;
        }

        String[] tmp = input.split(" ", 1);
        String com = tmp[0].toLowerCase();
        String args = (tmp.length > 1 ? tmp[1] : "");

        try
        {
            StoredCommand stored = CommandStore.retrieveCommand(com);
            if (stored == null)
            {
                Global.getLogger(Console.class).log(Level.ERROR,
                        "No such command \"" + com + "\" registered!");
                return false;
            }

            BaseCommand command = stored.getCommandClass().newInstance();
            return command.runCommand(args, context);
        }
        catch (Exception ex)
        {
            Global.getLogger(Console.class).log(Level.ERROR,
                    "Failed to execute command \"" + input
                    + "\" in context " + context, ex);
            return false;
        }
    }

    public static void reloadSettings() throws IOException, JSONException
    {
        JSONObject settings = Global.getSettings().loadJSON(
                "data/console/console_settings.json");
        CONSOLE_KEY = settings.getInt("consoleKey");

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
        if ((lastInput + MILLISECONDS_BETWEEN_INPUT <= System.currentTimeMillis())
                && Keyboard.isKeyDown(CONSOLE_KEY))
        {
            lastInput = System.currentTimeMillis();
            checkInput(context);
        }
    }
}
