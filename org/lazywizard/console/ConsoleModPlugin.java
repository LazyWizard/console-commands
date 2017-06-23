package org.lazywizard.console;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.lazywizard.console.commands.ReloadConsole;
import org.lwjgl.opengl.Display;

public class ConsoleModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        // Console settings
        ReloadConsole.reloadConsole(true);

        Console.showMessage("Console loaded, summon with "
                + Console.getSettings().getConsoleSummonKey(), Level.DEBUG);
    }

    @Override
    public void onGameLoad(boolean newGame)
    {
        Global.getSector().addTransientScript(new ConsoleCampaignListener());
    }
}
