package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SpawnPointPlugin;
import java.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public class ConsoleManager implements SpawnPointPlugin
{
    // Constants
    private static final boolean REQUIRE_DEV_MODE = false;
    private static final boolean REQUIRE_RUN_WINDOWED = true;
    private static final int CONSOLE_KEY = Keyboard.KEY_GRAVE;
    // Per-session variables
    private transient boolean justReloaded = false;
    private transient boolean didWarn = false;
    private transient boolean isPressed = false;
    // Saved variables
    private LocationAPI location;
    private Map consoleVars = new HashMap();
    private Map extendedCommands = new HashMap();

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

    public LocationAPI getLocation()
    {
        return location;
    }

    private void reloadCommands()
    {
        if (!extendedCommands.isEmpty())
        {
            Iterator iter = extendedCommands.keySet().iterator();
            String key;

            while (iter.hasNext())
            {
                key = (String) iter.next();

                try
                {
                    registerCommand(key, (Class) extendedCommands.get(key));
                }
                catch (Exception ex)
                {
                    Console.showMultiLineMessage("Error: failed to re-register command '"
                            + key + "':", ex.getMessage(), true);
                }
            }

            //showMessage("Extended console commands registered successfully!");
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
        Console.showMessage("The console will only function"
                + (REQUIRE_RUN_WINDOWED ? " in windowed mode" : "")
                + (REQUIRE_DEV_MODE ? " with devmode active." : "."));
    }

    @Override
    public void advance(SectorAPI sector, LocationAPI location)
    {
        // Re-register user/mod-created commands after a reload
        if (justReloaded)
        {
            justReloaded = false;
            reloadCommands();
        }

        if (!isPressed)
        {
            if (Keyboard.isKeyDown(CONSOLE_KEY))
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
            if (!Keyboard.isKeyDown(CONSOLE_KEY))
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