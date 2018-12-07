package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.lazywizard.console.CommandUtils.findBestIndustryMatch;

public class AddIndustry implements BaseCommand
{
    public static List<IndustrySpecAPI> getUpgrades(IndustrySpecAPI spec)
    {
        final List<IndustrySpecAPI> upgrades = new ArrayList<>();
        while (spec.getUpgrade() != null)
        {
            final IndustrySpecAPI newSpec = Global.getSettings().getIndustrySpec(spec.getUpgrade());
            upgrades.add(newSpec);
            spec = newSpec;
        }

        return upgrades;
    }

    public static List<IndustrySpecAPI> getDowngrades(IndustrySpecAPI spec)
    {
        final List<IndustrySpecAPI> downgrades = new ArrayList<>();
        while (spec.getDowngrade() != null)
        {
            final IndustrySpecAPI newSpec = Global.getSettings().getIndustrySpec(spec.getDowngrade());
            downgrades.add(newSpec);
            spec = newSpec;
        }

        return downgrades;
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInMarket())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final MarketAPI market = context.getMarket();
        if (args.isEmpty())
        {
            final List<String> industries = new ArrayList<>();
            for (Industry industry : market.getIndustries())
            {
                industries.add(industry.getId());
            }

            Collections.sort(industries);
            Console.showMessage("Existing industries of current market: " + CollectionUtils.implode(industries) + ".");
            return CommandResult.SUCCESS;
        }

        final String[] tmp = args.split(" ", 2);
        final IndustrySpecAPI spec = findBestIndustryMatch(tmp[0]);
        if (spec == null)
        {
            Console.showMessage("No industry found with id '" + args + "'! Use 'list industries' for a list of valid IDs.");
            return CommandResult.ERROR;
        }

        final String id = spec.getId();
        if (market.hasIndustry(id))
        {
            Console.showMessage("Industry '" + id + "' already exists in market '" + market.getName()
                    + "'. Use RemoveIndustry if you want to remove it.");
            return CommandResult.ERROR;
        }

        // Auto-remove any existing upgrades/downgrades of this industry (must calculate them ourselves)
        // TODO: Make sure to call upgrade methods (ensures fleet passing for stations, etc)
        Industry existing = null;
        for (IndustrySpecAPI upgrade : getUpgrades(spec))
        {
            if (market.hasIndustry(upgrade.getId()))
            {
                existing = market.getIndustry(upgrade.getId());
                //upgrade.getUpgradePluginInstance(market).notifyBeingRemoved(null, true);
                market.removeIndustry(upgrade.getId(), null, true);
                Console.showMessage("Removed existing industry '" + upgrade.getId()
                        + "' of same upgrade path from market '" + market.getName() + "'.");
            }
        }
        if (existing != null)
        {
            for (IndustrySpecAPI downgrade : getDowngrades(spec))
            {
                if (market.hasIndustry(downgrade.getId()))
                {
                    existing = market.getIndustry(downgrade.getId());
                    //downgrade.getDowngradePluginInstance(market).notifyBeingRemoved(null, true);
                    market.removeIndustry(downgrade.getId(), null, true);
                    Console.showMessage("Removed existing industry '" + downgrade.getId()
                            + "' of same upgrade path from market '" + market.getName() + "'.");
                }
            }
        }

        // TODO: Remove after 0.9.1a (provided hardcoded limitation is lifted)
        if (market.getIndustries().size() >= 12)
        {
            Console.showMessage("Warning: market '" + market.getName() + "' already has the maximum number of industries!\n" +
                    "Consider using RemoveIndustry to prune the list, or you won't be able to manage all of them using the UI.");
        }

        if (tmp.length > 1)
        {
            final List<String> params = Arrays.asList(tmp[1].split(" "));
            market.addIndustry(id, params);
            market.reapplyIndustries();
            Console.showMessage("Added industry '" + id + "' to market '" + market.getName() + "' with params " +
                    CollectionUtils.implode(params) + ".");
            return CommandResult.SUCCESS;
        }
        else
        {
            market.addIndustry(id);
            market.reapplyIndustries();
            Console.showMessage("Added industry '" + id + "' to market '" + market.getName() + "'.");
            return CommandResult.SUCCESS;
        }
    }
}
