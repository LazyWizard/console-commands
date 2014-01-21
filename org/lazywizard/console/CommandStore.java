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
        ClassLoader loader = Global.getSettings().getScriptClassLoader();
        Class commandClass;
        String commandName, commandHelp, commandSource;
        for (int x = 0; x < commandData.length(); x++)
        {
            // Prevents previous command's info showing up in error message
            commandName = commandSource = null;
            commandClass = null;

            try
            {
                tmp = commandData.getJSONObject(x);
                commandName = tmp.getString("command").toLowerCase();
                commandClass = loader.loadClass(tmp.getString("class"));
                commandHelp = tmp.getString("helpFile");
                commandSource = tmp.getString("fs_rowSource");

                if (!BaseCommand.class.isAssignableFrom(commandClass))
                {
                    throw new Exception(commandClass.getSimpleName()
                            + " does not extend "
                            + BaseCommand.class.getSimpleName());
                }

                storedCommands.put(commandName,
                        new StoredCommand(commandName, commandClass,
                                commandHelp, commandSource));
                Global.getLogger(CommandStore.class).log(Level.DEBUG,
                        "Loaded command " + commandName + " (class: "
                        + commandClass.getCanonicalName() + ") from " + commandSource);
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
        private final String name, syntax, help, source;

        // TODO: Don't load helpfile, instead leave that up to the 'help' command
        StoredCommand(String commandName, Class<? extends BaseCommand> commandClass,
                String helpFile, String source)
        {
            this.name = commandName;
            this.commandClass = commandClass;
            this.source = source;

            if (helpFile == null || helpFile.isEmpty())
            {
                Global.getLogger(CommandStore.class).log(Level.WARN,
                        "No helpfile registered for command \"" + name + "\"");
                syntax = null;
                help = null;
                return;
            }

            String[] raw;
            try
            {
                raw = Global.getSettings().loadText(helpFile).split("\n", 2);
            }
            catch (IOException ex)
            {
                Global.getLogger(CommandStore.class).log(Level.ERROR,
                        "Helpfile not found for command \"" + name + "\"");
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

        public String getSyntax()
        {
            return syntax;
        }

        public String getHelp()
        {
            return help;
        }

        public String getSource()
        {
            return source;
        }
    }

    private CommandStore()
    {
    }
}
