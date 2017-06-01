package org.lazywizard.console;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.CollectionUtils;

/**
 * The console mod's internal command storage. You can retrieve detailed
 * information on the loaded commands using this class.
 * <p>
 * @author LazyWizard
 * @since 2.0
 */
public class CommandStore
{
    private static final Logger Log = Global.getLogger(CommandStore.class);
    private static final Map<String, StoredCommand> storedCommands = new HashMap<>();
    private static final Map<String, String> aliases = new HashMap<>();
    private static final Set<String> tags = new HashSet<>();

    /**
     * Forces the console to clear its stored commands and reload them from the
     * CSV. If a command fails to load it will <i>not</i> throw an
     * {@link Exception}. Instead it will display an error message to the
     * player and continue to load the rest of the commands normally.
     *
     * @throws IOException   if the CSV file at {@link CommonStrings#PATH_CSV}
     *                       does not exist or can't be opened.
     * @throws JSONException if the CSV is malformed or missing columns.
     * @since 2.0
     */
    // Will only throw these exceptions if there is an error loading the CSV
    @SuppressWarnings("unchecked")
    public static void reloadCommands() throws IOException, JSONException
    {
        storedCommands.clear();
        tags.clear();
        JSONArray commandData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "command", CommonStrings.PATH_CSV, CommonStrings.MOD_ID);
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
                commandName = row.getString("command");

                // Skip empty rows
                if (commandName.isEmpty())
                {
                    continue;
                }

                // Load these first so we can display them if there's an error
                commandPath = row.getString("class");
                commandSource = row.getString("fs_rowSource");

                // Check if the class is valid
                Class commandClass = loader.loadClass(commandPath);
                if (!BaseCommand.class.isAssignableFrom(commandClass))
                {
                    throw new Exception(commandClass.getCanonicalName()
                            + " does not extend " + BaseCommand.class.getCanonicalName());
                }

                // Class is valid, start building command info
                String commandSyntax = row.optString("syntax", "");
                String commandHelp = row.optString("help", "")
                        .replace("\\n", "\n"); // Newline support

                // Generate the tag list
                String[] rawTags = row.optString("tags", "").split(",");
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
                Log.debug("Loaded command " + commandName + " (class: "
                        + commandClass.getCanonicalName() + ") from " + commandSource);
            }
            catch (Exception ex)
            {
                Console.showException("Failed to load command " + commandName
                        + " (class: " + commandPath + ") from " + commandSource, ex);
            }
        }

        Log.info("Loaded commands: " + CollectionUtils.implode(getLoadedCommands()));

        // Populate alias mapping
        aliases.clear();
        JSONArray aliasData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "alias", CommonStrings.PATH_ALIAS, CommonStrings.MOD_ID);
        for (int x = 0; x < aliasData.length(); x++)
        {
            try
            {
                JSONObject row = aliasData.getJSONObject(x);
                String alias = row.getString("alias");
                String command = row.getString("command");

                aliases.put(alias.toLowerCase(), command);
            }
            catch (JSONException ex)
            {
                Console.showException("Failed to parse aliases", ex);
            }
        }

        Log.info("Loaded aliases: " + CollectionUtils.implode(getAliases().keySet()));
    }

    /**
     * Returns all commands currently loaded by the mod.
     * <p>
     * @return A {@link List} containing the names of all loaded commands.
     * <p>
     * @since 2.0
     */
    public static List<String> getLoadedCommands()
    {
        List<String> commands = new ArrayList<>(storedCommands.size());
        for (StoredCommand tmp : storedCommands.values())
        {
            commands.add(tmp.getName());
        }

        return commands;
    }

    /**
     * Returns all aliases currently registered by the mod.
     * <p>
     * @return A {@link Map} containing all registered aliases as keys, with the
     *         commands they expand to as values.
     * <p>
     * @since 2.4
     */
    public static Map<String, String> getAliases()
    {
        return new HashMap<>(aliases);
    }

    /**
     * Returns all command tags that the mod is currently aware of.
     * <p>
     * @return A {@link List} containing all tags used by the currently loaded
     *         commands.
     * <p>
     * @since 2.0
     */
    public static List<String> getKnownTags()
    {
        return new ArrayList<>(tags);
    }

    /**
     * Returns all commands with a specific tag.
     * <p>
     * @param tag The tag to search for.
     * <p>
     * @return A {@link List} containing the names of all loaded commands that
     *         use the tag {@code tag}.
     * <p>
     * @since 2.0
     */
    public static List<String> getCommandsWithTag(String tag)
    {
        tag = tag.toLowerCase();

        List<String> commands = new ArrayList<>();
        for (StoredCommand tmp : storedCommands.values())
        {
            if (tmp.getTags().contains(tag))
            {
                commands.add(tmp.getName());
            }
        }

        return commands;
    }

    /**
     * Retrieves the raw data for a specific command.
     * <p>
     * @param command The name of the command to retrieve.
     * <p>
     * @return The {@link StoredCommand} containing all of the data the console
     *         needs to use this command, such as its name, class, syntax, helpfile, and
     *         what mod registered it.
     * <p>
     * @since 2.0
     */
    public static StoredCommand retrieveCommand(String command)
    {
        command = command.toLowerCase();

        if (storedCommands.containsKey(command))
        {
            return storedCommands.get(command);
        }

        return null;
    }

    /**
     * Contains detailed information on a loaded command.
     * <p>
     * @since 2.0
     */
    public static final class StoredCommand
    {
        private final Class<? extends BaseCommand> commandClass;
        private final String name, syntax, help, source;
        private final List<String> tags;

        private static String filterModPath(String fullPath)
        {
            try
            {
                String modPath = fullPath.replace("/", "\\");
                modPath = modPath.substring(modPath.lastIndexOf("\\mods\\"));
                modPath = modPath.substring(0, modPath.indexOf('\\', 6)) + "\\";
                return modPath;
            }
            catch (Exception ex)
            {
                Console.showException("Failed to reduce modpath '" + fullPath + "'", ex);
                return fullPath;
            }
        }

        private StoredCommand(String commandName, Class<? extends BaseCommand> commandClass,
                String syntax, String help, List<String> tags, String source)
        {
            this.name = commandName;
            this.commandClass = commandClass;
            this.syntax = (syntax == null ? "" : syntax);
            this.help = (help == null ? "" : help);
            this.tags = tags;
            this.source = filterModPath(source);
        }

        /**
         * Returns the class object for this command's implementation.
         * <p>
         * @return The {@link Class} of the {@link BaseCommand} implementation
         *         that will be instantiated when this command is run.
         * <p>
         * @since 2.0
         */
        public Class<? extends BaseCommand> getCommandClass()
        {
            return commandClass;
        }

        /**
         * Returns the name of this command (what the player would enter to use
         * it).
         * <p>
         * @return The name of this command, taken from the 'name' column of the
         *         CSV.
         * <p>
         * @since 2.0
         */
        public String getName()
        {
            return name;
        }

        /**
         * Returns the syntax for this command.
         * <p>
         * @return The syntax for this command, taken from the 'syntax' column
         *         of the CSV.
         * <p>
         * @since 2.0
         */
        public String getSyntax()
        {
            return syntax;
        }

        /**
         * Returns the detailed usage instructions for this command.
         * <p>
         * @return The detailed help for this command, taken from the 'help'
         *         column of the CSV.
         * <p>
         * @since 2.0
         */
        public String getHelp()
        {
            return help;
        }

        /**
         * Returns all tags associated with this command.
         * <p>
         * @return All tags associated with this command, taken from the 'tags'
         *         column of the CSV.
         * <p>
         * @since 2.0
         */
        public List<String> getTags()
        {
            return Collections.unmodifiableList(tags);
        }

        /**
         * Returns the complete file path of the CSV this command was loaded
         * from (<b>not</b> the relative path). Useful for determining which mod
         * added this command.
         * <p>
         * @return The complete file path of the CSV this command was loaded
         *         from. There is no other way to retrieve this information in
         *         the current API.
         * <p>
         * @since 2.0
         */
        public String getSource()
        {
            return source;
        }
    }

    private CommandStore()
    {
    }
}
