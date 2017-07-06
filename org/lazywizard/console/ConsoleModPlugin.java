package org.lazywizard.console;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;
import org.lazywizard.console.commands.ReloadConsole;

public class ConsoleModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        // Console settings
        ReloadConsole.reloadConsole(false);

        Console.showMessage("Console loaded, summon with "
                + Console.getSettings().getConsoleSummonKey(), Level.DEBUG);
    }

    @Override
    public void onGameLoad(boolean newGame)
    {
        Global.getSector().addTransientScript(new ConsoleCampaignListener());
    }
}
