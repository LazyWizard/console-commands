package org.lazywizard.sfconsole;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import org.lwjgl.input.Keyboard;

public class ConsoleScript implements EveryFrameScript
{
    private transient boolean justReloaded = false, isListening = false;
    private transient volatile boolean isPressed = false, showRestrictions = true;
    private int consoleKey = Console.DEFAULT_CONSOLE_KEY;

    public Object readResolve()
    {
        justReloaded = true;
        isPressed = false;
        isListening = false;
        Console.setConsoleScript(this);
        return this;
    }

    protected void checkInput()
    {
        if (!Keyboard.isCreated())
        {
            return;
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

            if (key != Keyboard.KEY_NONE && key != Console.REBIND_KEY)
            {
                if (!Console.RESTRICTED_KEYS.contains(key))
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
            if (Keyboard.isKeyDown(Console.REBIND_KEY)
                    && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                    || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)))
            {
                isListening = true;
                Console.showMessage("The console will be bound to the next key"
                        + " you press (escape to cancel).");
                Console.showRestrictedKeys();
            }
        }

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

                if (Console.allowConsole())
                {
                    Console.addCommandToQueue(Console.getInput());
                }

                showRestrictions = true;
            }
        }
    }

    /**
     * Automatically called by the game - don't call this manually.
     */
    @Override
    public void advance(float amount)
    {
        Console.activeSector = Global.getSector();
        Console.inBattle = false;

        if (justReloaded)
        {
            justReloaded = false;
            Console.showOnLoadMessages();
        }

        if (showRestrictions)
        {
            showRestrictions = false;
            Console.showRestrictions();
        }

        checkInput();
    }

    @Override
    public boolean isDone()
    {
        return false;
    }

    @Override
    public boolean runWhilePaused()
    {
        return true;
    }
}
