package org.lazywizard.console;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.ConsoleSettings.KeyStroke;
import org.lazywizard.lazylib.StringUtils;

public class ConsoleCombatListener extends BaseEveryFrameCombatPlugin implements ConsoleListener
{
    private CommandContext context;

    //<editor-fold defaultstate="collapsed" desc="Input handling">
    private static boolean checkInput(List<InputEventAPI> input)
    {
        KeyStroke key = Console.getSettings().getConsoleSummonKey();

        for (InputEventAPI event : input)
        {
            if (event.isConsumed() || !event.isKeyDownEvent()
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
    //</editor-fold>

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
            if (checkInput(events))
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

        final String[] messages = output.split("\n");
        Collections.reverse(Arrays.asList(messages));
        for (String message : messages) {
            message = StringUtils.wrapString(message, Console.getSettings().getMaxOutputLineLength());
            ui.addMessage(0, Console.getSettings().getOutputColor(), message);
        }

        return true;
    }
}
