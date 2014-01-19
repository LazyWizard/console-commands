package org.lazywizard.console;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.json.JSONObject;
import org.lazywizard.console.listeners.ConsoleCampaignListener;

public class Console extends BaseModPlugin
{
    private static int CONSOLE_KEY;

    static boolean checkInput(Context context)
    {
        return true;
    }

    static enum Context
    {
        COMBAT,
        CAMPAIGN
    }

    //<editor-fold defaultstate="collapsed" desc="ModPlugin stuff">
    @Override
    public void onApplicationLoad() throws Exception
    {
        JSONObject settings = Global.getSettings().loadJSON(
                "data/console/console_settings.json");
        CONSOLE_KEY = settings.getInt("consoleKey");

        // Load console commands
        CommandStore.reloadCommands();
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
