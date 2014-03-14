package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

public class ConsoleCombatListener implements EveryFrameCombatPlugin
{
    // Whether combat toggle commands should stay on for subsequent battles
    private static boolean PERSISTENT_COMBAT_COMMANDS = false;
    private static final Map<String, Class<? extends BaseCombatToggleCommand>> activePlugins = new HashMap<>();
    private final List<BaseCombatToggleCommand> activeCommands = new ArrayList<>();
    private CombatEngineAPI engine;
    private CommandContext context;

    static void setCommandPersistence(boolean persistent)
    {
        PERSISTENT_COMBAT_COMMANDS = persistent;
    }

    //<editor-fold defaultstate="collapsed" desc="Input handling">
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
        return JOptionPane.showInputDialog(null,
                "Enter command, or 'help' for a list of valid commands.");
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

        if (!PERSISTENT_COMBAT_COMMANDS)
        {
            activePlugins.clear();
        }
        else
        {
            for (Iterator<Class<? extends BaseCombatToggleCommand>> iter
                    = activePlugins.values().iterator(); iter.hasNext();)
            {
                Class<? extends BaseCombatToggleCommand> cmdClass = iter.next();
                try
                {
                    BaseCombatToggleCommand cmd = cmdClass.newInstance();
                    activeCommands.add(cmd);
                    cmd.onActivate(engine);
                }
                catch (InstantiationException | IllegalAccessException ex)
                {
                    Console.showException("Failed to instantiate combat plugin '"
                            + cmdClass.getCanonicalName() + "':", ex);
                    iter.remove();
                }
            }
        }
    }
}
