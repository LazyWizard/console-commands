package org.lazywizard.console.commands;

import java.util.HashMap;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class FindShip implements BaseCommand
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

        if (args.endsWith("_Hull"))
        {
            args = args.substring(0, args.lastIndexOf("_Hull"));
        }
        else if (args.endsWith("_wing"))
        {
            args = args.substring(0, args.lastIndexOf("_wing"));
        }

        Map<SubmarketAPI, Integer> found = new HashMap<>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            for (SubmarketAPI submarket : market.getSubmarketsCopy())
            {
                int total = 0;
                for (FleetMemberAPI member : submarket.getCargo().getMothballedShips().getMembersListCopy())
                {
                    if (member.getHullId().equalsIgnoreCase(args))
                    {
                        total++;
                    }
                }

                if (total > 0)
                {
                    found.put(submarket, total);
                }
            }
        }

        if (found.isEmpty())
        {
            Console.showMessage("No ships with id '" + args + "' found!");
            return CommandResult.SUCCESS;
        }

        Console.showMessage("Found " + found.size() + " markets with "
                + Character.toUpperCase(args.charAt(0)) + args.substring(1) + "s for sale:");
        for (Map.Entry<SubmarketAPI, Integer> entry : found.entrySet())
        {
            SubmarketAPI submarket = entry.getKey();
            Console.showMessage(" - " + entry.getValue() + " for sale in "
                    + submarket.getMarket().getName() + "'s "
                    + submarket.getNameOneLine() + " submarket ("
                    + submarket.getFaction().getDisplayName() + ", "
                    + submarket.getMarket().getPrimaryEntity()
                    .getContainingLocation().getName() + ")");
        }
        return CommandResult.SUCCESS;
    }
}
