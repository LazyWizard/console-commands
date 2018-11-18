package org.lazywizard.console;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.apache.log4j.Level;
import org.lazywizard.console.commands.DebugDialogs;
import org.lazywizard.console.commands.ReloadConsole;

public class ConsoleModPlugin extends BaseModPlugin
{
    private boolean debugDialogs = false;

    @Override
    public void onApplicationLoad() throws Exception
    {
        // Load console settings
        ReloadConsole.reloadConsole();

        Console.showMessage("Console loaded, summon with "
                + Console.getSettings().getConsoleSummonKey(), Level.DEBUG);
    }

    @Override
    public void onGameLoad(boolean newGame)
    {
        Global.getSector().getListenerManager().addListener(new ConsoleCampaignListener(), true);
    }

    @Override
    public void beforeGameSave()
    {
        final MemoryAPI memory = Global.getSector().getMemory();
        debugDialogs = memory.getBoolean(DebugDialogs.MEMORY_KEY);
        if (debugDialogs) memory.unset(DebugDialogs.MEMORY_KEY);
    }

    @Override
    public void afterGameSave()
    {
        if (debugDialogs) Global.getSector().getMemory().set(DebugDialogs.MEMORY_KEY, true);
    }
}
