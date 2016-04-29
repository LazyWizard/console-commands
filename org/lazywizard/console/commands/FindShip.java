package org.lazywizard.console.commands;

import java.util.HashMap;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SubmarketPlugin.TransferAction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class FindShip implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        if (args.endsWith("_Hull"))
        {
            args = args.substring(0, args.lastIndexOf("_Hull"));
        }
        else if (args.endsWith("_wing"))
        {
            args = args.substring(0, args.lastIndexOf("_wing"));
        }

        float shipPriceMod = Global.getSettings().getFloat("shipBuyPriceMult");

        final Map<SubmarketAPI, PriceData> found = new HashMap<>(), foundFree = new HashMap<>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            for (SubmarketAPI submarket : market.getSubmarketsCopy())
            {
                if (submarket.getPlugin() instanceof BaseSubmarketPlugin)
                {
                    final BaseSubmarketPlugin plugin = (BaseSubmarketPlugin) submarket.getPlugin();
                    plugin.updateCargoPrePlayerInteraction();
                }

                int total = 0;
                boolean isFirstFound = true;
                float price = 0f;
                boolean isIllegal = false, isFree = false;
                for (FleetMemberAPI member : submarket.getCargo().getMothballedShips().getMembersListCopy())
                {
                    if (member.getHullId().equalsIgnoreCase(args))
                    {
                        total++;

                        if (isFirstFound)
                        {
                            isFirstFound = false;

                            if (submarket.getPlugin().isFreeTransfer())
                            {
                                price = 0;
                                isIllegal = false;
                                isFree = true;
                                System.out.println("Found free in " + submarket.getName());
                            }
                            else
                            {
                                price = member.getBaseBuyValue();
                                price += (price * submarket.getTariff());
                                isIllegal = submarket.getPlugin().isIllegalOnSubmarket(
                                        member, TransferAction.PLAYER_BUY);
                            }
                        }
                    }
                }

                if (total > 0)
                {
                    if (isFree)
                    {
                        foundFree.put(submarket, new PriceData(price, total, isIllegal));
                    }
                    else
                    {
                        found.put(submarket, new PriceData(price, total, isIllegal));
                    }
                }
            }
        }

        if (found.isEmpty() && foundFree.isEmpty())
        {
            Console.showMessage("No ships with id '" + args + "' found!");
            return CommandResult.SUCCESS;
        }

        if (!found.isEmpty())
        {
            Console.showMessage("Found " + found.size() + " markets with an "
                    + Character.toUpperCase(args.charAt(0)) + args.substring(1) + " for sale:");
            for (Map.Entry<SubmarketAPI, PriceData> entry : found.entrySet())
            {
                SubmarketAPI submarket = entry.getKey();
                PriceData data = entry.getValue();
                Console.showMessage(" - " + data.getAvailable() + " available for "
                        + Math.round(data.getPrice()) + " credits at "
                        + submarket.getMarket().getName() + "'s "
                        + submarket.getNameOneLine() + " submarket ("
                        + submarket.getFaction().getDisplayName() + ", "
                        + submarket.getMarket().getPrimaryEntity()
                        .getContainingLocation().getName()
                        + (data.isIllegal() ? ", restricted)" : ")"));
            }
        }

        if (!foundFree.isEmpty())
        {
            Console.showMessage("Found " + foundFree.size() + " storage tabs with an "
                    + Character.toUpperCase(args.charAt(0)) + args.substring(1) + " stored in them:");
            for (Map.Entry<SubmarketAPI, PriceData> entry : foundFree.entrySet())
            {
                SubmarketAPI submarket = entry.getKey();
                PriceData data = entry.getValue();
                Console.showMessage(" - " + data.getAvailable() + " available at "
                        + submarket.getMarket().getName() + "'s "
                        + submarket.getNameOneLine() + " submarket ("
                        + submarket.getFaction().getDisplayName() + ", "
                        + submarket.getMarket().getPrimaryEntity()
                        .getContainingLocation().getName() + ")");
            }
        }

        return CommandResult.SUCCESS;
    }

    private class PriceData
    {
        private final float pricePer;
        private final int totalAvailable;
        private final boolean isIllegal;

        private PriceData(float pricePer, int totalAvailable, boolean isIllegal)
        {
            this.pricePer = pricePer;
            this.totalAvailable = totalAvailable;
            this.isIllegal = isIllegal;
        }

        private float getPrice()
        {
            return pricePer;
        }

        private int getAvailable()
        {
            return totalAvailable;
        }

        private boolean isIllegal()
        {
            return isIllegal;
        }
    }
}
