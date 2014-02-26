package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;
import javax.swing.JOptionPane;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lwjgl.input.Keyboard;

public class ConsoleCombatListener implements EveryFrameCombatPlugin
{
    private CombatEngineAPI engine;
    private CommandContext context;

    private static boolean checkInput(List<InputEventAPI> inputs)
    {
        KeyStroke key = Console.getConsoleKey();

        for (InputEventAPI input : inputs)
        {
            if (input.isConsumed())
            {
                continue;
            }

            if (input.isKeyDownEvent() && input.getEventValue() == key.getKey())
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
                    input.consume();
                    return true;
                }
            }

            return false;
        }

        return false;
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
