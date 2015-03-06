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

        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI target = engine.getPlayerShip().getShipTarget();

        if (target == null)
        {
            Console.showMessage("No target found!");
            return CommandResult.ERROR;
        }

        // Switch sides
        target.setOwner(target.getOwner() == 0 ? 1 : 0);
        target.getShipAI().forceCircumstanceEvaluation();
        Console.showMessage(target.getVariant().getFullDesignationWithHullName()
                + " is now fighting for side " + FleetSide.values()[target.getOwner()] + ".");
        return CommandResult.SUCCESS;
    }
}
