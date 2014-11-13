package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AdjustRelationship implements BaseCommand
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

        String[] tmp = args.split(" ");

        if (tmp.length != 3)
        {
            return CommandResult.BAD_SYNTAX;
        }

        float newRelationship;

        try
        {
            newRelationship = Float.parseFloat(tmp[2]);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: relationship change must be a number!");
            return CommandResult.BAD_SYNTAX;
        }

        String faction = tmp[0];
        String towardsFaction = tmp[1];
        FactionAPI fac1 = Global.getSector().getFaction(faction);
        FactionAPI fac2 = Global.getSector().getFaction(towardsFaction);

        if (fac1 == null)
        {
            Console.showMessage("Error: no such faction '" + faction + "'!");
            return CommandResult.ERROR;
        }
        if (fac2 == null)
        {
            Console.showMessage("Error: no such faction '" + towardsFaction + "'!");
            return CommandResult.ERROR;
        }

        fac1.adjustRelationship(towardsFaction, newRelationship / 100f);
        Console.showMessage("Adjusted relationship of " + faction + " towards "
                + towardsFaction + " by " + newRelationship + ".");
        return CommandResult.SUCCESS;
    }
}
