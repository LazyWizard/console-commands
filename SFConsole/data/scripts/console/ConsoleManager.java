package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpawnPointPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import data.scripts.console.commands.RunScript;
import java.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public class ConsoleManager implements SpawnPointPlugin
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
    private static CombatEngineAPI activeEngine;
    private transient Timer timer = new Timer();
    private transient boolean justReloaded = false;
    private transient boolean isPressed = false;
    private transient boolean isListening = false;
    // Saved variables
    private LocationAPI location;
    private int consoleKey = DEFAULT_CONSOLE_KEY;
    private Map consoleVars = new HashMap();
    private Set extendedCommands = new HashSet();

    static
    {
        RESTRICTED_KEYS.add(REBIND_KEY);
        RESTRICTED_KEYS.add(Keyboard.KEY_ESCAPE);
        RESTRICTED_KEYS.add(Keyboard.KEY_LMETA);
        RESTRICTED_KEYS.add(Keyboard.KEY_RMETA);
        RESTRICTED_KEYS.add(Keyboard.KEY_LSHIFT);
        RESTRICTED_KEYS.add(Keyboard.KEY_RSHIFT);
    }

    public ConsoleManager(LocationAPI location)
    {
        this.location = location;
        reloadInput();
    }

    public ConsoleManager()
    {
        location = Global.getSector().getStarSystem("Corvus");

        if (location == null)
        {
            throw new RuntimeException("No LocationAPI set for ConsoleManager!");
        }

        reloadInput();
    }

    public Object readResolve()
    {
        justReloaded = true;
        isPressed = false;
        isListening = false;
        return this;
    }

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

    private void checkInput()
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

                if (!allowConsole())
                {
                    showRestrictions();
                    return;
                }

                Console.getInput();
            }
        }
    }

    public static void setInBattle(boolean isInBattle)
    {
        inBattle = isInBattle;
    }

    public static boolean isInBattle()
    {
        return inBattle;
    }

    public static void setCombatEngine(CombatEngineAPI engine)
    {
        activeEngine = engine;
    }

    public static CombatEngineAPI getCombatEngine()
    {
        return activeEngine;
    }

    public void setVar(String varName, Object varData)
    {
        consoleVars.put(varName, varData);
    }

    public Object getVar(String varName)
    {
        return consoleVars.get(varName);
    }

    public boolean hasVar(String varName)
    {
        return consoleVars.keySet().contains(varName);
    }

    public LocationAPI getLocation()
    {
        return location;
    }

    public int getConsoleKey()
    {
        return consoleKey;
    }

    public void setConsoleKey(int key)
    {
        consoleKey = key;
    }

    private void reload()
    {
        Console.setManager(this);
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
            boolean success = true;
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
                    success = false;
                    Console.showMessage("Error: failed to re-register command '"
                            + (String) tmp.getSimpleName() + "':", ex.getMessage(), true);
                    iter.remove();
                }
            }

            if (success)
            {
                Console.showMessage("Extended console commands registered successfully!");
            }
            else
            {
                Console.showMessage("There were some errors while registering the extended console commands.");
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
        if (timer != null)
        {
            timer.cancel();
        }

        timer = new Timer("Console-Input", true);
        timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                ConsoleManager.this.checkInput();
            }
        }, 0, INPUT_FRAMERATE);
    }

    public static boolean allowConsole()
    {
        return !(REQUIRE_RUN_WINDOWED && Display.isFullscreen())
                || (REQUIRE_DEV_MODE && !Global.getSettings().getBoolean("devMode"));
    }

    public static void showWarning()
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

    public static void showRestrictions()
    {
        if (REQUIRE_RUN_WINDOWED || REQUIRE_DEV_MODE)
        {
            Console.showMessage("The console will only function"
                    + (REQUIRE_RUN_WINDOWED ? " in windowed mode" : "")
                    + (REQUIRE_DEV_MODE ? " with devmode active" : "") + ".");
        }
    }

    public static void showRestrictedKeys()
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
                    setConsoleKey(key);
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
        if (justReloaded)
        {
            justReloaded = false;
            reload();
        }

        checkBattle();
        checkRebind();
    }
}