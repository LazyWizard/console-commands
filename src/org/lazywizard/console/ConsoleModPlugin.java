package org.lazywizard.console;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.lazywizard.console.listeners.ConsoleCampaignListener;

public class ConsoleModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        CommandStore.loadCommandsFromCSV("data/console/console_commands.csv");
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
