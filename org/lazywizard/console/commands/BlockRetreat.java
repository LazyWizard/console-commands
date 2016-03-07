package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.apache.log4j.Logger;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class BlockRetreat implements BaseCommand
{
    private static final Logger Log = Logger.getLogger(BlockRetreat.class);

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

        // FIXME: Battle never ends while this is active
        final CombatTaskManagerAPI tm = engine.getFleetManager(FleetSide.ENEMY)
                .getTaskManager(false);
        final boolean canRetreat = !tm.isPreventFullRetreat();
        tm.setPreventFullRetreat(canRetreat);
        Console.showMessage("Enemy retreat is now " + (canRetreat ? "allowed." : "blocked."));
        return CommandResult.SUCCESS;
    }
}
