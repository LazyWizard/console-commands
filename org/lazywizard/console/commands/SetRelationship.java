package org.lazywizard.console.commands;

import com.fs.starfarer.api.campaign.FactionAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class SetRelationship implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
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

        String faction = tmp[0];
        String towardsFaction = tmp[1];

        float newRelationship;
        try
        {
            newRelationship = Float.parseFloat(tmp[2]);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: relationship amount must be a number!");
            return CommandResult.BAD_SYNTAX;
        }

        FactionAPI fac1 = CommandUtils.findBestFactionMatch(faction);
        FactionAPI fac2 = CommandUtils.findBestFactionMatch(towardsFaction);

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

        fac1.setRelationship(towardsFaction, newRelationship / 100f);
        Console.showMessage("Set relationship of "
                + CommandUtils.getFactionName(fac1) + " towards "
                + CommandUtils.getFactionName(fac2) + " to " + newRelationship + ".");
        return CommandResult.SUCCESS;
    }
}
