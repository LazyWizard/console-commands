package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import javax.swing.JOptionPane;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lwjgl.input.Keyboard;

public class ConsoleCombatListener implements EveryFrameCombatPlugin
{
    private static boolean CACHED_RESET = false;
    private static Method KEYBOARD_RESET;
    private CombatEngineAPI engine;
    private CommandContext context;

    private static boolean checkInput(List<InputEventAPI> input)
    {
        KeyStroke key = Console.getConsoleKey();

        for (InputEventAPI event : input)
        {
            if (event.isConsumed() || event.isMouseEvent())
            {
                continue;
            }

            if (event.isKeyUpEvent() && (event.getEventValue() == key.getKey()))
            {
                boolean modPressed = true;

                if (key.requiresShift()
                        && !(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                        || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)))
                {
                    modPressed = false;
                }

                if (key.requiresControl()
                        && !(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                        || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)))
                {
                    modPressed = false;
                }

                if (modPressed)
                {
                    event.consume();
                    return true;
                }
            }

            return false;
        }

        return false;
    }

    private static void resetKeyboard()
    {
        try
        {
            // Because Keyboard.reset() is private for some reason,
            // we have to go extremely overboard to fix sticky keys...
            if (!CACHED_RESET)
            {
                KEYBOARD_RESET = Keyboard.class.getDeclaredMethod("reset", null);
                KEYBOARD_RESET.setAccessible(true);
                CACHED_RESET = true;
            }

            KEYBOARD_RESET.invoke(null);
        }
        catch (IllegalAccessException | IllegalArgumentException |
                InvocationTargetException | NoSuchMethodException |
                SecurityException ex)
        {
            Console.showException("Failed to reset keyboard!", ex);

            // Do things the hard way
            Keyboard.destroy();
            Keyboard.create();
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        // Temp fix for .6.2a bug
        if (engine != Global.getCombatEngine())
        {
            return;
        }

        // Main menu check
        ShipAPI player = engine.getPlayerShip();
        if (player != null && engine.isEntityInPlay(player))
        {
            if (checkInput(events))
            {
                // Combat, summon regular Java input dialog for now
                // TODO: write an overlay if text rendering is ever added to API
                String rawInput = JOptionPane.showInputDialog(null,
                        "Enter command, or 'help' for a list of valid commands.");
                Console.parseInput(rawInput, context);

                // An unfortunate necessity due to a LWJGL window focus bug
                // Luckily, there shouldn't be any other input this frame
                // if the player is trying to summon the console
                resetKeyboard();
            }

            // COMBAT_SIMULATION will be added when the API supports it
            Console.advance(context);
        }
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        this.engine = engine;
        context = (engine.isInCampaign() ? CommandContext.COMBAT_CAMPAIGN
                : CommandContext.COMBAT_MISSION);
    }
}
