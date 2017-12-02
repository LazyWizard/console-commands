package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SubmarketPlugin.TransferAction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;

import java.text.NumberFormat;
import java.util.*;

// TODO: Potentially split into FindItem, FindWeapon, FindWing and FindHullmod?
// FIXME: Prices are sometimes incorrect
// FIXME: Stacks over 10k (ex: fuel) report incorrect totals
public class FindItem implements BaseCommand
{
    private static float getPrice()
    {
        return 0f; // TODO
    }

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

        boolean isWeapon = false, isWing = false;
        final List allWeps = Global.getSector().getAllWeaponIds(),
                allWings = Global.getSector().getAllFighterWingIds(),
                allItems = Global.getSector().getEconomy().getAllCommodityIds();
        final Map.Entry<String, Collection<String>> bestMatch =
                CommandUtils.findBestStringMatch(args, allWeps, allWings, allItems);
        final String id = bestMatch.getKey();
        if (id == null)
        {
            Console.showMessage("No weapons, LPCs or commodities found with id '"
                    + args + "'.\nUse \"list commodities\", \"list wings\" or \"list weapons\""
                    + " to show all valid options.");
            return CommandResult.ERROR;
        }

        final Collection<String> source = bestMatch.getValue();
        if (source == allWeps)
            isWeapon = true;
        else if (source == allWings)
            isWing = true;

        // Weapon analysis has to be done through a cargo stack
        CargoStackAPI stack = null;
        final float weaponPriceMod = Global.getSettings().getFloat("nonEconItemBuyPriceMult"),
                wingPriceMod = Global.getSettings().getFloat("shipBuyPriceMult");
        if (isWeapon || isWing)
        {
            CargoAPI tmp = Global.getFactory().createCargo(false);
            tmp.addItems(isWeapon ? CargoItemType.WEAPONS : CargoItemType.FIGHTER_CHIP, id, 1);
            stack = tmp.getStacksCopy().get(0);
        }

        final Comparator<SubmarketAPI> comparator = new SortMarketsByDistance(
                Global.getSector().getPlayerFleet());
        final Map<SubmarketAPI, PriceData> found = new TreeMap<>(comparator),
                foundFree = new TreeMap<>(comparator);
        for (final MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            for (final SubmarketAPI submarket : market.getSubmarketsCopy())
            {
                if (submarket.getPlugin() instanceof BaseSubmarketPlugin)
                {
                    final BaseSubmarketPlugin plugin = (BaseSubmarketPlugin) submarket.getPlugin();
                    plugin.updateCargoPrePlayerInteraction();
                }

                final int total = (int) submarket.getCargo().getQuantity(
                        (isWeapon ? CargoItemType.WEAPONS : (isWing ? CargoItemType.FIGHTER_CHIP : CargoItemType.RESOURCES)), id);
                if (total > 0)
                {
                    float price;
                    boolean isIllegal, isFree = false;

                    if (submarket.getPlugin().isFreeTransfer())
                    {
                        price = 0;
                        isIllegal = false;
                        isFree = true;
                    }
                    else if (isWeapon || isWing)
                    {
                        price = stack.getBaseValuePerUnit() * (isWeapon ? weaponPriceMod : wingPriceMod);
                        price += (price * submarket.getTariff());
                        isIllegal = submarket.getPlugin().isIllegalOnSubmarket(
                                stack, TransferAction.PLAYER_BUY);
                    }
                    else
                    {
                        price = market.getSupplyPrice(id, 1f, true);
                        price += (price * submarket.getTariff());
                        isIllegal = submarket.getPlugin().isIllegalOnSubmarket(
                                id, TransferAction.PLAYER_BUY);
                    }

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
            Console.showMessage("No " + (isWeapon ? "weapons" : (isWing ? "LPCs" : "commodities"))
                    + " with id '" + id + "' are available! Try using \"ForceMarketUpdate\".");
            return CommandResult.SUCCESS;
        }

        if (!found.isEmpty())
        {
            Console.showMessage("Found " + found.size() + " markets with "
                    + (isWeapon ? "weapon '" : (isWing ? "LPC '" : "commodity '")) + id + "' for sale:");
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
                    + (isWeapon ? "weapon '" : (isWing ? "LPC '" : "commodity '")) + id + "' stored in them:");
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

    public static class PriceData
    {
        private final float pricePer;
        private final int totalAvailable;
        private final boolean isIllegal;

        public PriceData(float pricePer, int totalAvailable, boolean isIllegal)
        {
            this.pricePer = pricePer;
            this.totalAvailable = totalAvailable;
            this.isIllegal = isIllegal;
        }

        public float getPrice()
        {
            return pricePer;
        }

        public String getFormattedPrice()
        {
            return NumberFormat.getIntegerInstance().format(Math.round(getPrice()));
        }

        public int getAvailable()
        {
            return totalAvailable;
        }

        public boolean isIllegal()
        {
            return isIllegal;
        }
    }

    public static class SortMarketsByDistance implements Comparator<SubmarketAPI>
    {
        private final SectorEntityToken token;
        private final LocationAPI location;

        public SortMarketsByDistance(SectorEntityToken token)
        {
            this.token = token;
            location = token.getContainingLocation();
        }

        @Override
        public int compare(SubmarketAPI o1, SubmarketAPI o2)
        {
            // Ensure there's an entity associated with this market
            final SectorEntityToken t1 = o1.getMarket().getPrimaryEntity(),
                    t2 = o2.getMarket().getPrimaryEntity();
            if (t1 == null)
            {
                //System.out.println(o1.getMarket().getName() + "'s primary entity was null!");
                return 1;
            }
            if (t2 == null)
            {
                //System.out.println(o2.getMarket().getName() + "'s primary entity was null!");
                return -1;
            }

            // If both markets are in another system, sort by hyperspace distance
            final LocationAPI l1 = t1.getContainingLocation(),
                    l2 = t2.getContainingLocation();
            if (l1 != location && l2 != location)
            {
                //System.out.println(t1.getFullName() + " and " + t2.getFullName()
                //        + " are in another system.");
                return Float.compare(MathUtils.getDistanceSquared(
                        token.getLocationInHyperspace(), t1.getLocationInHyperspace()),
                        MathUtils.getDistanceSquared(token.getLocationInHyperspace(),
                                t2.getLocationInHyperspace()));
            }

            // If only one market is in the same system as the sort token, things are simple
            if (l1 == location && l2 != location)
            {
                //System.out.println(t1.getFullName() + " is in the same system.");
                return -1;
            }
            if (l2 == location && l1 != location)
            {
                //System.out.println(t2.getFullName() + " is in the same system.");
                return 1;
            }

            // If both locations are in the same system as sort token, sort by local distance
            //System.out.println(t1.getFullName() + " and " + t2.getFullName()
            //        + " are in the same system.");
            return Float.compare(MathUtils.getDistanceSquared(token, t1),
                    MathUtils.getDistanceSquared(token, t2));
        }
    }
}
