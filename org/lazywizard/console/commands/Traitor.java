package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Traitor implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final CombatEngineAPI engine = Global.getCombatEngine();
        final ShipAPI target = engine.getPlayerShip().getShipTarget();
        final int newOwner = (target.getOwner() == 0 ? 1 : 0);

        if (target == null)
        {
            Console.showMessage("No target found!");
            return CommandResult.ERROR;
        }

        // Switch sides
        target.setOwner(newOwner);
        target.getShipAI().forceCircumstanceEvaluation();

        // Also switch sides of any drones (doesn't affect any new ones)
        if (target.getDeployedDrones() != null)
        {
            for (ShipAPI drone : target.getDeployedDrones())
            {
                drone.setOwner(newOwner);
                drone.getShipAI().forceCircumstanceEvaluation();
            }
        }

        Console.showMessage(target.getVariant().getFullDesignationWithHullName()
                + " is now fighting for side " + FleetSide.values()[target.getOwner()] + ".");
        return CommandResult.SUCCESS;
    }
}
