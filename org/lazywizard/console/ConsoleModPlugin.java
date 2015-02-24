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
        RunCode.reloadImports();
        RunCode.reloadMacros();

        Console.showMessage("Console loaded, summon with "
                + Console.getSettings().getConsoleSummonKey(), Level.DEBUG);

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
        Global.getSector().addTransientScript(new ConsoleCampaignListener());
    }
}
