package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.loading.IndustrySpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.lazywizard.console.CommandUtils.findBestIndustryMatch;
import static org.lazywizard.console.CommandUtils.findBestMarketConditionMatch;

public class RemoveIndustry implements BaseCommandWithSuggestion
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
        if (args.isEmpty())
        {
            final List<String> industries = new ArrayList<>();
            for (Industry industry : market.getIndustries())
            {
                industries.add(industry.getId());
            }

            Collections.sort(industries, String.CASE_INSENSITIVE_ORDER);
            Console.showMessage("Existing industries of current market: " + CollectionUtils.implode(industries) + ".");
            return CommandResult.SUCCESS;
        }

        final IndustrySpecAPI spec = findBestIndustryMatch(args);
        if (spec == null)
        {
            Console.showMessage("No industry found with id '" + args + "'! Use 'list industries' for a list of valid IDs.");
            return CommandResult.ERROR;
        }

        final String id = spec.getId();
        if (!market.hasIndustry(id))
        {
            Console.showMessage("Market '" + market.getName() + "' does not have industry '" + id + "'!");
            return CommandResult.ERROR;
        }

        // Don't allow player to remove population industry
        if ("population".equals(id))
        {
            Console.showMessage("You can't remove your population industry!");
            return CommandResult.ERROR;
        }

        market.removeIndustry(id, null, false);
        market.reapplyIndustries();
        Console.showMessage("Removed industry '" + id + "' from market '" + market.getName() + "'.");
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        if (parameter != 0) return new ArrayList<>();
        return Global.getSettings().getAllIndustrySpecs().stream().map(it -> it.getId()).toList();
    }
}
