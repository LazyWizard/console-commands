package org.lazywizard.console;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import java.awt.Color;
import javax.swing.UIManager;
import org.apache.log4j.Level;
import org.json.JSONObject;
import org.lazywizard.console.commands.Test;
import org.lazywizard.lazylib.JSONUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public class Console extends BaseModPlugin
{
    private static int CONSOLE_KEY;
    private static final long MILLISECONDS_BETWEEN_INPUT = 1_500l;
    private static long lastInput = Long.MIN_VALUE;

    static boolean checkInput(CommandContext context)
    {
        if ((lastInput + MILLISECONDS_BETWEEN_INPUT <= System.currentTimeMillis())
                && Keyboard.isKeyDown(CONSOLE_KEY))
        {
            lastInput = System.currentTimeMillis();

            // TODO: actually implement this!
            BaseCommand command = new Test();
            command.runCommand("This is a test!", context);
            return true;
        }

        return false;
    }


    //<editor-fold defaultstate="collapsed" desc="ModPlugin stuff">
    @Override
    public void onApplicationLoad() throws Exception
    {
        // Console settings
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

        // Load console commands
        CommandStore.reloadCommands();

        Global.getLogger(Console.class).log(Level.INFO,
                "Console loaded.");

        if (Display.isFullscreen())
        {
            Global.getLogger(Console.class).log(Level.WARN,
                    "It is highly recommended that you play Starsector"
                    + " in borderless windowed mode when using the console.");
        }
    }

    @Override
    public void onGameLoad()
    {
        Global.getSector().addScript(new ConsoleCampaignListener());
    }

    @Override
    public void beforeGameSave()
    {
        Global.getSector().removeScriptsOfClass(ConsoleCampaignListener.class);
    }

    @Override
    public void afterGameSave()
    {
        Global.getSector().addScript(new ConsoleCampaignListener());
    }
    //</editor-fold>
}
