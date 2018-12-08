package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.deciv.DecivTracker;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class DestroyColony implements BaseCommand
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
        DecivTracker.decivilize(market, true);
        MarketCMD.addBombardVisual(market.getPrimaryEntity());
        Global.getSector().getCampaignUI().getCurrentInteractionDialog().dismiss();
        Console.showMessage("Colony " + market.getName() + " destroyed. You monster.");
        return CommandResult.SUCCESS;
    }
}
