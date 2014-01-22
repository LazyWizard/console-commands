package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddCrew implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.COMBAT_MISSION)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        args = args.toLowerCase();

        String[] tmp = args.split(" ");

        if (tmp.length == 1)
        {
            return runCommand(args + " regular", context);
        }

        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        int amt;
        try
        {
            amt = Integer.parseInt(tmp[0]);
        }
        catch (NumberFormatException ex)
        {
            // Support for reversed arguments
            try
            {
                amt = Integer.parseInt(tmp[1]);
                tmp[1] = tmp[0];
            }
            catch (NumberFormatException ex2)
            {
                return CommandResult.BAD_SYNTAX;
            }
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
            return CommandResult.BAD_SYNTAX;
        }

        if (amt >= 0)
        {
            Global.getSector().getPlayerFleet().getCargo().addCrew(level, amt);
            Console.showMessage("Added " + amt + " " + level.getPrefix()
                    + " crew to player fleet.");
        }
        else
        {
            Global.getSector().getPlayerFleet().getCargo().removeCrew(level, -amt);
            Console.showMessage("Removed " + -amt + " " + level.getPrefix()
                    + " crew from player fleet.");
        }

        return CommandResult.SUCCESS;
    }
}
