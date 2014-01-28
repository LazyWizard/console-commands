package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Level;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.CollectionUtils;

public class CommandStore
{
    private static final Map<String, StoredCommand> storedCommands = new HashMap<>();
    private static final Set<String> categories = new HashSet();

    // Will only throw these exceptions if there is an error loading the CSV
    public static void reloadCommands() throws IOException, JSONException
    {
        storedCommands.clear();
        JSONArray commandData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "command", "data/console/commands.csv", "lw_console");
        JSONObject tmp;
        ClassLoader loader = Global.getSettings().getScriptClassLoader();
        Class commandClass;
        String commandName, commandSyntax, commandHelp, commandSource;
        List<String> commandCategories;
        for (int x = 0; x < commandData.length(); x++)
        {
            // Prevents previous command's info showing up in error message
            commandName = null;
            commandSource = null;
            commandClass = null;

            try
            {
                tmp = commandData.getJSONObject(x);
                commandName = tmp.getString("command");
                commandClass = loader.loadClass(tmp.getString("class"));

                if (!BaseCommand.class.isAssignableFrom(commandClass))
                {
                    throw new Exception(commandClass.getCanonicalName()
                            + " does not extend "
                            + BaseCommand.class.getCanonicalName());
                }

                commandSyntax = tmp.getString("syntax");
                commandHelp = tmp.getString("help");
                commandSource = tmp.getString("fs_rowSource");

                String[] rawCategories = tmp.getString("categories").split(",");
                commandCategories = new ArrayList<>();
                for (String category : rawCategories)
                {
                    category = category.toLowerCase().trim();
                    if (category.isEmpty())
                    {
                        continue;
                    }
                    
                    commandCategories.add(category);

                    if (!categories.contains(category))
                    {
                        categories.add(category);
                    }
                }

                storedCommands.put(commandName.toLowerCase(),
                        new StoredCommand(commandName, commandClass,
                                commandSyntax, commandHelp,
                                commandCategories, commandSource));
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

        Global.getLogger(CommandStore.class).log(Level.INFO,
                "Loaded commands: " + CollectionUtils.implode(getLoadedCommands()));
    }

    public static List<String> getLoadedCommands()
    {
        List<String> commands = new ArrayList(storedCommands.size());
        for (StoredCommand tmp : storedCommands.values())
        {
            commands.add(tmp.getName());
        }

        return commands;
    }

    public static List getCategories()
    {
        return new ArrayList(categories);
    }

    public static List getCommandsInCategory(String category)
    {
        category = category.toLowerCase();

        List<String> commands = new ArrayList();
        for (StoredCommand tmp : storedCommands.values())
        {
            if (tmp.categories.contains(category))
            {
                commands.add(tmp.getName());
            }
        }

        return commands;
    }

    public static StoredCommand retrieveCommand(String command)
    {
        if (storedCommands.containsKey(command))
        {
            return storedCommands.get(command);
        }

        return null;
    }

    public static String getSyntax(String command)
    {
        if (storedCommands.containsKey(command))
        {
            return storedCommands.get(command).getSyntax();
        }

        return null;
    }

    public static String getHelp(String command)
    {
        if (storedCommands.containsKey(command))
        {
            return storedCommands.get(command).getHelp();
        }

        return null;
    }

    public static class StoredCommand
    {
        private final Class<? extends BaseCommand> commandClass;
        private final String name, syntax, help, source;
        private final List<String> categories;

        StoredCommand(String commandName, Class<? extends BaseCommand> commandClass,
                String syntax, String help, List<String> categories, String source)
        {
            this.name = commandName;
            this.commandClass = commandClass;
            this.syntax = (syntax == null ? "" : syntax);
            this.help = (help == null ? "" : help);
            this.categories = categories;
            this.source = source;
        }

        public Class<? extends BaseCommand> getCommandClass()
        {
            return commandClass;
        }

        public String getName()
        {
            return name;
        }

        public String getSyntax()
        {
            return syntax;
        }

        public String getHelp()
        {
            return help;
        }

        public List<String> getCategories()
        {
            return categories;
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
