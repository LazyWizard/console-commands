package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class ForceMarketUpdate implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        int totalMarkets = 0, totalSubmarkets = 0, failedSubmarkets = 0;
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            totalMarkets++;
            for (SubmarketAPI submarket : market.getSubmarketsCopy())
            {
                // Ignore storage tabs
                if (Submarkets.SUBMARKET_STORAGE.equals(submarket.getSpec().getId()))
                {
                    continue;
                }

                // Only update submarkets that implement BaseSubmarketPlugin (guaranteed to have the proper fields)
                if (submarket.getPlugin() instanceof BaseSubmarketPlugin)
                {
                    try
                    {
                        final BaseSubmarketPlugin plugin = (BaseSubmarketPlugin) submarket.getPlugin();
                        plugin.setSinceSWUpdate(plugin.getMinSWUpdateInterval() + 1f);
                        plugin.setSinceLastCargoUpdate(plugin.getMinSWUpdateInterval() + 1f);
                        plugin.updateCargoPrePlayerInteraction();
                        plugin.setSinceSWUpdate(0f);
                        plugin.setSinceLastCargoUpdate(0f);
                        totalSubmarkets++;
                    }
                    catch (Exception ex)
                    {
                        Console.showException("Failed to update submarket '" + submarket.getName() + "' (" +
                                submarket.getSpecId() + ") in market '" + market.getName() + "' (" +
                                market.getId() + "): ", ex);
                        failedSubmarkets++;
                    }
                }
            }
        }

        Console.showMessage("Updated inventory for " + totalSubmarkets
                + " submarkets in " + totalMarkets + " markets." + (failedSubmarkets > 0 ?
                " " + failedSubmarkets + " submarkets failed to update." : ""));
        return CommandResult.SUCCESS;
    }
}
