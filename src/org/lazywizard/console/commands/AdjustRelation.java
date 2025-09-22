package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import org.lazywizard.console.*;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.lazylib.CollectionUtils;

public class AdjustRelation implements BaseCommandWithSuggestion
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

        // Done through the fleet to support mods that allow the player to join a faction
        final String playerFaction = Global.getSector().getPlayerFleet().getFaction().getId();
        String[] tmp = args.split(" ");

        if (tmp.length == 2)
        {
            return runCommand(tmp[0] + " " + playerFaction + " " + tmp[1], context);
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
            final List<String> ids = new ArrayList<>();
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
                    + CommandUtils.getFactionName(towardsFaction) + " (ID: "
                    + towardsFaction.getId() + ") by " + newRelationship + ".");
            return CommandResult.SUCCESS;
        }

        FactionAPI faction = CommandUtils.findBestFactionMatch(factionId);
        if (faction == null)
        {
            Console.showMessage("Error: no such faction '" + factionId + "'!");
            return CommandResult.ERROR;
        }

        if (towardsFaction == faction)
        {
            Console.showMessage("You can't change how a faction views itself!");
            return CommandResult.ERROR;
        }

        faction.adjustRelationship(towardsFaction.getId(), newRelationship / 100f);
        Console.showMessage("Adjusted relationship of " + CommandUtils.getFactionName(faction)
                + " (ID: " + faction.getId() + ") towards "
                + CommandUtils.getFactionName(towardsFaction) + " (ID: "
                + towardsFaction.getId() + ") by " + newRelationship + ".");
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        List<String> suggestions = new ArrayList<>();

        if (parameter == 0) {
            suggestions.add("All");
            suggestions.addAll(Global.getSettings().getAllFactionSpecs().stream().map(it -> it.getId()).toList());
        } else if (parameter == 1) {
            suggestions.addAll(Global.getSettings().getAllFactionSpecs().stream().map(it -> it.getId()).toList());
        }

        return suggestions;
    }
}
