package org.lazywizard.console;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatUIAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import javax.swing.JOptionPane;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.ConsoleSettings.KeyStroke;

public class ConsoleCombatListener implements EveryFrameCombatPlugin, ConsoleListener
{
    private static final float MESSAGE_SIZE = 25f;
    // Controls spawning and assigning threads for the input popup
    // Multi-threading allows the popup to be on top of the game window
    private final Executor exe = Executors.newSingleThreadExecutor();
    // Stores input from the console popup thread that hasn't been parsed yet
    private final Queue<String> input = new ConcurrentLinkedQueue<>();
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
            // Parse stored input
            synchronized (input)
            {
                while (!input.isEmpty())
                {
                    Console.parseInput(input.poll(), context);
                }
            }

            if (checkInput(events))
            {
                // Combat, summon regular Java input dialog for now
                // TODO: write an overlay if text rendering is ever added to API
                exe.execute(new ShowInputPopup());
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
        for (int x = 0; x < messages.length; x++)
        {
            ui.addMessage(0, Console.getSettings().getOutputColor(), messages[x]);
        }

        return true;
    }

    @Override
    public void renderInWorldCoords(ViewportAPI view)
    {
    }

    @Override
    public void renderInUICoords(ViewportAPI view)
    {
    }

    private class ShowInputPopup implements Runnable
    {
        @Override
        public void run()
        {
            String tmp = JOptionPane.showInputDialog(null, CommonStrings.INPUT_QUERY);
            if (tmp != null)
            {
                input.add(tmp);
            }
        }
    }
}
