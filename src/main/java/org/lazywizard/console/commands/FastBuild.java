package org.lazywizard.console.commands;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class FastBuild implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInMarket())
        {
            Console.showMessage(CommonStrings.ERROR_MARKET_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final MarketAPI market = context.getMarket();
        final List<Industry> toBuild = new ArrayList<>(), toUpgrade = new ArrayList<>();
        for (Industry industry : market.getIndustries())
        {
            // Ignore population industry
            if ("population".equals(industry.getId())) continue;

            if (industry.isUpgrading())
            {
                toUpgrade.add(industry);
            }
            else if (industry.isBuilding())
            {
                toBuild.add(industry);
            }
        }

        if (toBuild.isEmpty() && toUpgrade.isEmpty())
        {
            Console.showMessage("No industries are under construction at this market.");
            return CommandResult.SUCCESS;
        }

        if (!toBuild.isEmpty())
        {
            final List<String> built = new ArrayList<>();
            for (Industry industry : toBuild)
            {
                built.add(industry.getId() + " (" + industry.getCurrentName() + ")");
                industry.finishBuildingOrUpgrading();
            }

            Console.showIndentedMessage("Completed construction of " + toBuild.size() + " industries:",
                    CollectionUtils.implode(built, "\n"), 3);
        }

        if (!toUpgrade.isEmpty())
        {
            final List<String> upgraded = new ArrayList<>();
            for (Industry industry : toUpgrade)
            {
                upgraded.add(industry.getId() + " (" + industry.getCurrentName() + ")");
                industry.finishBuildingOrUpgrading();
            }

            Console.showIndentedMessage("Completed upgrading of " + toUpgrade.size() + " industries:",
                    CollectionUtils.implode(upgraded, "\n"), 3);
        }

        Console.showMessage("Total industries affected: " + (toBuild.size() + toUpgrade.size()) + ".");
        return CommandResult.SUCCESS;
    }
}
