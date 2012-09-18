package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import data.scripts.console.BaseCommand;

public class AddShip extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "";
    }

    @Override
    protected String getSyntax()
    {
        return "addship <variantID> <optionalAmount>";
    }

    @Override
    public boolean runCommand(String args)
    {
        String[] tmp = args.split(" ");

        if (tmp.length == 1)
        {
            return runCommand(args + " 1");
        }

        if (tmp.length != 2)
        {
            showSyntax();
            return false;
        }

        int amt;

        try
        {
            amt = Integer.parseInt(tmp[1]);
        }
        catch (NumberFormatException ex)
        {
            showSyntax();
            return false;
        }

        if (amt == 0)
        {
            return true;
        }

        FleetDataAPI fleet = Global.getSector().getPlayerFleet().getFleetData();
        FleetMemberAPI ship = null;

        for (int x = 0; x < amt; x++)
        {
            ship = Global.getFactory().createFleetMember(
                    FleetMemberType.SHIP, tmp[0]);
            fleet.addFleetMember(ship);
        }

        showMessage("Added " + amt + " of ship " + ship.getSpecId() + " to player fleet.");
        return true;
    }
}
