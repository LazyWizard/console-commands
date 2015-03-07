package org.lazywizard.console;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import javax.swing.JOptionPane;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.ConsoleSettings.KeyStroke;
import org.lwjgl.util.vector.Vector2f;

public class ConsoleCombatListener implements EveryFrameCombatPlugin, ConsoleListener
{
    private static final float MESSAGE_SIZE = 25f;
    // Controls spawning and assigning threads for the input popup
    // Multi-threading allows the popup to be on top of the game window
    private final Executor exe = Executors.newSingleThreadExecutor();
    // Stores input from the console popup thread that hasn't been parsed yet
    private final Queue<String> input = new ConcurrentLinkedQueue<>();
    // The Y offset of console output this frame, used so
    // that multiple messages while paused won't overlap
    private float messageOffset = 0f;
    // The position of console output this frame, used so
    // that the output is always centered on the screen
    private Vector2f messagePos = null;
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

        // Reset new message offset each frame
        if (!engine.isPaused())
        {
            messagePos = null;
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
    public void showOutput(String output)
    {
        // TODO: Clean this method up, or comment it more, or... something
        ShipAPI player = Global.getCombatEngine().getPlayerShip();
        String[] messages = output.split("\n");

        // Ensure messages are centered, but don't reset the position
        // if multiple commands are entered in one frame
        if (messagePos == null)
        {
            ViewportAPI view = Global.getCombatEngine().getViewport();
            messagePos = new Vector2f(view.getCenter().x,
                    (view.getLLY() + view.getVisibleHeight()) - (50f * view.getViewMult()));
        }

        for (int x = 0; x < messages.length; x++)
        {
            // TODO: The values here are kind of arbitrary, need to be worked out properly
            Global.getCombatEngine().addFloatingText(Vector2f.add(
                    new Vector2f(-Console.getSettings().getMaxOutputLineLength() / 2f,
                            -((MESSAGE_SIZE * 2) + messageOffset + (x * MESSAGE_SIZE))),
                    messagePos, null), messages[x], MESSAGE_SIZE,
                    Console.getSettings().getOutputColor(), player, 0f, 0f);
        }

        messageOffset += messages.length * MESSAGE_SIZE;
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
