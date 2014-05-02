package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.ConsoleSettings.KeyStroke;
import org.lwjgl.util.vector.Vector2f;

public class ConsoleCombatListener implements EveryFrameCombatPlugin, ConsoleListener
{
    private static final float MESSAGE_SIZE = 25f;
    // Controls spawning and assigning threads for the input popup
    // Multi-threading allows the popup to be on top of the game window
    private static final Executor exe = Executors.newSingleThreadExecutor();
    // Stores input from the console popup thread that hasn't been parsed yet
    private final Queue<String> input = new ConcurrentLinkedQueue<>();
    // The Y offset of console output this frame, used so
    // that multiple messages while paused won't overlap
    private float messageOffset = 0f;
    private CombatEngineAPI engine;
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
        // Temp fix for .6.2a bug
        if (engine != Global.getCombatEngine())
        {
            return;
        }

        if (!engine.isPaused())
        {
            messageOffset = 0f;
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
        this.engine = engine;
        // COMBAT_SIMULATION will be added when the API supports it
        context = (engine.isInCampaign() ? CommandContext.COMBAT_CAMPAIGN
                : CommandContext.COMBAT_MISSION);
    }

    @Override
    public CommandContext getContext()
    {
        return context;
    }

    @Override
    public void showOutput(String output)
    {
        // TODO: the values here are kind of arbitrary, need to be worked out properly
        // TODO: display to the side of the player's ship furthest from the edge of the screen
        ShipAPI player = engine.getPlayerShip();
        String[] messages = output.split("\n");
        for (int x = 0; x < messages.length; x++)
        {
            engine.addFloatingText(Vector2f.add(
                    new Vector2f(-Console.getSettings().getMaxOutputLineLength() / 2f,
                            -(player.getCollisionRadius() + (MESSAGE_SIZE * 2)
                            + messageOffset + (x * MESSAGE_SIZE))),
                    player.getLocation(), null), messages[x], MESSAGE_SIZE,
                    Console.getSettings().getOutputColor(), player, 0f, 0f);
        }

        messageOffset += messages.length * MESSAGE_SIZE;
    }

    private class ShowInputPopup implements Runnable
    {
        @Override
        public void run()
        {
            input.add(JOptionPane.showInputDialog(null, CommonStrings.INPUT_QUERY));
        }
    }
}
