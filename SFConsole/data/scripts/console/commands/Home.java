package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.scripts.console.BaseCommand;
import org.lwjgl.util.vector.Vector2f;

public class Home extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Teleports the player to their home location.\n"
                + "Home location is set with the 'sethome' command.";
    }

    @Override
    protected String getSyntax()
    {
        return "home (no arguments)";
    }

    @Override
    public boolean runCommand(String args)
    {
        SectorEntityToken home = getVar("Home", SectorEntityToken.class);

        if (home == null)
        {
            showMessage("No home found! Use SetHome first!");
            return false;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

        Vector2f loc = home.getLocation();
        playerFleet.setLocation(loc.x, loc.y);
        playerFleet.clearAssignments();
        playerFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, home, 1);
        showMessage("Teleported to " + home.getFullName() + " successfully.");
        return true;
    }
}
