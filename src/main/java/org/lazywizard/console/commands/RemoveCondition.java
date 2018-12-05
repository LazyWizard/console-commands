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

public class RemoveCondition implements BaseCommand
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

        final String id = spec.getId();
        if (!market.hasCondition(id))
        {
            Console.showMessage("Market '" + market.getName() + "' does not have condition '" + id + "'!");
            return CommandResult.ERROR;
        }

        market.removeCondition(id);
        market.reapplyConditions();
        Console.showMessage("Removed condition '" + id + "' from market '" + market.getName() + "'.");
        return CommandResult.SUCCESS;
    }
}
