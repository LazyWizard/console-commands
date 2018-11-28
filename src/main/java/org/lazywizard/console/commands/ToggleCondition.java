package org.lazywizard.console.commands;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.lazywizard.console.CommandUtils.findBestMarketConditionMatch;

public class ToggleCondition implements BaseCommand
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
            final List<String> conditions = new ArrayList<>();
            for (MarketConditionAPI condition : market.getConditions())
            {
                conditions.add(condition.getId());
            }

            Collections.sort(conditions);
            Console.showMessage("Conditions of current market: " + CollectionUtils.implode(conditions) + ".");
            return CommandResult.SUCCESS;
        }

        final MarketConditionSpecAPI spec = findBestMarketConditionMatch(args);
        if (spec == null)
        {
            Console.showMessage("No market condition found with id '" + args + "'! Use 'list conditions' for a list of valid IDs.");
            return CommandResult.ERROR;
        }

        final String condition = spec.getId();
        if (market.hasCondition(condition))
        {
            market.removeCondition(condition);
            Console.showMessage("Removed condition '" + condition + "' from market '" + market.getName() + "'.");
            return CommandResult.SUCCESS;
        }

        // FIXME: new conditions not showing up (need to check vanilla setup files)
        market.addCondition(condition);
        Console.showMessage("Added condition '" + condition + "' to market '" + market.getName() + "'.");
        return CommandResult.SUCCESS;
    }
}
