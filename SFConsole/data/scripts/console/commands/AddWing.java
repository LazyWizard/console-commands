package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import data.scripts.console.BaseCommand;

public class AddWing extends BaseCommand
{
    @Override
    protected String getName()
    {
        return "AddWing";
    }

    @Override
    protected String getHelp()
    {
        return "";
    }

    @Override
    protected String getSyntax()
    {
        return "addwing <variantID> <optionalAmount>";
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

        if (!tmp[0].endsWith("_wing"))
        {
            tmp[0] = tmp[0] + "_wing";
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
                    FleetMemberType.FIGHTER_WING, tmp[0]);
            fleet.addFleetMember(ship);
        }

        showMessage("Added " + amt + " of wing " + ship.getSpecId() + " to player fleet.");
        return true;
    }
}
