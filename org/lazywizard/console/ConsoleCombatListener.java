package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;
import javax.swing.JOptionPane;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;

public class ConsoleCombatListener implements EveryFrameCombatPlugin
{
    private CombatEngineAPI engine;
    private CommandContext context;

    private static boolean checkInput(List<InputEventAPI> input)
    {
        KeyStroke key = Console.getConsoleKey();

        for (InputEventAPI event : input)
        {
            // Since remaining input will be nuked on success,
            // we can safely check both keyboard event types
            if (event.isConsumed() || !event.isKeyboardEvent()
                    || event.getEventValue() != key.getKey())
            {
                continue;
            }

            if ((key.requiresShift() && !event.isShiftDown())
                    || (key.requiresControl() && !event.isCtrlDown())
                    || (key.requiresAlt() && !event.isAltDown()))
            {
                return false;
            }

            event.consume();
            return true;
        }

        return false;
    }

    private static void resetKeyboard()
    {
        try
        {
            // Because Keyboard.reset() doesn't seem to reset modifier keys,
            // we have to go extremely overboard to fix sticky keys...
            Keyboard.destroy();
            Keyboard.create();
        }
        catch (LWJGLException ex)
        {
            Console.showException("Failed to reset keyboard!", ex);
        }
    }

    private static String getInput()
    {
        return JOptionPane.showInputDialog(null,
                "Enter command, or 'help' for a list of valid commands.");
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
                String rawInput = getInput();
                Console.parseInput(rawInput, context);

                // An unfortunate necessity due to a LWJGL window focus bug
                // Luckily, there shouldn't be any other input this frame
                // if the player is trying to summon the console
                resetKeyboard();
            }

            Console.advance(context);
        }
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        this.engine = engine;
        // COMBAT_SIMULATION will be added when the API supports it
        context = (engine.isInCampaign() ? CommandContext.COMBAT_CAMPAIGN
                : CommandContext.COMBAT_MISSION);
    }
}
