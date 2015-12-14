package org.lazywizard.console.commands;

import java.lang.reflect.Field;
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

        final Field sinceLastCargoUpdate, minCargoUpdateInterval;
        try
        {
            sinceLastCargoUpdate = BaseSubmarketPlugin.class.getDeclaredField(
                    "sinceLastCargoUpdate");
            sinceLastCargoUpdate.setAccessible(true);
            minCargoUpdateInterval = BaseSubmarketPlugin.class.getDeclaredField(
                    "minCargoUpdateInterval");
            minCargoUpdateInterval.setAccessible(true);
        }
        catch (Exception ex)
        {
            Console.showException("Failed to access required fields! Has BaseSubmarketPlugin been modified?", ex);
            return CommandResult.ERROR;
        }

        int totalMarkets = 0, totalSubmarkets = 0;
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

                if (submarket.getPlugin() instanceof BaseSubmarketPlugin)
                {
                    final BaseSubmarketPlugin plugin = (BaseSubmarketPlugin) submarket.getPlugin();
                    totalSubmarkets++;

                    try
                    {
                        sinceLastCargoUpdate.setFloat(plugin,
                                minCargoUpdateInterval.getFloat(plugin) + 1);
                    }
                    catch (Exception ex)
                    {
                        totalSubmarkets--;
                        continue;
                    }

                    plugin.updateCargoPrePlayerInteraction();
                }
            }
        }

        Console.showMessage("Updated inventory for " + totalSubmarkets
                + " submarkets in " + totalMarkets + " markets.");
        return CommandResult.SUCCESS;
    }
}
