package org.lazywizard.sfconsole;

import com.fs.starfarer.api.Global;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Level;
import org.json.JSONArray;
import org.json.JSONException;

public class CommandStore
{
    private static final Map<String, Class<? extends BaseCommand>> allCommands = new HashMap<>();

    public static void loadCommandsFromCSV(String path) throws IOException, JSONException
    {
        allCommands.clear();
        JSONArray commandData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "command", path, "lw_devconsole");
        Class cls;
        String commandName = null, commandClass = null, source = null;
        for (int x = 0; x < commandData.length(); x++)
        {
            try
            {
                commandName = commandData.getJSONObject(x).getString("command");
                commandClass = commandData.getJSONObject(x).getString("class");
                source = commandData.getJSONObject(x).getString("fs_rowSource");
                cls = Global.getSettings().getScriptClassLoader().loadClass(commandClass);

                if (!BaseCommand.class.isAssignableFrom(cls))
                {
                    throw new Exception(cls.getSimpleName() + " does not extend BaseCommand");
                }

                allCommands.put(commandName, (Class<? extends BaseCommand>) Global.getSettings()
                        .getScriptClassLoader().loadClass(commandClass));
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
        if (!allCommands.containsKey(command))
        {
            return null;
        }

        try
        {
            return allCommands.get(command).newInstance();
        }
        catch (Exception ex)
        {
            Global.getLogger(CommandStore.class).log(Level.ERROR,
                    "Failed to instantiate command " + command, ex);
            return null;
        }
    }

    private CommandStore()
    {
    }
}
