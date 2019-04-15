package org.lazywizard.console.commands;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.findBestStringMatch;

public class AddSubmarket implements BaseCommand
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
        final String submarketId = findBestStringMatch(args, List_.getSubmarketIds());
        if (submarketId == null)
        {
            Console.showMessage("No submarket found with id '" + args
                    + "'! Use 'list submarkets' for a complete list of valid ids.");
            return CommandResult.ERROR;
        }

        if (market.hasSubmarket(submarketId))
        {
            Console.showMessage("This market already has the '" + submarketId + "' submarket! Use 'RemoveSubmarket "
                    + submarketId + "' to remove it.");
            return CommandResult.ERROR;
        }

        market.addSubmarket(submarketId);
        Console.showMessage("Added '" + submarketId + "' submarket to market.");
        return CommandResult.SUCCESS;
    }
}
