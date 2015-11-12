package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

public class Home implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final SectorAPI sector = Global.getSector();
        final SectorEntityToken home = (SectorEntityToken) sector.getPersistentData()
                .get(CommonStrings.DATA_HOME_ID);

        if (home == null)
        {
            Console.showMessage("No home found! Use SetHome first!");
            return CommandResult.ERROR;
        }

        final CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        final LocationAPI loc = home.getContainingLocation();
        if (loc != playerFleet.getContainingLocation())
        {
            playerFleet.getContainingLocation().removeEntity(playerFleet);
            loc.addEntity(playerFleet);
            Global.getSector().setCurrentLocation(loc);
        }

        final Vector2f homeLoc = home.getLocation();
        playerFleet.setLocation(homeLoc.x, homeLoc.y);
        playerFleet.setNoEngaging(2.0f);
        playerFleet.clearAssignments();
        playerFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, home, 1f);
        Console.showMessage("Teleported to " + home.getFullName()
                + " in " + (loc.isHyperspace() ? "hyperspace" : "the "
                        + loc.getName() + " system") + " successfully.");
        return CommandResult.SUCCESS;
    }
}
