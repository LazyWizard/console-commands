package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Level;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.CollectionUtils;

// TODO: Javadoc public methods
public class CommandStore
{
    private static final Map<String, StoredCommand> storedCommands = new HashMap<>();
    //private static final Map<String, String> aliases = new HashMap();
    private static final Set<String> tags = new HashSet<>();

    // Will only throw these exceptions if there is an error loading the CSV
    public static void reloadCommands() throws IOException, JSONException
    {
        storedCommands.clear();
        JSONArray commandData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "command", CommonStrings.CSV_PATH, CommonStrings.MOD_ID);
        ClassLoader loader = Global.getSettings().getScriptClassLoader();
        for (int x = 0; x < commandData.length(); x++)
        {
            // Defined here so we can use them in the catch block
            String commandName = null;
            String commandSource = null;
            String commandPath = null;

            try
            {
                JSONObject row = commandData.getJSONObject(x);

                // Load these first so we can display them if there's an error
                commandName = row.getString("command");
                commandPath = row.getString("class");
                commandSource = row.getString("fs_rowSource");

                // Check if the class is valid
                Class commandClass = loader.loadClass(commandPath);
                if (!BaseCommand.class.isAssignableFrom(commandClass))
                {
                    throw new Exception(commandClass.getCanonicalName()
                            + " does not extend "
                            + BaseCommand.class.getCanonicalName());
                }

                // Class is valid, start building command info
                String commandSyntax = row.getString("syntax");
                String commandHelp = row.getString("help");

                // Generate the tag list
                String[] rawTags = row.getString("tags").split(",");
                List<String> commandTags = new ArrayList<>();
                for (String tag : rawTags)
                {
                    tag = tag.toLowerCase().trim();
                    if (tag.isEmpty())
                    {
                        continue;
                    }

                    commandTags.add(tag);

                    // Add to global list of tags
                    if (!tags.contains(tag))
                    {
                        tags.add(tag);
                    }
                }

                // Built command info, register it in the master command list
                storedCommands.put(commandName.toLowerCase(),
                        new StoredCommand(commandName, commandClass,
                                commandSyntax, commandHelp,
                                commandTags, commandSource));
                Global.getLogger(CommandStore.class).log(Level.DEBUG,
                        "Loaded command " + commandName + " (class: "
                        + commandClass.getCanonicalName() + ") from " + commandSource);
            }
            catch (Exception ex)
            {
                Console.showException("Failed to load command " + commandName
                        + " (class: " + commandPath + ") from " + commandSource, ex);
            }
        }

        Global.getLogger(CommandStore.class).log(Level.INFO,
                "Loaded commands: " + CollectionUtils.implode(getLoadedCommands()));
    }

    public static List<String> getLoadedCommands()
    {
        List<String> commands = new ArrayList<>(storedCommands.size());
        for (StoredCommand tmp : storedCommands.values())
        {
            commands.add(tmp.getName());
        }

        return commands;
    }

    public static List<String> getKnownTags()
    {
        return new ArrayList<>(tags);
    }

    public static List<String> getCommandsWithTag(String tag)
    {
        tag = tag.toLowerCase();

        List<String> commands = new ArrayList<>();
        for (StoredCommand tmp : storedCommands.values())
        {
            if (tmp.tags.contains(tag))
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

    // TODO: Javadoc public methods
    public static class StoredCommand
    {
        private final Class<? extends BaseCommand> commandClass;
        private final String name, syntax, help, source;
        private final List<String> tags;

        StoredCommand(String commandName, Class<? extends BaseCommand> commandClass,
                String syntax, String help, List<String> tags, String source)
        {
            this.name = commandName;
            this.commandClass = commandClass;
            this.syntax = (syntax == null ? "" : syntax);
            this.help = (help == null ? "" : help);
            this.tags = tags;
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

        public List<String> getTags()
        {
            return Collections.unmodifiableList(tags);
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
