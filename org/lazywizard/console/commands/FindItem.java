package org.lazywizard.console.commands;

import java.util.HashMap;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

// TODO: Needs some cleanup, a lot of weapon/item code can be merged
public class FindItem implements BaseCommand
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

        boolean isWeapon = true;
        String id = CommandUtils.findBestStringMatch(args, Global.getSector().getAllWeaponIds());
        if (id == null)
        {
            isWeapon = false;
            id = CommandUtils.findBestStringMatch(args,
                    Global.getSector().getEconomy().getAllCommodityIds());
            if (id == null)
            {
                Console.showMessage("No weapons or commodities found with id '"
                        + args + "'.");
                return CommandResult.ERROR;
            }
        }

        // Weapon analysis has to be done through a cargo stack
        CargoStackAPI stack = null;
        float weaponPriceMod = Global.getSettings().getFloat("nonEconItemBuyPriceMult");
        if (isWeapon)
        {
            CargoAPI tmp = Global.getFactory().createCargo(false);
            tmp.addWeapons(id, 1);
            stack = tmp.getStacksCopy().get(0);
        }

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

                int total = (int) submarket.getCargo().getQuantity(
                        (isWeapon ? CargoItemType.WEAPONS : CargoItemType.RESOURCES), id);
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
                    else if (isWeapon)
                    {
                        price = stack.getBaseValuePerUnit() * weaponPriceMod;
                        price += (price * submarket.getTariff());
                        isIllegal = submarket.getPlugin().isIllegalOnSubmarket(
                                stack, SubmarketPlugin.TransferAction.PLAYER_BUY);
                    }
                    else
                    {
                        price = market.getSupplyPrice(id, 1f, true);
                        price += (price * submarket.getTariff());
                        isIllegal = submarket.getPlugin().isIllegalOnSubmarket(
                                id, SubmarketPlugin.TransferAction.PLAYER_BUY);
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
            Console.showMessage("No " + (isWeapon ? "weapons" : "commodities")
                    + " with id '" + id + "' found!");
            return CommandResult.SUCCESS;
        }

        if (!found.isEmpty())
        {
            Console.showMessage("Found " + found.size() + " markets with "
                    + (isWeapon ? "weapon '" : " commodity '") + id + "' for sale:");
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
            Console.showMessage("Found " + foundFree.size() + " storage tabs with "
                    + (isWeapon ? "weapon '" : " commodity '") + id + "' stored in them:");
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
