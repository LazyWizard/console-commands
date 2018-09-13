package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

// TODO
public class ListStorage implements BaseCommand
{
    public static List<SubmarketAPI> getStorageSubmarkets()
    {
        final List<SubmarketAPI> submarkets = new ArrayList<>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE))
            {
                continue;
            }

            submarkets.add(market.getSubmarket(Submarkets.SUBMARKET_STORAGE));
        }

        return submarkets;
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        boolean showShips = false, showWeapons = false, showItems = false;
        switch (args.toLowerCase())
        {
            case "ships":
                showShips = true;
                break;
            case "weapons":
                showWeapons = true;
                break;
            case "items":
            case "commodities":
                showItems = true;
                break;
            case "all":
            default:
                showShips = true;
                showWeapons = true;
                showItems = true;
        }

        final List<String> itemIds, weaponIds;
        if (showItems)
        {
            itemIds = new ArrayList<>(
                    Global.getSector().getEconomy().getAllCommodityIds());
            Collections.sort(itemIds);
        }
        if (showWeapons)
        {
            weaponIds = new ArrayList<>(
                    Global.getSector().getAllWeaponIds());
            Collections.sort(weaponIds);
        }

        final Map<String, CommodityInfo> results = new TreeMap<>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE))
            {
                continue;
            }

            final SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
            final SubmarketPlugin plugin = storage.getPlugin();
            if (!(plugin instanceof StoragePlugin))
            {
                continue;
            }

            if (showShips)
            {
                // TODO
            }

            if (showWeapons)
            {
                final CommodityInfo info = new CommodityInfo(
                        market.getId(), CommodityType.WEAPON, storage.getCargo());
            }
        }

        return CommandResult.SUCCESS;
    }

    private enum CommodityType
    {
        SHIP,
        WEAPON,
        COMMODITY
    }

    private static class CommodityInfo
    {
        final String id;
        final CommodityType type;
        final Map<String, Integer> totals;

        private CommodityInfo(String id, CommodityType type, CargoAPI cargo)
        {
            this.id = id;
            this.type = type;
            totals = new TreeMap<>();
        }

        private void checkAndAddCargo(String marketId, CargoAPI cargo)
        {
            int total;
            switch (type)
            {
                case SHIP:
                    total = 0;
                    for (FleetMemberAPI member
                            : cargo.getMothballedShips().getMembersListCopy())
                    {
                        if (id.equals((member.isFighterWing() ? member.getSpecId()
                                : member.getHullId())));
                        total++;
                    }
                    break;
                case WEAPON:
                    total = cargo.getNumWeapons(id);
                    break;
                default:
                    total = (int) cargo.getCommodityQuantity(id);
            }

            if (totals.containsKey(marketId))
            {
                totals.put(marketId, totals.get(marketId) + total);
            }
            else
            {
                totals.put(marketId, total);
            }
        }
    }
}
