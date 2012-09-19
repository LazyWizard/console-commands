package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpawnPointPlugin;
import data.scripts.console.commands.RunScript;
import java.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public class ConsoleManager implements SpawnPointPlugin
{
    // Constants
    private static final boolean REQUIRE_DEV_MODE = false;
    private static final boolean REQUIRE_RUN_WINDOWED = true;
    private static final int DEFAULT_CONSOLE_KEY = Keyboard.KEY_GRAVE;
    private static final int REBIND_KEY = Keyboard.KEY_K;
    private static final List RESTRICTED_KEYS = new ArrayList();
    // Per-session variables
    private transient int consoleKey = DEFAULT_CONSOLE_KEY;
    private transient boolean justReloaded = false;
    private transient boolean didWarn = false;
    private transient boolean isPressed = false;
    private transient boolean isListening = false;
    // Saved variables
    private LocationAPI location;
    private Map consoleVars = new HashMap();
    private Map extendedCommands = new HashMap();

    static
    {
        RESTRICTED_KEYS.add(REBIND_KEY);
        RESTRICTED_KEYS.add(Keyboard.KEY_ESCAPE);
        RESTRICTED_KEYS.add(Keyboard.KEY_LMETA);
        RESTRICTED_KEYS.add(Keyboard.KEY_RMETA);
    }

    public ConsoleManager(LocationAPI location)
    {
        this.location = location;
    }

    public ConsoleManager()
    {
        location = Global.getSector().getStarSystem("Corvus");

        if (location == null)
        {
            throw new RuntimeException("No LocationAPI set for ConsoleManager!");
        }
    }

    public Object readResolve()
    {
        justReloaded = true;
        didWarn = false;
        isPressed = false;
        return this;
    }

    public boolean registerCommand(String command, Class commandClass)
    {
        // We can't write our own exceptions at the moment, so the console
        // is forced to throw/catch generic Exceptions/RuntimeExceptions
        try
        {
            Console.registerCommand(command, commandClass);
        }
        catch (Exception ex)
        {
            Console.showMultiLineMessage("Failed to register command '"
                    + command + "':", ex.getMessage(), true);
            return false;
        }

        extendedCommands.put(command, commandClass);
        return true;
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

    private void reloadConsoleKey()
    {
        consoleKey = (hasVar("ConsoleKey")
                ? ((Integer) getVar("ConsoleKey")).intValue()
                : DEFAULT_CONSOLE_KEY);
    }

    private void reloadCommands()
    {
        if (!extendedCommands.isEmpty())
        {
            boolean success = true;
            Iterator iter = extendedCommands.entrySet().iterator();
            Map.Entry tmp;

            while (iter.hasNext())
            {
                tmp = (Map.Entry) iter.next();

                try
                {
                    registerCommand((String) tmp.getKey(), (Class) tmp.getValue());
                }
                catch (Exception ex)
                {
                    success = false;
                    Console.showMultiLineMessage("Error: failed to re-register command '"
                            + (String) tmp.getKey() + "':", ex.getMessage(), true);
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
            Map userScripts = (HashMap) getVar("UserScripts");
            Iterator iter = userScripts.entrySet().iterator();
            Map.Entry tmp;

            while (iter.hasNext())
            {
                tmp = (Map.Entry) iter.next();
                RunScript.addScript((String) tmp.getKey(), (Script) tmp.getValue());
            }
        }
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
                Console.showMultiLineMessage("The console will only be visible"
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

    @Override
    public void advance(SectorAPI sector, LocationAPI location)
    {
        // Re-register user/mod-created commands after a reload
        if (justReloaded)
        {
            justReloaded = false;
            reloadConsoleKey();
            reloadCommands();
            reloadScripts();
        }

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
                if (RESTRICTED_KEYS.contains(key))
                {
                    Console.showMessage("That key can't be used for the console!");
                    return;
                }
                else
                {
                    isListening = false;
                    Console.showMessage("The console is now bound to '"
                            + Keyboard.getEventCharacter() + "'. Key index: "
                            + key + " (" + Keyboard.getKeyName(key) + ")");
                    setVar("ConsoleKey", key);
                    reloadConsoleKey();
                    return;
                }
            }
        }
        else
        {
            if (Keyboard.isKeyDown(REBIND_KEY))
            {
                isListening = true;
                Console.showMessage("The console will be bound to the next key"
                        + " you press (escape to cancel).");
            }
        }

        if (!isPressed)
        {
            if (Keyboard.isKeyDown(consoleKey))
            {
                isPressed = true;

                if (!didWarn)
                {
                    didWarn = true;
                    showWarning();
                }

                // Due to a bug with LWJGL input and window focus, the console
                // will only activate once the console key is released
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

                Console.getInput(this);
            }
        }
    }
}