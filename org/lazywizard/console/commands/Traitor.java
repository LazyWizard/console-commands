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
    private static void turnTraitorInternal(ShipAPI ship, int newOwner)
    {
        // Switch to the opposite side
        ship.setOwner(newOwner);
        ship.getShipAI().forceCircumstanceEvaluation();

        // Also switch sides of any drones (doesn't affect any new ones)
        if (ship.getDeployedDrones() != null)
        {
            for (ShipAPI drone : ship.getDeployedDrones())
            {
                drone.setOwner(newOwner);
                drone.getShipAI().forceCircumstanceEvaluation();
            }
        }
    }

    public static void turnTraitor(ShipAPI ship)
    {
        // Switch squadmates if this is a fighter wing
        final int newOwner = (ship.getOwner() == 0 ? 1 : 0);
        if (ship.isFighter())
        {
            for (ShipAPI member : ship.getWing().getWingMembers())
            {
                turnTraitorInternal(member, newOwner);
            }
        }
        else
        {
            turnTraitorInternal(ship, newOwner);
        }
    }

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

        if (target == null)
        {
            Console.showMessage("No target found!");
            return CommandResult.ERROR;
        }

        turnTraitor(target);
        Console.showMessage(target.getVariant().getFullDesignationWithHullName()
                + " is now fighting for side " + FleetSide.values()[target.getOwner()] + ".");
        return CommandResult.SUCCESS;
    }
}
