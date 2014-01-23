package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

public class Home implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.COMBAT_MISSION)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        SectorAPI sector = Global.getSector();
        SectorEntityToken home = (SectorEntityToken) sector.getPersistentData()
                .get(CommonStrings.DATA_HOME_ID);

        if (home == null)
        {
            Console.showMessage("No home found! Use SetHome first!");
            return CommandResult.ERROR;
        }

        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        LocationAPI loc = home.getContainingLocation();
        if (loc == playerFleet.getContainingLocation())
        {
            Vector2f homeLoc = home.getLocation();
            playerFleet.setLocation(homeLoc.x, homeLoc.y);
        }
        else
        {
            sector.doHyperspaceTransition(playerFleet, playerFleet,
                    new JumpDestination(home, "Teleporting home"));
        }

        playerFleet.setNoEngaging(2.0f);
        playerFleet.clearAssignments();
        playerFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, home, 1);
        Console.showMessage("Teleported to " + home.getFullName()
                + " in " + (loc.isHyperspace() ? "hyperspace" : "the "
                + ((StarSystemAPI) loc).getName() + " system") + " successfully.");
        return CommandResult.SUCCESS;
    }
}
