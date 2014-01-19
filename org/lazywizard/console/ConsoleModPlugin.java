package org.lazywizard.console;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.lwjgl.opengl.Display;

public class ConsoleModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        // Console settings
        Console.reloadSettings();
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
}
