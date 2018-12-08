package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.JSONUtils;
import org.lazywizard.lazylib.JSONUtils.CommonDataJSONObject;

import java.io.IOException;
import java.util.*;

/**
 * The console mod's internal command storage. You can retrieve detailed
 * information on the loaded commands using this class.
 *
 * @author LazyWizard
 * @since 2.0
 */
public class CommandStore
{
    private static final Logger Log = Global.getLogger(CommandStore.class);
    private static final Map<String, StoredCommand> storedCommands = new HashMap<>();
    private static final Set<ListenerData> listeners = new TreeSet<>();
    private static final Map<String, String> aliases = new HashMap<>();
    private static final Set<String> tags = new HashSet<>();
    private static CommonDataJSONObject aliasData = null;

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
        final JSONArray commandData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "command", CommonStrings.PATH_CSV, CommonStrings.MOD_ID);
        final ClassLoader loader = Global.getSettings().getScriptClassLoader();
        for (int i = 0; i < commandData.length(); i++)
        {
            // Defined here so we can use them in the catch block
            String commandName = null;
            String commandSource = null;
            String commandPath = null;

            try
            {
                final JSONObject row = commandData.getJSONObject(i);
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
                final Class commandClass = loader.loadClass(commandPath);
                if (!BaseCommand.class.isAssignableFrom(commandClass))
                {
                    throw new Exception(commandClass.getCanonicalName()
                            + " does not extend " + BaseCommand.class.getCanonicalName());
                }

                // Class is valid, start building command info
                final String commandSyntax = row.optString("syntax", "");
                final String commandHelp = row.optString("help", "")
                        .replace("\\n", "\n"); // Newline support

                // Generate the tag list
                final String[] rawTags = row.optString("tags", "").split(",");
                final List<String> commandTags = new ArrayList<>();
                for (String tag : rawTags)
                {
                    tag = tag.toLowerCase().trim();
                    if (tag.isEmpty())
                    {
                        continue;
                    }

                    commandTags.add(tag);

                    // Add to global list of tags
                    tags.add(tag);
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
        try
        {
            aliasData = JSONUtils.loadCommonJSON("config/lw_console_aliases.json", "data/console/aliases.default");
            for (Iterator iter = aliasData.keys(); iter.hasNext(); )
            {
                final String alias = (String) iter.next();
                final String toRun = aliasData.optString(alias, null);
                if (toRun != null)
                {
                    aliases.put(alias.toLowerCase(), toRun);
                }
            }
        }
        catch (JSONException ex)
        {
            Console.showException("Failed to parse aliases", ex);
        }

        Log.info("Loaded aliases: " + CollectionUtils.implode(getAliases().keySet()));

        // Populate listeners
        listeners.clear();
        final JSONArray listenerData = Global.getSettings().getMergedSpreadsheetDataForMod(
                "listenerId", CommonStrings.PATH_LISTENER_CSV, CommonStrings.MOD_ID);
        for (int i = 0; i < listenerData.length(); i++)
        {
            // Defined here so we can use them in the catch block
            String listenerId = null;
            String listenerPath = null;
            String listenerSource = null;
            int listenerPriority;

            try
            {
                final JSONObject row = listenerData.getJSONObject(i);
                listenerId = row.getString("listenerId");

                // Skip empty rows
                if (listenerId.isEmpty())
                {
                    continue;
                }

                // Load these first so we can display them if there's an error
                listenerPath = row.getString("listenerClass");
                listenerPriority = row.optInt("priority", 0);
                listenerSource = row.getString("fs_rowSource");

                // Check if the class is valid
                final Class listenerClass = loader.loadClass(listenerPath);
                if (!CommandListener.class.isAssignableFrom(listenerClass))
                {
                    throw new Exception(listenerClass.getCanonicalName()
                            + " does not extend " + CommandListener.class.getCanonicalName());
                }

                // Register listener
                listeners.add(new ListenerData((CommandListener) listenerClass.newInstance(), listenerPriority));
                Log.debug("Loaded listener " + listenerId + " (class: "
                        + listenerClass.getCanonicalName() + ") from " + listenerSource);
            }
            catch (Exception ex)
            {
                Console.showException("Failed to load listener " + listenerId
                        + " (class: " + listenerPath + ") from " + listenerSource, ex);
            }
        }
    }

    /**
     * Returns all commands currently loaded by the mod.
     *
     * @return A {@link List} containing the names of all loaded commands.
     *
     * @since 2.0
     */
    public static List<String> getLoadedCommands()
    {
        final List<String> commands = new ArrayList<>(storedCommands.size());
        for (StoredCommand tmp : storedCommands.values())
        {
            commands.add(tmp.getName());
        }

        return commands;
    }

    private static boolean isApplicable(StoredCommand command, CommandContext context)
    {
        final List<String> tags = command.tags;

        if (tags.contains("console"))
        {
            return true;
        }
        else if (context.isInMarket())
        {
            return tags.contains("market") || (!tags.contains("combat") && !tags.contains("campaign"));
        }
        else if (context.isInCampaign())
        {
            return tags.contains("campaign") || (!tags.contains("combat") && !tags.contains("market"));
        }
        else if (context.isInCombat())
        {
            return tags.contains("combat") || (!tags.contains("campaign") && !tags.contains("market"));
        }

        return true;
    }

    /**
     * Returns all commands applicable in the given {@link CommandContext}. A command will only be excluded if it
     * contains an opposing tag and does not contain the matching one (ex: for {@link CommandContext#CAMPAIGN_MAP},
     * commands will only be excluded if they contain the tag "combat" or "market" but <i>not</i> the tag "campaign".
     * <p>
     * Commands with the tag "console" are assumed to be system-level, and will run anywhere.
     *
     * @return A {@link List} containing the names of all loaded commands
     *         that are applicable to the given context.
     *
     * @since 3.0
     */
    public static List<String> getApplicableCommands(CommandContext context)
    {
        final List<String> commands = new ArrayList<>();
        for (StoredCommand command : storedCommands.values())
        {
            if (isApplicable(command, context))
            {
                commands.add(command.getName());
            }
        }

        return commands;
    }

    /**
     * Returns all aliases currently registered by the mod.
     *
     * @return A {@link Map} containing all registered aliases as keys, with the
     *         commands they expand to as values.
     *
     * @since 2.4
     */
    public static Map<String, String> getAliases()
    {
        return new HashMap<>(aliases);
    }

    /**
     * Registers an alias to be used as shorthand for long or multiple commands..
     *
     * @param alias   The alias to register.
     * @param command The command that will be run in place of {@code alias}. This can include separators to run
     *                multiple commands. If {@code null} is passed in, removes any existing alias instead.
     *
     * @throws IOException
     * @throws JSONException
     * @since 3.0
     */
    public static void registerAlias(String alias, String command) throws IOException, JSONException
    {
        alias = alias.toLowerCase();
        if (command == null)
        {
            if (!aliases.containsKey(alias))
            {
                return;
            }

            aliasData.remove(alias);
            aliasData.save();
            aliases.remove(alias);
        }
        else
        {
            command = command.replaceAll(Console.getSettings().getCommandSeparator(), ";");
            aliasData.put(alias, command);
            aliasData.save();
            aliases.put(alias, command);
        }
    }

    /**
     * Returns all command tags that the mod is currently aware of.
     *
     * @return A {@link List} containing all tags used by the currently loaded
     *         commands.
     *
     * @since 2.0
     */
    public static List<String> getKnownTags()
    {
        return new ArrayList<>(tags);
    }

    /**
     * Returns all commands with a specific tag.
     *
     * @param tag The tag to search for.
     *
     * @return A {@link List} containing the names of all loaded commands that
     *         use the tag {@code tag}.
     *
     * @since 2.0
     */
    public static List<String> getCommandsWithTag(String tag)
    {
        tag = tag.toLowerCase();

        final List<String> commands = new ArrayList<>();
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
     * Returns all {@link CommandListener}s that are registered with the console.
     *
     * @return All registered {@link CommandListener}s.
     *
     * @since 3.0
     */
    public static List<CommandListener> getListeners()
    {
        final List<CommandListener> commandListeners = new ArrayList<>(listeners.size());
        for (ListenerData tmp : listeners)
        {
            commandListeners.add(tmp.listener);
        }

        return commandListeners;
    }

    /**
     * Retrieves the raw data for a specific command.
     *
     * @param command The name of the command to retrieve.
     *
     * @return The {@link StoredCommand} containing all of the data the console
     *         needs to use this command, such as its name, class, syntax, helpfile, and
     *         what mod registered it.
     *
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
     *
     * @since 2.0
     */
    public static final class StoredCommand
    {
        private final Class<? extends BaseCommand> commandClass;
        private final String name, syntax, help, source;
        private final List<String> tags;

        // TODO: This can be done through the API now
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
         *
         * @return The {@link Class} of the {@link BaseCommand} implementation
         *         that will be instantiated when this command is run.
         *
         * @since 2.0
         */
        public Class<? extends BaseCommand> getCommandClass()
        {
            return commandClass;
        }

        /**
         * Returns the name of this command (what the player would enter to use
         * it).
         *
         * @return The name of this command, taken from the 'name' column of the
         *         CSV.
         *
         * @since 2.0
         */
        public String getName()
        {
            return name;
        }

        /**
         * Returns the syntax for this command.
         *
         * @return The syntax for this command, taken from the 'syntax' column
         *         of the CSV.
         *
         * @since 2.0
         */
        public String getSyntax()
        {
            return syntax;
        }

        /**
         * Returns the detailed usage instructions for this command.
         *
         * @return The detailed help for this command, taken from the 'help'
         *         column of the CSV.
         *
         * @since 2.0
         */
        public String getHelp()
        {
            return help;
        }

        /**
         * Returns all tags associated with this command.
         *
         * @return All tags associated with this command, taken from the 'tags'
         *         column of the CSV.
         *
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
         *
         * @return The complete file path of the CSV this command was loaded
         *         from. There is no other way to retrieve this information in
         *         the current API.
         *
         * @since 2.0
         */
        public String getSource()
        {
            return source;
        }
    }

    private static class ListenerData implements Comparable<ListenerData>
    {
        private final CommandListener listener;
        private final int priority;

        private ListenerData(CommandListener listener, int priority)
        {
            this.listener = listener;
            this.priority = priority;
        }

        @Override
        public int compareTo(@NotNull ListenerData other)
        {
            // Highest priority wins
            return Integer.compare(other.priority, priority);
        }

    }

    private CommandStore()
    {
    }
}
