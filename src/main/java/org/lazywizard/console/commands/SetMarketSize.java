package org.lazywizard.console.commands;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.population.CoreImmigrationPluginImpl;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;

import static org.lazywizard.console.CommandUtils.isInteger;

public class SetMarketSize implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInMarket())
        {
            Console.showMessage(CommonStrings.ERROR_MARKET_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (!isInteger(args))
        {
            return CommandResult.BAD_SYNTAX;
        }

        final MarketAPI market = context.getMarket();
        final int curSize = market.getSize(), newSize = MathUtils.clamp(Integer.parseInt(args), 1, 10);
        if (curSize == newSize)
        {
            Console.showMessage("Market is already size " + newSize + ", so no changes were made.");
            return CommandResult.SUCCESS;
        }

        // The below is all copied from CoreImmigrationPluginImpl, and should be periodically checked to ensure it matches
        // Remove old population condition
        market.removeCondition("population_" + market.getSize());
        market.addCondition("population_" + newSize);

        // Set size and adjust population growth
        market.setSize(newSize);
        market.getPopulation().setWeight(Misc.getImmigrationPlugin(market).getWeightForMarketSize(market.getSize()));
        market.getPopulation().normalize();

        market.reapplyConditions();
        market.reapplyIndustries();
        Console.showMessage("Market size changed to " + market.getSize() + " (was " + curSize + ").");
        return CommandResult.SUCCESS;
    }
}
