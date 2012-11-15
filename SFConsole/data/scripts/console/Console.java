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
 * Executes commands and handles console output. Can't be instantiated.
 *
 * @see ConsoleManager
 */
public class Console implements SpawnPointPlugin
{
    // Constants
    private static final String COMMAND_PACKAGE = "data.scripts.console.commands";
    private static final boolean REQUIRE_DEV_MODE = false;
    private static final boolean REQUIRE_RUN_WINDOWED = true;
    private static final Color CONSOLE_COLOR = Color.YELLOW;
    private static final int LINE_LENGTH = 80;
    private static final long INPUT_FRAMERATE = (long) (1000 / 20);
    private static final int DEFAULT_CONSOLE_KEY = Keyboard.KEY_GRAVE;
    private static final int REBIND_KEY = Keyboard.KEY_F1; // Shift+key to rebind
    private static final List RESTRICTED_KEYS = new ArrayList();
    // Maps the command to the associated class
    private final Map allCommands = new TreeMap();
    private final Set hardcodedCommands = new HashSet();
    // Per-session variables
    private static boolean inBattle = false;
    private static WeakReference activeConsole, activeEngine;
    private static InputHandler inputHandler;
    private transient boolean justReloaded = false, isListening = false;
    private transient volatile boolean isPressed = false, showRestrictions = true;
    // Saved variables
    private LocationAPI location;
    private volatile List queuedCommands = new ArrayList();
    private int consoleKey = DEFAULT_CONSOLE_KEY;
    private Map consoleVars = new HashMap();
    private Set extendedCommands = new HashSet();

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
    }

    public Console()
    {
        Console.activeConsole = new WeakReference(this);
        addBuiltInCommands();
        reloadInput();
    }

    public Object readResolve()
    {
        justReloaded = true;
        isPressed = false;
        isListening = false;
        Console.activeConsole = new WeakReference(this);
        addBuiltInCommands();
        return this;
    }

    /*@Override
    protected void finalize() throws Throwable
    {
        if (inputHandler != null)
        {
            inputHandler.shouldStop = true;
        }

        super.finalize();
    }*/

    private void addBuiltInCommands()
    {
        // Built-in commands, don't need to go through registerCommand's checks
        allCommands.put("runscript", RunScript.class);
        allCommands.put("runcode", RunCode.class);
        allCommands.put("spawnfleet", SpawnFleet.class);
        allCommands.put("addship", AddShip.class);
        allCommands.put("addwing", AddWing.class);
        allCommands.put("addcredits", AddCredits.class);
        allCommands.put("addfuel", AddFuel.class);
        allCommands.put("addsupplies", AddSupplies.class);
        allCommands.put("setrelationship", SetRelationship.class);
        allCommands.put("adjustrelationship", AdjustRelationship.class);
        allCommands.put("addweapon", AddWeapon.class);
        allCommands.put("additem", AddItem.class);
        allCommands.put("addcrew", AddCrew.class);
        allCommands.put("addmarines", AddMarines.class);
        allCommands.put("allweapons", AllWeapons.class);
        allCommands.put("goto", GoTo.class);
        allCommands.put("home", Home.class);
        allCommands.put("sethome", SetHome.class);
        allCommands.put("gc", GC.class);
        allCommands.put("addcp", AddCP.class);
        allCommands.put("reveal", Reveal.class);
        allCommands.put("unreveal", Unreveal.class);

        // Commands that can't be overwritten
        hardcodedCommands.add("help");
        hardcodedCommands.add("status");
        hardcodedCommands.add("runtests");
        hardcodedCommands.addAll(allCommands.keySet());
    }

    public void registerCommand(Class commandClass) throws Exception
    //throws InvalidCommandObjectException, InvalidCommandPackageException
    {
        String command = commandClass.getSimpleName().toLowerCase();

        if (hardcodedCommands.contains(command))
        {
            // InvalidCommandNameException
            throw new Exception("Can't overwrite built-in commands!");
        }

        // getPackage() won't work for classes compiled with Janino's classloader
        // There's an extremely ugly workaround below
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

    static Console getConsole()
    {
        if (activeConsole == null || activeConsole.get() == null)
        {
            return null;
        }

        return (Console) activeConsole.get();
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
        activeEngine = new WeakReference(engine);
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

        return (CombatEngineAPI) activeEngine.get();
    }

    /**
     * Creates/sets a persistent variable that can be accessed by all console commands.<p>
     *
     * Important: the variable storage is not type-safe! Ensure you use unique names for your variables to avoid conflict!
     *
     * @param varName the name this variable can be retrieved under
     * @param varData the data this variable should hold
     */
    protected void setVar(String varName, Object varData)
    {
        consoleVars.put(varName, varData);
    }

    /**
     * Retrieves the value of the variable varName, if any.
     *
     * @param varName the name of the variable to retrieve
     * @return the data associated with that variable
     */
    protected Object getVar(String varName)
    {
        return consoleVars.get(varName);
    }

    /**
     * Checks for the existence of a variable with the supplied name.
     *
     * @param varName the name of the variable to check the existence of
     * @return true if the variable has been set, false otherwise
     */
    protected boolean hasVar(String varName)
    {
        return consoleVars.keySet().contains(varName);
    }

    /**
     * Gets the current {@link LocationAPI} of the player fleet
     *
     * @return the last location to update advance()
     */
    protected LocationAPI getLocation()
    {
        return location;
    }

    private void reload()
    {
        reloadCommands();
        reloadScripts();
        reloadInput();

        Global.getSector().addMessage("To rebind the console to another key,"
                + " press shift+" + Keyboard.getKeyName(REBIND_KEY)
                + " while on the campaign map.");
    }

    private void reloadCommands()
    {
        if (!extendedCommands.isEmpty())
        {
            boolean hadError = false;
            Iterator iter = extendedCommands.iterator();
            Class tmp;

            while (iter.hasNext())
            {
                tmp = (Class) iter.next();

                try
                {
                    registerCommand(tmp);
                }
                catch (Exception ex)
                {
                    hadError = true;
                    showError("Error: failed to re-register command '"
                            + (String) tmp.getSimpleName() + "'!", ex);
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
            Map userScripts = (Map) getVar("UserScripts");
            Iterator iter = userScripts.entrySet().iterator();
            Map.Entry tmp;

            while (iter.hasNext())
            {
                tmp = (Map.Entry) iter.next();
                RunScript.addScript((String) tmp.getKey(), (Script) tmp.getValue());
            }
        }
    }

    private void reloadInput()
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

        queuedCommands.add(command);
    }

    private static boolean allowConsole()
    {
        return !(REQUIRE_RUN_WINDOWED && Display.isFullscreen())
                || (REQUIRE_DEV_MODE && !Global.getSettings().getBoolean("devMode"));
    }

    private static void showWarning()
    {
        if (allowConsole() && !REQUIRE_RUN_WINDOWED)
        {
            Console.showMessage("The console will only be visible"
                    + " if you run the game in windowed mode.\n");
        }
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
            keys.append(Keyboard.getKeyName(((Integer) RESTRICTED_KEYS.get(x)).intValue()));
            if (x < RESTRICTED_KEYS.size() - 1)
            {
                keys.append(", ");
            }
        }

        Global.getSector().addMessage("Restricted keys: " + keys.toString());
    }

    protected void checkQueue()
    {
        if (queuedCommands.isEmpty())
        {
            return;
        }

        for (int x = 0; x < queuedCommands.size(); x++)
        {
            parseCommand((String) queuedCommands.get(x));
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
            showMessage("Console status:",
                    "Active thread: " + Thread.currentThread().getName()
                    //+ "\nIn campaign: "
                    //+ (getConsole() != null ? "yes" : "no")
                    + "\nIn battle: "
                    + (getCombatEngine() != null ? "yes" : "no"),
                    true);
        }
        catch (Exception ex)
        {
            showError("Error showing status: ", ex);
        }
    }

    private void listCommands()
    {
        StringBuilder names = new StringBuilder("Help, Status");
        Iterator iter = allCommands.values().iterator();
        Class tmp;

        while (iter.hasNext())
        {
            names.append(", ");
            tmp = (Class) iter.next();
            names.append(tmp.getSimpleName());
        }

        showMessage("Valid commands (not case-sensitive): ",
                names.toString(), true);
        showMessage("Running a command with the argument"
                + " 'help' will display more detailed instructions on how to"
                + " use that command.");
    }

    private boolean parseCommand(String command)
    {
        // Don't try to parse blank lines
        if (command == null || command.length() == 0)
        {
            return false;
        }

        String[] args = command.split(" ");
        String com = args[0].toLowerCase();

        Global.getSector().addMessage("Running command '" + command + "'.",
                command, Color.GREEN);

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
            if (args.length == 2)
            {
                com = args[1];
                args[1] = "help";
            }
            else
            {
                listCommands();
                return false;
            }
        }

        if (args.length > 1)
        {
            StringBuilder arg = new StringBuilder();

            for (int x = 1; x < args.length; x++)
            {
                if (x != 1)
                {
                    arg.append(" ");
                }

                arg.append(args[x]);
            }

            return executeCommand(com, arg.toString());
        }

        return executeCommand(com, "");
    }

    public static void addScript(String name, Script script)
    {
        RunScript.addScript(name, script);
    }

    private synchronized boolean executeCommand(String com, String args)
    {
        BaseCommand command;

        if (allCommands.containsKey(com))
        {
            try
            {
                command = (BaseCommand) ((Class) allCommands.get(com)).newInstance();
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

        if (command.isCombatOnly() && !isInBattle())
        {
            showMessage("This command can only be run during combat!");
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
        private WeakReference console;
        boolean shouldStop = false;

        private InputHandler()
        {
        }

        public InputHandler(Console console)
        {
            this.console = new WeakReference(console);
        }

        private Console getConsole()
        {
            return (Console) console.get();
        }

        private void setConsole(Console console)
        {
            this.console = new WeakReference(console);
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