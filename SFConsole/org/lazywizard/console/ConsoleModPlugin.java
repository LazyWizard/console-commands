package org.lazywizard.console;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

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
        Global.getSector().addScript(new ConsoleCampaignScript());
    }

    @Override
    public void beforeGameSave()
    {
        Global.getSector().removeScriptsOfClass(ConsoleCampaignScript.class);
    }

    @Override
    public void afterGameSave()
    {
        Global.getSector().addScript(new ConsoleCampaignScript());
    }
}
