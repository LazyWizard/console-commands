package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CommandStore
{
    private static final Map<String, BaseCommand> allCommands = new HashMap<>();

    public static void reloadCommands() throws IOException, JSONException
    {
        allCommands.clear();
        JSONArray commandData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "command", "data/console/console_commands.csv", "lw_console");
        JSONObject tmp;
        Class cls;
        String commandName = null, commandClass = null, source = null;
        boolean isUsableInCombat, isUsableInCampaign;
        for (int x = 0; x < commandData.length(); x++)
        {
            try
            {
                tmp = commandData.getJSONObject(x);
                commandName = tmp.getString("command");
                commandClass = tmp.getString("class");
                source = tmp.getString("fs_rowSource");
                isUsableInCombat = tmp.getBoolean("usable in combat");
                isUsableInCampaign = tmp.getBoolean("usable in combat");

                cls = Global.getSettings().getScriptClassLoader().loadClass(commandClass);

                if (!BaseCommand.class.isAssignableFrom(cls))
                {
                    throw new Exception(cls.getSimpleName() + " does not extend BaseCommand");
                }

                allCommands.put(commandName, (BaseCommand) cls.newInstance());
                Global.getLogger(CommandStore.class).log(Level.INFO,
                        "Loaded command " + commandName + " (class: "
                        + commandClass + ") from " + source);
            }
            catch (Exception ex)
            {
                Global.getLogger(CommandStore.class).log(Level.ERROR,
                        "Failed to load command " + commandClass, ex);
            }
        }
    }

    public static BaseCommand retrieveCommand(String command)
    {
        return allCommands.get(command);
    }

    private CommandStore()
    {
    }
}
