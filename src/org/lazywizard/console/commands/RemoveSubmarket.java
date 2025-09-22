package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

public class RemoveSubmarket implements BaseCommandWithSuggestion {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInMarket()) {
            Console.showMessage(CommonStrings.ERROR_MARKET_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final MarketAPI market = context.getMarket();
        if (args.isEmpty()) {
            final List<String> submarkets = new ArrayList<>();
            for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
                submarkets.add(submarket.getSpecId());
            }

            Collections.sort(submarkets, String.CASE_INSENSITIVE_ORDER);
            Console.showMessage("Existing submarkets of current market: " + CollectionUtils.implode(submarkets) + ".");
            return CommandResult.SUCCESS;
        }

        final String id = args;
        if (!market.hasSubmarket(id)) {
            Console.showMessage("Market '" + market.getName() + "' does not have submarket '" + id + "'!");
            return CommandResult.ERROR;
        }

        market.removeSubmarket(id);
        Console.showMessage("Removed submarket '" + id + "' from market '" + market.getName() + "'.");
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        if (parameter != 0) return new ArrayList<>();
        return Global.getSettings().getAllSubmarketSpecs().stream().map(it -> it.getId()).toList();
    }
}
