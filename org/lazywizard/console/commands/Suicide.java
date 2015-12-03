package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class Suicide implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        // Used in campaign: destroy entire player fleet
        if (context.isInCampaign())
        {
            final CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            for (FleetMemberAPI member : player.getFleetData().getMembersListCopy())
            {
                player.removeFleetMemberWithDestructionFlash(member);
            }

            Console.showMessage("Destroyed player fleet.");
            return CommandResult.SUCCESS;
        }

        // Used in combat: kill flagship only
        final ShipAPI player = Global.getCombatEngine().getPlayerShip();
        Kill.killShip(player, true);
        Console.showMessage("Destroyed currently piloted ship.");
        return CommandResult.SUCCESS;
    }
}
