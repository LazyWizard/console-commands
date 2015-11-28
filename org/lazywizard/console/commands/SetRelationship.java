package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
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

        final String factionId = tmp[0];
        final String towardsFactionId = tmp[1];
        if ("all".equalsIgnoreCase(towardsFactionId) && !"all".equalsIgnoreCase(factionId))
        {
            return runCommand(tmp[1] + " " + tmp[0] + " " + tmp[2], context);
        }

        final FactionAPI towardsFaction = CommandUtils.findBestFactionMatch(towardsFactionId);
        if (towardsFaction == null)
        {
            Console.showMessage("Error: no such faction '" + towardsFactionId + "'!");
            return CommandResult.ERROR;
        }

        if ("all".equalsIgnoreCase(factionId))
        {
            int totalFactions = 0;
            for (FactionAPI faction : Global.getSector().getAllFactions())
            {
                if (faction != towardsFaction)
                {
                    faction.setRelationship(towardsFaction.getId(), newRelationship / 100f);
                    totalFactions++;
                }
            }

            Console.showMessage("Set relationship of " + totalFactions + " factions towards "
                    + CommandUtils.getFactionName(towardsFaction) + " to " + newRelationship + ".");
            return CommandResult.SUCCESS;
        }

        FactionAPI faction = CommandUtils.findBestFactionMatch(factionId);
        if (faction == null)
        {
            Console.showMessage("Error: no such faction '" + factionId + "'!");
            return CommandResult.ERROR;
        }

        faction.setRelationship(towardsFaction.getId(), newRelationship / 100f);
        Console.showMessage("Set relationship of "
                + CommandUtils.getFactionName(faction) + " towards "
                + CommandUtils.getFactionName(towardsFaction) + " to " + newRelationship + ".");
        return CommandResult.SUCCESS;
    }
}
