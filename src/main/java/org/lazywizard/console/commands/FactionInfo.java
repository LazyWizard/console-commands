package org.lazywizard.console.commands;

import com.fs.starfarer.api.campaign.FactionAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FactionInfo implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isCampaignAccessible())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final FactionAPI faction = CommandUtils.findBestFactionMatch(args);
        if (faction == null)
        {
            Console.showMessage("No such faction '" + args + "'! Use 'list factions' for a complete list of valid IDs.\n");
            return CommandResult.ERROR;
        }
        final StringBuilder sb = new StringBuilder(1024);
        sb.append("Faction report for faction '" + faction.getId() + "':");

        // Ships
        List<String> known = new ArrayList<>(faction.getKnownShips());
        Collections.sort(known, String.CASE_INSENSITIVE_ORDER);
        if (known.isEmpty())
        {
            sb.append("\n\nKnown ships: none\n");
        }
        else
        {
            sb.append("\n\nKnown ships:\n " + CollectionUtils.implode(known, "\n "));
        }

        // Fighters
        known = new ArrayList<>(faction.getKnownFighters());
        Collections.sort(known, String.CASE_INSENSITIVE_ORDER);
        if (known.isEmpty())
        {
            sb.append("\n\nKnown LPCs: none\n");
        }
        else
        {
            sb.append("\n\nKnown LPCs:\n " + CollectionUtils.implode(known, "\n "));
        }

        // Weapons
        known = new ArrayList<>(faction.getKnownWeapons());
        Collections.sort(known, String.CASE_INSENSITIVE_ORDER);
        if (known.isEmpty())
        {
            sb.append("\n\nKnown weapons: none\n");
        }
        else
        {
            sb.append("\n\nKnown weapons:\n " + CollectionUtils.implode(known, "\n "));
        }

        // Hullmods
        known = new ArrayList<>(faction.getKnownHullMods());
        Collections.sort(known, String.CASE_INSENSITIVE_ORDER);
        if (known.isEmpty())
        {
            sb.append("\n\nKnown hullmods: none\n");
        }
        else
        {
            sb.append("\n\nKnown hullmods:\n " + CollectionUtils.implode(known, "\n "));
        }

        // Industries
        known = new ArrayList<>(faction.getKnownIndustries());
        Collections.sort(known, String.CASE_INSENSITIVE_ORDER);
        if (known.isEmpty())
        {
            sb.append("\n\nKnown industries: none\n");
        }
        else
        {
            sb.append("\n\nKnown industries:\n " + CollectionUtils.implode(known, "\n "));
        }

        Console.showMessage(sb.toString());
        return CommandResult.SUCCESS;
    }
}
