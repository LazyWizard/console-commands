package org.lazywizard.sfconsole;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.json.JSONArray;

public class ConsolePlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        JSONArray commandData = Global.getSettings()
                .getMergedSpreadhsheetDataForMod("command",
                "data/console/console_commands.csv", "lw_devconsole");
        Map<String, Class> allCommands = new HashMap<String, Class>();
        String commandName = null, commandClass = null, source = null;
        for (int x = 0; x < commandData.length(); x++)
        {
            try
            {
                commandName = commandData.getJSONObject(x).getString("command");
                commandClass = commandData.getJSONObject(x).getString("class");
                source = commandData.getJSONObject(x).getString("fs_rowSource");
                allCommands.put(commandName, Global.getSettings()
                        .getScriptClassLoader().loadClass(commandClass));
                Global.getLogger(ConsolePlugin.class).log(Level.INFO,
                        "Loaded command " + commandName + " (class: "
                        + commandClass + ") from " + source);
            }
            catch (Exception ex)
            {
                Global.getLogger(ConsolePlugin.class).log(Level.ERROR,
                        "Failed to load console command " + commandClass, ex);
            }
        }

        System.out.println(allCommands);

        for (Class tmp : allCommands.values())
        {
            Console.registerCommand(tmp);
        }
        // TODO: Load all commands dynamically from a JSON file
    }

    @Override
    public void onNewGame()
    {
        Global.getSector().addScript(new Console());
        Console.showMessage("Console successfully activated for this save.");
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore)
    {
        // TODO: generate if not enabled (only after .6.1a is released)
    }
}
