package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;
import javax.swing.JOptionPane;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.ConsoleSettings.KeyStroke;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

public class ConsoleCombatListener implements EveryFrameCombatPlugin, ConsoleListener
{
    private static final float MESSAGE_SIZE = 25f;
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

    static void resetKeyboard()
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
        return JOptionPane.showInputDialog(null, CommonStrings.INPUT_QUERY);
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
        String[] messages = output.toString().split("\n");
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
}
