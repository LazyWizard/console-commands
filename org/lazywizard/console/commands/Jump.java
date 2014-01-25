package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class Jump implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if ("home".equalsIgnoreCase(args))
        {
            return (new Home().runCommand("", context));
        }

        if (args.equalsIgnoreCase("hyperspace"))
        {
            Console.showMessage("Jumping to hyperspace isn't supported yet!");
            return CommandResult.ERROR;
            // TODO: Jump to hyperspace anchor
            //destLocation = Global.getSector().getHyperspace();
            //destination = destLocation.
        }

        StarSystemAPI system = Global.getSector().getStarSystem(args);
        if (system == null)
        {
            Console.showMessage("No system found with the name '" + args + "'!");
            return CommandResult.ERROR;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (system == playerFleet.getContainingLocation())
        {
            Console.showMessage("You successfully traveled nowhere.");
            return CommandResult.SUCCESS;
        }

        SectorEntityToken destination = system.getStar();
        if (destination == null)
        {
            destination = system.createToken(0f, 0f);
        }

        Global.getSector().doHyperspaceTransition(playerFleet, playerFleet,
                new JumpPointAPI.JumpDestination(destination, "Jumping to " + args));
        playerFleet.setNoEngaging(2.0f);
        playerFleet.clearAssignments();
        playerFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, destination, 1f);
        Console.showMessage("Jumped to " + args + ".");
        return CommandResult.SUCCESS;
    }
}
