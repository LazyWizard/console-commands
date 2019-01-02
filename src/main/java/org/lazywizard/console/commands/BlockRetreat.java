package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.cheatmanager.CheatPlugin;
import org.lazywizard.console.cheatmanager.CombatCheatManager;

import java.util.List;

public class BlockRetreat implements BaseCommand
{
    private static final Logger Log = Global.getLogger(BlockRetreat.class);
    private static final String CHEAT_ID = "lw_console_blockretreat";

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

        if (CombatCheatManager.isEnabled(CHEAT_ID))
        {
            CombatCheatManager.disableCheat(CHEAT_ID);
            Console.showMessage("Enemy retreat is now allowed.");
            return CommandResult.SUCCESS;
        }
        else
        {
            CombatCheatManager.enableCheat(CHEAT_ID, "Prevent Enemy Retreat", new BlockRetreatPlugin(), null);
            Console.showMessage("Enemy retreat is now prevented.");
            return CommandResult.SUCCESS;
        }
    }

    private static class BlockRetreatPlugin extends CheatPlugin
    {
        private static final float TIME_BETWEEN_CHECKS = 1f;
        private float nextCheck = TIME_BETWEEN_CHECKS;
        private boolean endedCombat = false;

        @Override
        public void onStart(@NotNull CombatEngineAPI engine)
        {
            final CombatTaskManagerAPI tm = engine.getFleetManager(FleetSide.ENEMY).getTaskManager(false);
            tm.setPreventFullRetreat(true);
        }

        @Override
        public void onEnd(@NotNull CombatEngineAPI engine)
        {
            final CombatTaskManagerAPI tm = engine.getFleetManager(FleetSide.ENEMY)
                    .getTaskManager(false);
            tm.setPreventFullRetreat(false);
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events)
        {
            if (endedCombat) return;

            // Ensure battle ends when all enemies are defeated
            nextCheck -= amount;
            if (nextCheck <= 0f)
            {
                if (Global.getCombatEngine().getFleetManager(FleetSide.ENEMY).getDeployedCopy().isEmpty())
                {
                    Log.info("Forcing player victory");
                    Global.getCombatEngine().endCombat(1f, FleetSide.PLAYER);
                    endedCombat = true;
                    return;
                }

                nextCheck = TIME_BETWEEN_CHECKS;
            }
        }

        @Override
        public boolean runWhilePaused()
        {
            return true;
        }
    }
}
