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
                "command", "data/console/commands.csv", "lw_console");
        JSONObject tmp;
        Class clazz;
        String commandName, commandClass, commandHelp, commandSource;
        for (int x = 0; x < commandData.length(); x++)
        {
            // Prevents previous command's info showing up in error message
            commandName = commandClass = commandSource = null;

            try
            {
                tmp = commandData.getJSONObject(x);
                commandName = tmp.getString("command").toLowerCase();
                commandClass = tmp.getString("class");
                commandSource = tmp.getString("fs_rowSource");
                commandHelp = tmp.getString("helpfile");

                clazz = Global.getSettings().getScriptClassLoader().loadClass(commandClass);

                if (!BaseCommand.class.isAssignableFrom(clazz))
                {
                    throw new Exception(clazz.getSimpleName()
                            + " does not extend "
                            + BaseCommand.class.getSimpleName());
                }

                storedCommands.put(commandName,
                        new StoredCommand(commandName, clazz, commandHelp));
                Global.getLogger(CommandStore.class).log(Level.DEBUG,
                        "Loaded command " + commandName + " (class: "
                        + commandClass + ") from " + commandSource);
            }
            catch (Exception ex)
            {
                Global.getLogger(CommandStore.class).log(Level.ERROR,
                        "Failed to load command " + commandName + " (class: "
                        + commandClass + ") from " + commandSource, ex);
            }
        }
    }

    public static StoredCommand retrieveCommand(String command)
            throws InstantiationException, IllegalAccessException
    {
        if (storedCommands.containsKey(command))
        {
            return storedCommands.get(command);
        }

        return null;
    }

    public static class StoredCommand
    {
        private final Class<? extends BaseCommand> commandClass;
        private final String name, syntax, help;

        StoredCommand(String commandName, Class<? extends BaseCommand> commandClass,
                String helpFile)
        {
            this.name = commandName;
            this.commandClass = commandClass;

            if (helpFile == null || helpFile.isEmpty())
            {
                Global.getLogger(CommandStore.class).log(Level.WARN,
                        "No helpfile found for command \"" + name + "\"");
                syntax = null;
                help = null;
                return;
            }

            String[] raw;
            try
            {
                raw = Global.getSettings().loadText(helpFile).split("\n", 1);
            }
            catch (IOException ex)
            {
                Global.getLogger(CommandStore.class).log(Level.WARN,
                        "No helpfile found for command \"" + name + "\"", ex);
                syntax = null;
                help = null;
                return;
            }

            syntax = raw[0];
            help = (raw.length > 1 ? raw[1] : null);
        }

        public Class<? extends BaseCommand> getCommandClass()
        {
            return commandClass;
        }
    }

    private CommandStore()
    {
    }
}
