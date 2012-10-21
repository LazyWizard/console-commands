package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpawnPointPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import data.scripts.console.commands.RunScript;
import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.*;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

/**
 * Handles input, provides save-specific console settings and safe public access to certain methods of {@link Console}.
 */
public final class ConsoleManager implements SpawnPointPlugin
{
    // Constants
    private static final boolean REQUIRE_DEV_MODE = false;
    private static final boolean REQUIRE_RUN_WINDOWED = true;
    private static final long INPUT_FRAMERATE = (long) (1000 / 20);
    private static final int DEFAULT_CONSOLE_KEY = Keyboard.KEY_GRAVE;
    private static final int REBIND_KEY = Keyboard.KEY_F1; // Shift+key to rebind
    private static final List RESTRICTED_KEYS = new ArrayList();
    // Per-session variables
    private static boolean inBattle = false;
    private static WeakReference activeEngine;
    private static WeakReference inputHandler;
    private transient boolean justReloaded = false;
    private transient volatile boolean isPressed = false;
    private transient boolean isListening = false;
    // Saved variables
    private LocationAPI location;
    private volatile List queuedCommands = new ArrayList();
    private int consoleKey = DEFAULT_CONSOLE_KEY;
    private Map consoleVars = new HashMap();
    private Set extendedCommands = new HashSet();

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

        RESTRICTED_KEYS.add(REBIND_KEY);
        RESTRICTED_KEYS.add(Keyboard.KEY_ESCAPE);
        RESTRICTED_KEYS.add(Keyboard.KEY_LMETA);
        RESTRICTED_KEYS.add(Keyboard.KEY_RMETA);
        RESTRICTED_KEYS.add(Keyboard.KEY_LSHIFT);
        RESTRICTED_KEYS.add(Keyboard.KEY_RSHIFT);
    }

    public ConsoleManager()
    {
        reloadInput();
    }

    public Object readResolve()
    {
        justReloaded = true;
        isPressed = false;
        isListening = false;
        return this;
    }

    /**
     * Registers a command with the {@link Console}.
     *
     * Commands must pass validation, otherwise registration will fail!<p>
     *
     * Validation consists of the following:<br>
     *  * Checking that there isn't a built-in command with the same name<br>
     *  * Checking that the command's class is in the correct package<br>
     *  * Checking that the command extends {@link BaseCommand}
     *
     * @param commandClass the class object of the command to be registered
     * @return true if the command was successfully added, false otherwise
     */
    public boolean registerCommand(Class commandClass)
    {
        // We can't write our own exceptions at the moment, so the console
        // is forced to throw/catch generic Exceptions/RuntimeExceptions
        try
        {
            Console.registerCommand(commandClass);
        }
        catch (Exception ex)
        {
            Console.showMessage("Failed to register command '"
                    + commandClass.getSimpleName() + "':", ex.getMessage(), true);
            return false;
        }

        extendedCommands.add(commandClass);
        return true;
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

                showRestrictions();
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

                if (!registerCommand(tmp))
                {
                    hadError = true;
                    Console.showMessage("Error: failed to re-register command '"
                            + (String) tmp.getSimpleName() + "'!");
                    iter.remove();
                }
            }

            if (hadError)
            {
                Console.showMessage("There were some errors while registering the extended console commands.");
            }
            else
            {
                Console.showMessage("Extended console commands registered successfully!");
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
        if (inputHandler != null && inputHandler.get() != null)
        {
            ((InputHandler) inputHandler.get()).shouldStop = true;
        }

        InputHandler tmp;

        tmp = new InputHandler(Thread.currentThread(), this);
        tmp.setName("Console-Input");
        tmp.setDaemon(true);
        inputHandler = new WeakReference(tmp);
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
        if (allowConsole())
        {
            Console.showMessage("If the game appears frozen, switch windows to"
                    + " 'Starfarer Console' to enter your command.");

            if (!REQUIRE_RUN_WINDOWED)
            {
                Console.showMessage("The console will only be visible"
                        + " if you run the game in windowed mode.\n");
            }
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

    private void checkQueue()
    {
        if (queuedCommands.isEmpty())
        {
            return;
        }

        Console.setManager(this);
        for (int x = 0; x < queuedCommands.size(); x++)
        {
            Console.parseCommand((String) queuedCommands.get(x));
        }

        queuedCommands.clear();
    }

    private void checkBattle()
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

    @Override
    public void advance(SectorAPI sector, LocationAPI location)
    {
        this.location = location;

        if (justReloaded)
        {
            justReloaded = false;
            reload();
        }

        checkQueue();
        checkBattle();
        checkRebind();
    }

    private static class InputHandler extends Thread
    {
        private Thread mainThread;
        private ConsoleManager manager;
        boolean shouldStop = false;

        private InputHandler()
        {
        }

        public InputHandler(Thread thread, ConsoleManager manager)
        {
            this.mainThread = thread;
            this.manager = manager;
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
                if (manager.checkInput())
                {
                    String command = getInput();

                    if (ConsoleManager.isInBattle())
                    {
                        JOptionPane.showMessageDialog(null,
                                "There is no stable in-battle command"
                                + " support yet, sorry!", "Error",
                                JOptionPane.ERROR_MESSAGE);

                        // Nasty concurrency-avoiding hack - trading one set
                        // of problems for another since 2012!
                        /*mainThread.suspend();
                         Console.setManager(ConsoleManager.this);
                         Console.parseCommand(command);
                         mainThread.resume();*/
                    }
                    else
                    {
                        manager.addCommandToQueue(command);
                    }

                    continue;
                }

                try
                {
                    Thread.sleep(INPUT_FRAMERATE);
                }
                catch (InterruptedException ex)
                {
                    throw new RuntimeException("Console input thread interrupted!");
                }
            }
        }
    }
}
