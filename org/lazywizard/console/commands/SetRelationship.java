package org.lazywizard.console.commands;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
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

        if (tmp.length == 2)
        {
            return runCommand(tmp[0] + " player " + tmp[1], context);
        }

        if (tmp.length != 3)
        {
            return CommandResult.BAD_SYNTAX;
        }

        final String faction = tmp[0];
        final String towardsFaction = tmp[1];

        float newRelationship;
        try
        {
            newRelationship = Float.parseFloat(tmp[2]);
        }
        catch (NumberFormatException ex)
        {
            try
            {
                final RepLevel level = RepLevel.valueOf(tmp[2].toUpperCase());
                newRelationship = 1 + (level.getMin() * 100f);
                if (level.isNegative())
                {
                    newRelationship *= -1f;
                }
            }
            catch (IllegalArgumentException ex2)
            {
                Console.showMessage("Error: relationship amount must be a number or RepLevel!");
                return CommandResult.BAD_SYNTAX;
            }
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

        fac1.setRelationship(fac2.getId(), newRelationship / 100f);
        Console.showMessage("Set relationship of "
                + CommandUtils.getFactionName(fac1) + " towards "
                + CommandUtils.getFactionName(fac2) + " to " + newRelationship + ".");
        return CommandResult.SUCCESS;
    }
}
