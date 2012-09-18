package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
import data.scripts.console.BaseCommand;

public class AddCrew extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Adds the specified amount of crew.\n"
                + "You can optionally choose an experience level for the spawned crew.\n"
                + "Valid levels: green, regular, veteran, and elite";
    }

    @Override
    protected String getSyntax()
    {
        return "addcrew <amount> <optionalLevel>";
    }

    @Override
    public boolean runCommand(String args)
    {
        args = args.toLowerCase();

        String[] tmp = args.split(" ");

        if (tmp.length == 1)
        {
            return runCommand(args + " regular");
        }

        if (tmp.length != 2)
        {
            showSyntax();
            return false;
        }

        int amt;

        try
        {
            amt = Integer.parseInt(tmp[0]);
        }
        catch (NumberFormatException ex)
        {
            showSyntax();
            return false;
        }

        CrewXPLevel level;

        if (tmp[1].equals("green"))
        {
            level = CrewXPLevel.GREEN;
        }
        else if (tmp[1].equals("regular"))
        {
            level = CrewXPLevel.REGULAR;
        }
        else if (tmp[1].equals("veteran"))
        {
            level = CrewXPLevel.VETERAN;
        }
        else if (tmp[1].equals("elite"))
        {
            level = CrewXPLevel.ELITE;
        }
        else
        {
            showSyntax();
            return false;
        }

        if (amt >= 0)
        {
            Global.getSector().getPlayerFleet().getCargo().addCrew(level, amt);
            showMessage("Added " + amt + " " + tmp[1] + " crew to player fleet.");
        }
        else
        {
            Global.getSector().getPlayerFleet().getCargo().removeCrew(level, -amt);
            showMessage("Removed " + -amt + " " + tmp[1] + " crew from player fleet.");
        }

        return true;
    }
}
