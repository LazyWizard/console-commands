package org.lazywizard.console.commands;

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

import java.util.*;

// FIXME: Prices are sometimes incorrect
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

        if (args.endsWith("_wing"))
        {
            return new FindItem().runCommand(args, context);
        }

        if (args.endsWith("_Hull"))
        {
            args = args.substring(0, args.lastIndexOf("_Hull"));
        }

        String id = CommandUtils.findBestStringMatch(args + "_Hull", Global.getSector().getAllEmptyVariantIds());
        if (id == null)
        {
            id = CommandUtils.findBestStringMatch(args + "_wing",
                    Global.getSector().getAllFighterWingIds());
            if (id != null)
            {
                return new FindItem().runCommand(args + "_wing", context);
            }

            Console.showMessage("No hull found with base id '" + args
                    + "'!\nUse \"list hulls\" to show all valid options.");
            return CommandResult.ERROR;
        }

        id = id.substring(0, id.lastIndexOf("_Hull"));

        //System.out.println(id);

        //final float shipPriceMod = Global.getSettings().getFloat("shipBuyPriceMult"); // Unused?
        final List<PriceData> found = new ArrayList<>(),
                foundFree = new ArrayList<>();
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
                    if (member.getHullId().equalsIgnoreCase(id))
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
                        foundFree.add(new PriceData(submarket, price, total, isIllegal));
                    }
                    else
                    {
                        found.add(new PriceData(submarket, price, total, isIllegal));
                    }
                }
            }
        }

        if (found.isEmpty() && foundFree.isEmpty())
        {
            Console.showMessage("No hulls with id '" + id + "' are available! Try using \"ForceMarketUpdate\".");
            return CommandResult.SUCCESS;
        }

        final Comparator<PriceData> comparator = new FindItem.SortByMarketDistance(
                Global.getSector().getPlayerFleet());

        if (!found.isEmpty())
        {
            Console.showMessage("Found " + found.size() + " markets with hull '" + id + "' for sale:");
            Collections.sort(found, comparator);
            for (final PriceData data : found)
            {
                final SubmarketAPI submarket = data.getSubmarket();
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
            Console.showMessage("Found " + foundFree.size() + " storage tabs with hull '" + id + "' stored in them:");
            Collections.sort(foundFree, comparator);
            for (final PriceData data : foundFree)
            {
                final SubmarketAPI submarket = data.getSubmarket();
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
