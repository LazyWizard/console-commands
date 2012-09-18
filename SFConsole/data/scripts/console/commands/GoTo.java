package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import data.scripts.console.BaseCommand;
import org.lwjgl.util.vector.Vector2f;

public class GoTo extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "";
    }

    @Override
    protected String getSyntax()
    {
        return "goto <locationName>";
    }

    @Override
    public boolean runCommand(String args)
    {
        if ("home".equals(args))
        {
            return (new Home().runCommand(null));
        }

        SectorEntityToken token = getStarSystem().getEntityByName(args);

        if (token == null)
        {
            showMessage("Couldn't find a token by the name '"+args+"'!");
            return false;
        }

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();

        if (token.equals(playerFleet))
        {
            showMessage("You successfully traveled nowhere.");
            return true;
        }

        Vector2f loc = token.getLocation();
        playerFleet.setLocation(loc.x, loc.y);
        playerFleet.clearAssignments();
        playerFleet.addAssignment(FleetAssignment.GO_TO_LOCATION, token, 1);
        showMessage("Teleported to "+args+".");
        return true;
    }
}
