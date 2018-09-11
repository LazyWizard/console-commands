package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatUIAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.lazylib.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConsoleCombatListener extends BaseEveryFrameCombatPlugin implements ConsoleListener
{
    private CommandContext context;

    @Override
    public void advance(float amount, List<InputEventAPI> events)
    {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (engine == null)
        {
            return;
        }

        // Main menu check
        ShipAPI player = engine.getPlayerShip();
        if (player != null && engine.isEntityInPlay(player))
        {
            if (Console.getSettings().getConsoleSummonKey().isPressed(events))
            {
                ConsoleOverlay.show(context);
            }

            // Advance the console and all combat commands
            Console.advance(amount, this);
        }
    }

    @Override
    public void init(CombatEngineAPI engine)
    {
        // Determine what context this battle is in
        if (engine.isSimulation())
        {
            context = CommandContext.COMBAT_SIMULATION;
        }
        else if (engine.isInCampaign())
        {
            context = CommandContext.COMBAT_CAMPAIGN;
        }
        else
        {
            context = CommandContext.COMBAT_MISSION;
        }
    }

    @Override
    public CommandContext getContext()
    {
        return context;
    }

    @Override
    public boolean showOutput(String output)
    {
        final CombatUIAPI ui = Global.getCombatEngine().getCombatUI();
        if (ui == null)
        {
            return false;
        }

        // Fallback if the console overlay doesn't exist for some reason
        final String[] messages = output.split("\n");
        Collections.reverse(Arrays.asList(messages));
        for (String message : messages)
        {
            message = StringUtils.wrapString(message, 80);
            ui.addMessage(0, Console.getSettings().getOutputColor(), message);
        }

        return true;
    }
}
