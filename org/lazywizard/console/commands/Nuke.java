package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Nuke implements BaseCommand
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
        for (ShipAPI ship : engine.getShips())
        {
            if (ship.isHulk() || ship.isShuttlePod())
            {
                continue;
            }

            if (ship.getOwner() == FleetSide.ENEMY.ordinal())
            {
                Kill.killShip(ship);
            }
        }

        Console.showMessage("All enemy ships destroyed.");
        return CommandResult.SUCCESS;
    }
}
