package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

public class AdjustRelationship implements BaseCommand
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
            Console.showMessage("Error: relationship change must be a number!");
            return CommandResult.BAD_SYNTAX;
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
            final List ids = new ArrayList<>();
            for (FactionAPI faction : Global.getSector().getAllFactions())
            {
                ids.add(faction.getId());
            }

            Console.showMessage("Error: no such faction '" + towardsFactionId
                    + "'! Valid factions: " + CollectionUtils.implode(ids) + ".");
            return CommandResult.ERROR;
        }

        if ("all".equalsIgnoreCase(factionId))
        {
            int totalFactions = 0;
            for (FactionAPI faction : Global.getSector().getAllFactions())
            {
                if (faction != towardsFaction)
                {
                    faction.adjustRelationship(towardsFaction.getId(), newRelationship / 100f);
                    totalFactions++;
                }
            }

            Console.showMessage("Adjusted relationship of " + totalFactions + " factions towards "
                    + CommandUtils.getFactionName(towardsFaction) + " by " + newRelationship + ".");
            return CommandResult.SUCCESS;
        }

        FactionAPI faction = CommandUtils.findBestFactionMatch(factionId);
        if (faction == null)
        {
            Console.showMessage("Error: no such faction '" + factionId + "'!");
            return CommandResult.ERROR;
        }

        faction.adjustRelationship(towardsFaction.getId(), newRelationship / 100f);
        Console.showMessage("Adjusted relationship of "
                + CommandUtils.getFactionName(faction) + " towards "
                + CommandUtils.getFactionName(towardsFaction) + " by " + newRelationship + ".");
        return CommandResult.SUCCESS;
    }
}
