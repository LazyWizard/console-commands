package org.lazywizard.console.commands;

import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Logger;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class BlockRetreat implements BaseCommand
{
    private static final Logger Log = Logger.getLogger(BlockRetreat.class);
    private static final String PLUGIN_ID = "BlockRetreat";

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isSimulation())
        {
            Console.showMessage("Retreat is always impossible in a simulation!");
            return CommandResult.ERROR;
        }

        final CombatTaskManagerAPI tm = engine.getFleetManager(FleetSide.ENEMY)
                .getTaskManager(false);
        final boolean canRetreat = !tm.isPreventFullRetreat();
        tm.setPreventFullRetreat(canRetreat);

        // Fix for battles never ending while this command is active
        if (!canRetreat && !CommandUtils.isCombatPluginRegistered(PLUGIN_ID))
        {
            final EveryFrameCombatPlugin plugin = new EnsureBattleEndPlugin();
            CommandUtils.registerCombatPlugin(PLUGIN_ID, plugin);
            engine.addPlugin(plugin);
        }
        else if (canRetreat && CommandUtils.isCombatPluginRegistered(PLUGIN_ID))
        {
            engine.removePlugin(CommandUtils.getRegisteredCombatPlugin(PLUGIN_ID));
            CommandUtils.deregisterCombatPlugin(PLUGIN_ID);
        }

        Console.showMessage("Enemy retreat is now " + (canRetreat ? "allowed." : "blocked."));
        return CommandResult.SUCCESS;
    }

    private static class EnsureBattleEndPlugin extends BaseEveryFrameCombatPlugin
    {
        private static final float TIME_BETWEEN_CHECKS = 1f;
        private float nextCheck = TIME_BETWEEN_CHECKS;

        @Override
        public void advance(float amount, List<InputEventAPI> events)
        {
            final CombatEngineAPI engine = Global.getCombatEngine();
            if (engine.isPaused())
            {
                return;
            }

            nextCheck -= amount;
            if (nextCheck <= 0f)
            {
                if (engine.getFleetManager(FleetSide.ENEMY).getDeployedCopy().isEmpty())
                {
                    Log.info("Forcing player victory");
                    engine.endCombat(1f, FleetSide.PLAYER);
                    engine.removePlugin(this);
                    return;
                }

                nextCheck = TIME_BETWEEN_CHECKS;
            }
        }
    }
}
