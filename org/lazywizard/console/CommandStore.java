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
    private static final Map<String, StoredCommand> storedCommands = new HashMap<>();

    // Will only throw these exceptions if there is an error loading the CSV
    public static void reloadCommands() throws IOException, JSONException
    {
        // TODO: This could use some cleanup
        storedCommands.clear();
        JSONArray commandData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "command", "data/console/console_commands.csv", "lw_console");
        JSONObject tmp;
        Class clazz;
        String commandName, commandClass, source;
        boolean isUsableInCombat, isUsableInCampaign;
        for (int x = 0; x < commandData.length(); x++)
        {
            // Prevents previous command's info showing up in error message
            commandName = commandClass = source = null;

            try
            {
                tmp = commandData.getJSONObject(x);
                commandName = tmp.getString("command").toLowerCase();
                commandClass = tmp.getString("class");
                source = tmp.getString("fs_rowSource");
                isUsableInCombat = tmp.getBoolean("usable in combat");
                isUsableInCampaign = tmp.getBoolean("usable in campaign");

                clazz = Global.getSettings().getScriptClassLoader().loadClass(commandClass);

                if (!BaseCommand.class.isAssignableFrom(clazz))
                {
                    throw new Exception(clazz.getSimpleName()
                            + " does not extend "
                            + BaseCommand.class.getSimpleName());
                }

                storedCommands.put(commandName,
                        new StoredCommand(clazz, isUsableInCampaign, isUsableInCombat));
                Global.getLogger(CommandStore.class).log(Level.DEBUG,
                        "Loaded command " + commandName + " (class: "
                        + commandClass + ") from " + source);
            }
            catch (Exception ex)
            {
                Global.getLogger(CommandStore.class).log(Level.ERROR,
                        "Failed to load command " + commandName + " (class: "
                        + commandClass + ") from " + source, ex);
            }
        }
    }

    public static boolean isUsableInCombat(String command)
    {
        if (storedCommands.containsKey(command))
        {
            return storedCommands.get(command).isUsableInCombat;
        }

        return false;
    }

    public static boolean isUsableInCampaign(String command)
    {
        if (storedCommands.containsKey(command))
        {
            return storedCommands.get(command).isUsableInCampaign;
        }

        return false;
    }

    public static BaseCommand retrieveCommand(String command)
            throws InstantiationException, IllegalAccessException
    {
        if (storedCommands.containsKey(command))
        {
            return storedCommands.get(command).commandClass.newInstance();
        }

        return null;
    }

    private static class StoredCommand
    {
        private final Class<? extends BaseCommand> commandClass;
        private final boolean isUsableInCombat, isUsableInCampaign;

        StoredCommand(Class<? extends BaseCommand> commandClass,
                boolean isUsableInCampaign, boolean isUsableInCombat)
        {
            this.commandClass = commandClass;
            this.isUsableInCombat = isUsableInCombat;
            this.isUsableInCampaign = isUsableInCampaign;
        }
    }

    private CommandStore()
    {
    }
}
