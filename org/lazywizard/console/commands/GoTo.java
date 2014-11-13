package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

public class GoTo implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.COMBAT_MISSION)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        if ("home".equalsIgnoreCase(args))
        {
            return (new Home().runCommand("", context));
        }

        SectorEntityToken token = Global.getSector().getCurrentLocation().getEntityByName(args);
        if (token == null)
        {
            token = Global.getSector().getCurrentLocation().getEntityById(args);
        }

        if (token == null)
        {
            Console.showMessage("Couldn't find a token by the name '" + args + "'!");
            return CommandResult.ERROR;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (token.equals(playerFleet))
        {
            Console.showMessage("You successfully traveled nowhere.");
            return CommandResult.SUCCESS;
        }

        Vector2f loc = token.getLocation();
        playerFleet.setLocation(loc.x, loc.y);
        playerFleet.clearAssignments();
        playerFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, token, 1f);
        Console.showMessage("Teleported to " + token.getFullName() + ".");
        return CommandResult.SUCCESS;
    }
}
