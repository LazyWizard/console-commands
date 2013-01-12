package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpawnPointPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import data.scripts.console.commands.*;
import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/**
 * Executes commands and handles console output. Instances hold custom settings.
 */
public final class Console implements SpawnPointPlugin
{
    // Console constants
    /** Does the console require devMode=true in settings.json to function? */
    public static final boolean REQUIRE_DEV_MODE = false;
    /** Does the console require the game to be run windowed to function? */
    public static final boolean REQUIRE_RUN_WINDOWED = true;
    /** The package all console commands must be in */
    public static final String COMMAND_PACKAGE = "data.scripts.console.commands";
    /** The color of messages posted by {@link Console#showMessage(java.lang.String)} */
    public static final Color CONSOLE_COLOR = Color.YELLOW;
    /** How long a line can be before being split by {@link Console#showMessage(java.lang.String)} */
    public static final int LINE_LENGTH = 80;
    /** How often (in milliseconds) between polling the keyboard for input */
    public static final long INPUT_FRAMERATE = (long) (1000 / 20);
    /** This is the LWJGL constant of the default keyboard key to summon the console */
    public static final int DEFAULT_CONSOLE_KEY = Keyboard.KEY_GRAVE;
    /** This is the LWJGL constant of the keyboard key which, combined with shift, rebinds the console */
    public static final int REBIND_KEY = Keyboard.KEY_F1; // Shift+key to rebind
    /** A list of LWJGL keyboard constants that can't be bound to summon the console */
    public static final List<Integer> RESTRICTED_KEYS = new ArrayList<Integer>();
    // Maps the command to the associated class
    private static final Map<String, Class<? extends BaseCommand>> allCommands = new HashMap<String, Class<? extends BaseCommand>>();
    private static final Set<String> hardcodedCommands = new HashSet();
    // Per-session variables
    private static boolean inBattle = false;
    private static WeakReference<CombatEngineAPI> activeEngine;
    private static Console activeConsole;
    private static InputHandler inputHandler;
    private transient List<String> queuedCommands = Collections.synchronizedList(new ArrayList<String>());
    private transient boolean justReloaded = false, isListening = false;
    private transient volatile boolean isPressed = false, showRestrictions = true;
    // Saved variables
    private LocationAPI location;
    private int consoleKey = DEFAULT_CONSOLE_KEY;
    private Map<String, Object> consoleVars = new HashMap<String, Object>();
    private Map<String, String> aliases = new HashMap<String, String>();
    private Set<Class> extendedCommands = new HashSet<Class>();

    // Everything in this block absolutely MUST compile or the console will crash
    static
    {
        // Change the look and feel of the console pop-up
        UIManager.put("Panel.background", Color.BLACK);
        UIManager.put("OptionPane.background", Color.BLACK);
        UIManager.put("OptionPane.messageForeground", Color.CYAN);
        UIManager.put("TextField.background", Color.BLACK);
        UIManager.put("TextField.foreground", Color.YELLOW);
        UIManager.put("Button.background", Color.BLACK);
        UIManager.put("Button.foreground", Color.LIGHT_GRAY);

        // These keys can't be bound to summon the console
        RESTRICTED_KEYS.add(REBIND_KEY);
        RESTRICTED_KEYS.add(Keyboard.KEY_ESCAPE);
        RESTRICTED_KEYS.add(Keyboard.KEY_LMETA);
        RESTRICTED_KEYS.add(Keyboard.KEY_RMETA);
        RESTRICTED_KEYS.add(Keyboard.KEY_LSHIFT);
        RESTRICTED_KEYS.add(Keyboard.KEY_RSHIFT);

        // Built-in commands, don't need to go through registerCommand's checks
        allCommands.put("addaptitudepoints", AddAptitudePoints.class);
        allCommands.put("addcommandpoints", AddCommandPoints.class);
        allCommands.put("addcredits", AddCredits.class);
        allCommands.put("addcrew", AddCrew.class);
        allCommands.put("addfuel", AddFuel.class);
        allCommands.put("additem", AddItem.class);
        allCommands.put("addmarines", AddMarines.class);
        allCommands.put("addship", AddShip.class);
        allCommands.put("addskillpoints", AddSkillPoints.class);
        allCommands.put("addsupplies", AddSupplies.class);
        allCommands.put("addweapon", AddWeapon.class);
        allCommands.put("addwing", AddWing.class);
        allCommands.put("adjustrelationship", AdjustRelationship.class);
        allCommands.put("alias", Alias.class);
        allCommands.put("allweapons", AllWeapons.class);
        allCommands.put("gc", GC.class);
        allCommands.put("god", God.class);
        allCommands.put("goto", GoTo.class);
        allCommands.put("home", Home.class);
        allCommands.put("infiniteammo", InfiniteAmmo.class);
        allCommands.put("infiniteflux", InfiniteFlux.class);
        allCommands.put("nocooldown", NoCooldown.class);
        allCommands.put("reveal", Reveal.class);
        allCommands.put("runcode", RunCode.class);
        allCommands.put("runscript", RunScript.class);
        allCommands.put("sethome", SetHome.class);
        allCommands.put("setrelationship", SetRelationship.class);
        allCommands.put("spawnfleet", SpawnFleet.class);
        allCommands.put("unreveal", Unreveal.class);

        // Commands that can't be overwritten
        hardcodedCommands.add("help");
        hardcodedCommands.add("runtests");
        hardcodedCommands.add("status");
        hardcodedCommands.addAll(allCommands.keySet());
    }

    public Console()
    {
        setConsole(this);
        reloadInput();
    }

    /**
     * Automatically called by the game - don't call this manually.
     */
    public Object readResolve()
    {
        justReloaded = true;
        isPressed = false;
        isListening = false;
        queuedCommands = Collections.synchronizedList(new ArrayList<String>());
        setConsole(this);
        return this;
    }

    /**
     * Registers a command with the {@link Console}.
     *
     * Commands must pass validation, otherwise registration will fail!<p>
     *
     * Validation consists of the following:<br>
     *  - Checking that there isn't a built-in command with the same name<br>
     *  - Checking that the command's class is in the correct package<br>
     *  - Checking that the command extends {@link BaseCommand}
     *
     * @param commandClass the class object of the command to register
     * @throws Exception if the command doesn't pass validation
     */
    public void registerCommand(Class commandClass) throws Exception
    //throws InvalidCommandObjectException, InvalidCommandPackageException
    {
        String command = commandClass.getSimpleName().toLowerCase();

        if (hardcodedCommands.contains(command))
        {
            // InvalidCommandNameException
            throw new Exception("Can't overwrite built-in commands!");
        }

        // getPackage() won't work for classes loaded through Starfarer's
        // classloader. There's an extremely ugly workaround below
        //if (!COMMAND_PACKAGE.equals(commandClass.getPackage().getName()))
        if (!COMMAND_PACKAGE.equals(commandClass.getCanonicalName().substring(0,
                commandClass.getCanonicalName().lastIndexOf('.'))))
        {
            // InvalidCommandPackageException
            throw new Exception("Console command "
                    + commandClass.getCanonicalName() + " is not in the '"
                    + COMMAND_PACKAGE + "' package!");
        }

        if (!BaseCommand.class.isAssignableFrom(commandClass))
        {
            // InvalidCommandObjectException
            throw new Exception("Console command "
                    + commandClass.getCanonicalName()
                    + " does not extend BaseCommand!");
        }

        if (allCommands.put(command, commandClass) != null)
        {
            showMessage("Replaced existing command '" + command + "'.");
        }

        extendedCommands.add(commandClass);
    }

    /**
     * Registers an alias to be used in place of a longer command.
     *
     * Fails if the alias would conflict with an existing command.
     *
     * @param alias the alias (shorthand) for the command
     * @param command the command to replace alias with
     * @return true if the alias was successfully added, false otherwise
     */
    public boolean addAlias(String alias, String command)
    {
        if (allCommands.containsKey(alias) || !allCommands.containsKey(command)
                || alias.contains(" "))
        {
            return false;
        }

        aliases.put(alias, command);
        return true;
    }

    /**
     * Returns all aliases associated with a command
     *
     * @param command the command to check for aliases of
     * @return a List of Strings of all aliases for this command
     */
    public List<String> getAliases(String command)
    {
        if (command == null)
        {
            return Collections.EMPTY_LIST;
        }

        List<String> ret = new ArrayList();
        for (Map.Entry<String, String> entry : aliases.entrySet())
        {
            if (entry.getValue().equals(command))
            {
                ret.add(entry.getKey());
            }
        }

        return ret;
    }

    static Console getConsole()
    {
        return activeConsole;
    }

    static void setConsole(Console console)
    {
        activeConsole = console;
    }

    private synchronized boolean checkInput()
    {
        if (!isPressed)
        {
            if (Keyboard.isKeyDown(consoleKey))
            {
                isPressed = true;
            }
        }
        else
        {
            if (!Keyboard.isKeyDown(consoleKey))
            {
                isPressed = false;

                if (allowConsole())
                {
                    return true;
                }

                showRestrictions = true;
            }
        }

        return false;
    }

    /**
     * Tells the {@link Console} whether to allow battle-only commands or not.
     *
     * @param isInBattle
     */
    protected static void setInBattle(boolean isInBattle)
    {
        inBattle = isInBattle;
    }

    /**
     * Check if the player is on the battle map.
     *
     * @return true if battle-only commands are allowed
     */
    protected static boolean isInBattle()
    {
        return inBattle;
    }

    /**
     * Sets the {@link CombatEngineAPI} used by in-battle commands.
     *
     * @see BaseCombatHook
     *
     * @param engine the active {@link CombatEngineAPI}
     */
    protected static void setCombatEngine(CombatEngineAPI engine)
    {
        activeEngine = new WeakReference<CombatEngineAPI>(engine);
    }

    /**
     * Returns the {@link CombatEngineAPI} used by in-battle commands.
     *
     * @return the active {@link CombatEngineAPI}
     */
    protected static CombatEngineAPI getCombatEngine()
    {
        if (activeEngine == null || activeEngine.get() == null)
        {
            return null;
        }

        return activeEngine.get();
    }

    /**
     * Creates/sets a persistent variable that can be accessed by all console commands.<p>
     *
     * Important: ensure you use unique names for your variables to avoid conflict!
     *
     * @param varName the unique key this variable can be retrieved with
     * @param varData the data this variable should hold
     */
    protected void setVar(String varName, Object varData)
    {
        consoleVars.put(varName, varData);
    }

    /**
     * Retrieves the value of the variable varName, if any.
     *
     * @param <T>
     * @param varName the name of the variable to retrieve
     * @param type the type to be returned (e.g. Script.class)
     * @return the data associated with that variable
     */
    protected <T> T getVar(String varName, Class<T> type)
    {
        if (!hasVar(varName))
        {
            return null;
        }

        try
        {
            return type.cast(consoleVars.get(varName));
        }
        catch (ClassCastException ex)
        {
            showError("Error: " + varName + " is not of type "
                    + type.getSimpleName() + "!", ex);
            return null;
        }
    }

    /**
     * Checks for the existence of a variable with the supplied name.
     *
     * @param varName the name of the variable to check
     * @return true if the variable has been set, false otherwise
     */
    protected boolean hasVar(String varName)
    {
        return consoleVars.keySet().contains(varName);
    }

    /**
     * Get the runtime type of the variable with the supplied name.
     *
     * @param varName the name of the variable to check
     * @return the Class object for the variable if it exists, null otherwise
     */
    protected Class getVarType(String varName)
    {
        if (!hasVar(varName))
        {
            return null;
        }

        return consoleVars.get(varName).getClass();
    }

    /**
     * Gets the current {@link LocationAPI} of the player fleet
     *
     * @return the last location this campaign's advance() occurred in
     */
    protected LocationAPI getLocation()
    {
        /*if (location == null)
         {
         location = Global.getSector().getPlayerFleet().getContainingLocation();
         }*/

        return location;
    }

    private void reload()
    {
        reloadCommands();
        reloadScripts();
        reloadInput();

        Global.getSector().addMessage("The console will only be visible"
                + " when the game is run in windowed mode.");
        Global.getSector().addMessage("To rebind the console to another key,"
                + " press shift+" + Keyboard.getKeyName(REBIND_KEY)
                + " while on the campaign map.");
    }

    private void reloadCommands()
    {
        if (!extendedCommands.isEmpty())
        {
            showMessage("Reloading custom commands...");

            boolean hadError = false;
            Iterator<Class> iter = extendedCommands.iterator();
            Class tmp;

            while (iter.hasNext())
            {
                tmp = iter.next();

                try
                {
                    registerCommand(tmp);
                }
                catch (Exception ex)
                {
                    hadError = true;
                    showError("Error: failed to re-register command '"
                            + tmp.getSimpleName() + "'!", ex);
                    iter.remove();
                }
            }

            if (hadError)
            {
                showMessage("There were some errors while registering the extended console commands.");
            }
            else
            {
                showMessage("Extended console commands registered successfully!");
            }
        }
    }

    private void reloadScripts()
    {
        if (hasVar("UserScripts"))
        {
            Map<String, Script> userScripts = (Map) getVar("UserScripts", Map.class);
            RunScript.addScripts(userScripts);
        }
    }

    private synchronized void reloadInput()
    {
        if (inputHandler != null)
        {
            inputHandler.setConsole(this);
            return;
        }

        InputHandler tmp = new InputHandler(this);
        tmp.setName("Console-Input");
        tmp.setDaemon(true);
        Console.inputHandler = tmp;
        tmp.start();
    }

    private synchronized void addCommandToQueue(String command)
    {
        if (command == null || command.isEmpty())
        {
            return;
        }

        if (command.startsWith("alias "))
        {
            queuedCommands.add(command);
        }
        else
        {
            queuedCommands.addAll(Arrays.asList(command.split(";")));
        }
    }

    private static boolean allowConsole()
    {
        return !(REQUIRE_RUN_WINDOWED && Display.isFullscreen())
                || (REQUIRE_DEV_MODE && !Global.getSettings().getBoolean("devMode"));
    }

    private static void showRestrictions()
    {
        if (REQUIRE_RUN_WINDOWED || REQUIRE_DEV_MODE)
        {
            Console.showMessage("The console will only function"
                    + (REQUIRE_RUN_WINDOWED ? " in windowed mode" : "")
                    + (REQUIRE_DEV_MODE ? " with devmode active" : "") + ".");
        }
    }

    private static void showRestrictedKeys()
    {
        StringBuilder keys = new StringBuilder();

        for (int x = 0; x < RESTRICTED_KEYS.size(); x++)
        {
            keys.append(Keyboard.getKeyName(RESTRICTED_KEYS.get(x).intValue()));
            if (x < RESTRICTED_KEYS.size() - 1)
            {
                keys.append(", ");
            }
        }

        Global.getSector().addMessage("Restricted keys: " + keys.toString());
    }

    /**
     * Runs any commands that are queued for execution.
     */
    protected void checkQueue()
    {
        if (queuedCommands.isEmpty())
        {
            return;
        }

        for (String command : queuedCommands)
        {
            parseCommand(command);
        }

        queuedCommands.clear();
    }

    private static void checkBattle()
    {
        inBattle = false;
        activeEngine = null;
    }

    private void checkRebind()
    {
        if (isListening)
        {
            int key = Keyboard.getEventKey();

            if (key == Keyboard.KEY_ESCAPE)
            {
                isListening = false;
                Console.showMessage("Cancelled.");
                return;
            }

            if (key != Keyboard.KEY_NONE && key != REBIND_KEY)
            {
                if (!RESTRICTED_KEYS.contains(key))
                {
                    isListening = false;
                    consoleKey = key;
                    Console.showMessage("The console is now bound to '"
                            + Keyboard.getEventCharacter() + "'. Key index: "
                            + key + " (" + Keyboard.getKeyName(key) + ")");
                }
            }
        }
        else
        {
            if (Keyboard.isKeyDown(REBIND_KEY)
                    && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                    || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)))
            {
                isListening = true;
                Console.showMessage("The console will be bound to the next key"
                        + " you press (escape to cancel).");
                showRestrictedKeys();
            }
        }
    }

    /**
     * Registers a script to be executable with the RunScript command.
     *
     * @param name The name of the script (for retrieval)
     * @param script The script to be added
     */
    public static void addScript(String name, Script script)
    {
        RunScript.addScript(name, script);
    }

    /**
     * Retrieve a script from {@link RunScript}'s script storage.
     *
     * @param name The name of the script to be retrieved
     * @return The Script registered with this name, null if none found
     */
    public static Script getScript(String name)
    {
        return RunScript.getScript(name);
    }

    private void runTests()
    {
        Global.getSector().addMessage("Running console tests...");
        //parseCommand("runscript help");
        //parseCommand("runscript list");
        //parseCommand("spawnfleet hegemony fake");
        parseCommand("spawnfleet hegemony supplyConvoy");
        parseCommand("addcredits 500000");
        parseCommand("addsupplies 500");
        parseCommand("addfuel 500");
        parseCommand("addship atlas_Hull");
        parseCommand("addwing talon_wing");
        parseCommand("setrelationship player hegemony 5");
        parseCommand("adjustrelationship player hegemony -5.1");
        parseCommand("adjustrelationship player hegemo53ny -5.1");
        parseCommand("runcode Global.getSector().addMessage(\"Test\");");
    }

    private static void showStatus()
    {
        try
        {
            JOptionPane.showMessageDialog(null, "Console status:",
                    "Active thread: " + Thread.currentThread().getName()
                    //+ "\nIn campaign: "
                    //+ (getConsole() != null ? "yes" : "no")
                    + "\nIn battle: "
                    + (getCombatEngine() != null ? "yes" : "no"),
                    JOptionPane.PLAIN_MESSAGE);
        }
        catch (Exception ex)
        {
            showError("Error showing status: ", ex);
        }
    }

    private void listCommands()
    {
        StringBuilder names = new StringBuilder("Help, Status");
        Iterator<Class<? extends BaseCommand>> iter = allCommands.values().iterator();
        Class tmp;

        while (iter.hasNext())
        {
            names.append(", ");
            tmp = iter.next();
            names.append(tmp.getSimpleName());
        }

        showMessage("Valid commands (not case-sensitive): ",
                names.toString(), true);
        showMessage("Running a command with the argument"
                + " 'help' will display more detailed instructions on how to"
                + " use that command.");
    }

    private static String implode(String[] args)
    {
        StringBuilder arg = new StringBuilder();

        for (int x = 0; x < args.length; x++)
        {
            if (x != 0)
            {
                arg.append(" ");
            }

            arg.append(args[x]);
        }

        return arg.toString();
    }

    private boolean parseCommand(String command)
    {
        // Don't try to parse blank lines
        if (command == null || command.length() == 0)
        {
            return false;
        }

        command = command.trim();
        String[] tmp = command.split(" ");
        String com = tmp[0].toLowerCase();
        String args;
        if (tmp.length > 1)
        {
            tmp = Arrays.copyOfRange(tmp, 1, tmp.length);
            args = implode(tmp);
        }
        else
        {
            args = "";
        }

        if (!isInBattle())
        {
            Global.getSector().addMessage("Running command '" + command + "'.",
                    command, Color.GREEN);
        }
        else
        {
            // In battle message hooks not included yet
        }

        if (com.equals("runtests"))
        {
            runTests();
            return true;
        }

        if (com.equals("status"))
        {
            showStatus();
            return true;
        }

        if (com.equals("list"))
        {
            listCommands();
            return true;
        }

        if (com.equals("help"))
        {
            if (!args.isEmpty())
            {
                com = args;
                args = "help";
            }
            else
            {
                listCommands();
                return false;
            }
        }

        if (aliases.containsKey(com))
        {
            com = aliases.get(com);
            if (com.contains(" "))
            {
                tmp = com.split(" ");
                com = tmp[0];
                args = implode(Arrays.copyOfRange(tmp, 1, tmp.length))
                        + " " + args;
            }
        }

        return executeCommand(com, args);
    }

    private synchronized boolean executeCommand(String com, String args)
    {
        BaseCommand command;

        if (allCommands.containsKey(com))
        {
            try
            {
                command = (BaseCommand) allCommands.get(com).newInstance();
            }
            catch (InstantiationException ex)
            {
                showError("Error while retrieving command "
                        + com + ": ", ex);
                return false;
            }
            catch (IllegalAccessException ex)
            {
                showError("Error while retrieving command "
                        + com + ": ", ex);
                return false;
            }
            catch (ClassCastException ex)
            {
                showError("Error while retrieving command "
                        + com + ": ", ex);
                return false;
            }
        }
        else
        {
            showMessage("No such command '" + com + "'!");
            listCommands();
            return false;
        }

        if ("help".equals(args))
        {
            command.showHelp();
            return true;
        }

        if (command.isCombatOnly() ^ isInBattle())
        {
            showMessage("This command can only be run "
                    + (isInBattle() ? "outside" : "during") + " combat!");
            return false;
        }

        try
        {
            return command.runCommand(args);
        }
        catch (Exception ex)
        {
            command.showSyntax();
            showError("Error while running command: ", ex);
            return false;
        }
    }

    /**
     * Formats and word-wraps the supplied text, then outputs it to the player.
     *
     * @param preamble the header for this message, won't be indented
     * @param message the main body of text
     * @param indent whether to indent each line of the main body
     */
    public static void showMessage(String preamble,
            String message, boolean indent)
    {
        if (preamble != null)
        {
            showMessage(preamble);
        }

        if (message == null)
        {
            message = "";
        }

        // Analyse each line of the message seperately
        String[] lines = message.split("\n");
        StringBuilder line = new StringBuilder(LINE_LENGTH);

        // Word wrapping is complicated ;)
        for (int x = 0; x < lines.length; x++)
        {
            // Check if the string even needs to be broken up
            if (lines[x].length() > LINE_LENGTH)
            {
                // Clear the StringBuilder so we can generate a new line
                line.setLength(0);
                // Split the line up into the individual words, and append each
                // word to the next line until the character limit is reached
                String[] words = lines[x].split(" ");
                for (int y = 0; y < words.length; y++)
                {
                    // If this word by itself is longer than the line limit,
                    // just go ahead and post it in its own line
                    if (words[y].length() > LINE_LENGTH)
                    {
                        // Make sure to post the previous line in queue, if any
                        if (line.length() > 0)
                        {
                            printLine(line.toString(), indent);
                            line.setLength(0);
                        }

                        printLine(words[y], indent);
                    }
                    // If this word would put us over the length limit, post
                    // the queue and back up a step (re-check this word with
                    // a blank line - this is in case it trips the above block)
                    else if (words[y].length() + line.length() > LINE_LENGTH)
                    {
                        printLine(line.toString(), indent);
                        line.setLength(0);
                        y--;
                    }
                    // This word won't put us over the limit, add it to the queue
                    else
                    {
                        line.append(words[y]);
                        line.append(" ");

                        // If we have reached the end of the message, ensure
                        // that we post the remaining part of the queue
                        if (y == (words.length - 1))
                        {
                            printLine(line.toString(), indent);
                        }
                    }
                }
            }
            // Entire message fits into a single line
            else
            {
                printLine(lines[x], indent);
            }
        }
    }

    /**
     * Formats and word-wraps the supplied text, then outputs it to the player.
     *
     * @param message the message to output
     */
    public static void showMessage(String message)
    {
        showMessage(null, message, false);
    }

    /**
     * Displays information to the user about an exception that has occurred.
     *
     * @param preamble A short message to be shown before the exception
     * @param ex The exception to display
     */
    public static void showError(String preamble, Exception ex)
    {
        if (preamble == null)
        {
            preamble = "Error: ";
        }
        else if (!preamble.endsWith(": "))
        {
            preamble = preamble + ": ";
        }

        showMessage(preamble + ex.toString(), ex.getMessage(), true);
    }

    private static void printLine(String message, boolean indent)
    {
        if (isInBattle())
        {
            // No in-battle message hooks yet
        }
        else
        {
            if (indent)
            {
                Global.getSector().addMessage("   " + message, CONSOLE_COLOR);
            }
            else
            {
                Global.getSector().addMessage(message, CONSOLE_COLOR);
            }
        }
    }

    /**
     * Automatically called by the game - don't call this manually.
     */
    @Override
    public void advance(SectorAPI sector, LocationAPI location)
    {
        this.location = location;

        if (justReloaded)
        {
            justReloaded = false;
            reload();
        }

        if (showRestrictions)
        {
            showRestrictions = false;
            showRestrictions();
        }

        checkQueue();
        checkBattle();
        checkRebind();
    }

    private static class InputHandler extends Thread
    {
        private Console console;
        boolean shouldStop = false;

        private InputHandler()
        {
        }

        public InputHandler(Console console)
        {
            this.console = console;
        }

        private Console getConsole()
        {
            return console;
        }

        private void setConsole(Console console)
        {
            this.console = console;
        }

        private String getInput()
        {
            return JOptionPane.showInputDialog(null,
                    "Enter a console command (or 'help' for a list of valid commands):",
                    "Starfarer Console", JOptionPane.PLAIN_MESSAGE);
        }

        @Override
        public void run()
        {
            while (!shouldStop)
            {
                if (getConsole() == null)
                {
                    return;
                }

                if (getConsole().checkInput())
                {
                    getConsole().addCommandToQueue(getInput());
                    continue;
                }

                try
                {
                    Thread.sleep(INPUT_FRAMERATE);
                }
                catch (InterruptedException ex)
                {
                    showError("Console input thread interrupted!", ex);
                }
            }
        }
    }
}