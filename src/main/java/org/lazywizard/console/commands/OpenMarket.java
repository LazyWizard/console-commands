package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class OpenMarket implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final MarketAPI market = CommandUtils.findBestMarketMatch(args);
        if (market == null)
        {
            final List<String> markets = new ArrayList<>();
            for (MarketAPI tmp : Global.getSector().getEconomy().getMarketsCopy())
            {
                markets.add(tmp.getId() + " (" + tmp.getName() + ")");
            }

            Console.showMessage("No such market '" + args + "'!\nValid markets: "
                    + CollectionUtils.implode(markets));
            return CommandResult.ERROR;
        }

        if (market.getPrimaryEntity() == null)
        {
            Console.showMessage("No interactable campaign entity found for market '" + args + "'.");
            return CommandResult.ERROR;
        }

        Console.showDialogOnClose(market.getPrimaryEntity());
        Console.showMessage("The market dialog will be shown when you next unpause on the campaign map.");
        return CommandResult.SUCCESS;
    }
}
