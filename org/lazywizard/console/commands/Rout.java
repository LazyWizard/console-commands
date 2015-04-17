package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Rout implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isSimulation())
        {
            Console.showMessage("You can't order a rout in a simulation!");
            return CommandResult.ERROR;
        }

        engine.getFleetManager(FleetSide.ENEMY).orderFullRetreat();
        Console.showMessage("Enemy side is now retreating.");
        return CommandResult.SUCCESS;
    }
}
