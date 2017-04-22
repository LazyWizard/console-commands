package org.lazywizard.console.commands;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SubmarketPlugin.TransferAction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.commands.FindItem.PriceData;
import org.lazywizard.console.commands.FindItem.SortMarketsByDistance;

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

        boolean isWing = true;
        String id = CommandUtils.findBestStringMatch(args + "_wing",
                Global.getSector().getAllFighterWingIds());
        if (id == null)
        {
            isWing = false;
            id = CommandUtils.findBestStringMatch(args + "_Hull", Global.getSector().getAllEmptyVariantIds());
            if (id == null)
            {
                Console.showMessage("No hull or wing found with base id '" + args
                        + "'!\nUse \"list hulls\" or \"list wings\" to show all valid options.");
                return CommandResult.ERROR;
            }

            id = id.substring(0, id.lastIndexOf("_Hull"));
        }

        //System.out.println(id);

        //final float shipPriceMod = Global.getSettings().getFloat("shipBuyPriceMult"); // Unused?
        final Comparator<SubmarketAPI> comparator = new SortMarketsByDistance(
                Global.getSector().getPlayerFleet());
        final Map<SubmarketAPI, PriceData> found = new TreeMap<>(comparator),
                foundFree = new TreeMap<>(comparator);
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
                    if ((isWing && member.getSpecId().equalsIgnoreCase(id))
                            || (!isWing && member.getHullId().equalsIgnoreCase(id)))
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
            Console.showMessage("No " + (isWing ? "wings" : "hulls")
                    + " with id '" + id + "' are available! Try using \"ForceMarketUpdate\".");
            return CommandResult.SUCCESS;
        }

        if (!found.isEmpty())
        {
            Console.showMessage("Found " + found.size() + " markets with "
                    + (isWing ? "wing '" : "hull '") + id + "' for sale:");
            for (Map.Entry<SubmarketAPI, PriceData> entry : found.entrySet())
            {
                SubmarketAPI submarket = entry.getKey();
                PriceData data = entry.getValue();
                Console.showMessage(" - " + data.getAvailable() + " available for "
                        + data.getFormattedPrice() + " credits at "
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
            Console.showMessage("Found " + foundFree.size() + " storage tabs with "
                    + (isWing ? "wing '" : "hull '") + id + "' stored in them:");
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
}
