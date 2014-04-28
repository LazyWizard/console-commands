package org.lazywizard.console;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.lazywizard.console.commands.RunCode;
import org.lwjgl.opengl.Display;

public class ConsoleModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        // Console settings
        Console.reloadSettings();
        CommandStore.reloadCommands();
        RunCode.loadImports();

        Console.showMessage("Console loaded.", Level.INFO);

        if (Display.isFullscreen())
        {
            Console.showMessage("It is highly recommended that you play"
                    + " Starsector in borderless windowed mode when using"
                    + " the console.", Level.WARN);
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
