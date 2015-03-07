package org.lazywizard.console.commands;

import java.util.HashMap;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

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

        Map<SubmarketAPI, Integer> found = new HashMap<>();
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
        {
            for (SubmarketAPI submarket : market.getSubmarketsCopy())
            {
                int total = (int) submarket.getCargo().getQuantity(
                        (isWeapon ? CargoItemType.WEAPONS : CargoItemType.RESOURCES),
                        id);
                if (total > 0)
                {
                    found.put(submarket, total);
                }
            }
        }

        if (found.isEmpty())
        {
            Console.showMessage("No " + (isWeapon ? "weapons" : "commodities")
                    + " with id '" + id + "' found!");
            return CommandResult.SUCCESS;
        }

        Console.showMessage("Found " + found.size() + " markets with "
                + (isWeapon ? "weapon '" : " commodity '")
                + Character.toUpperCase(id.charAt(0)) + id.substring(1)
                + "' for sale:");
        for (Map.Entry<SubmarketAPI, Integer> entry : found.entrySet())
        {
            SubmarketAPI submarket = entry.getKey();
            Console.showMessage(" - " + entry.getValue() + " for sale in "
                    + submarket.getMarket().getName() + "'s "
                    + submarket.getNameOneLine() + " submarket ("
                    + submarket.getFaction().getDisplayName() + ", "
                    + submarket.getMarket().getPrimaryEntity()
                    .getContainingLocation().getName() + ")");
        }
        return CommandResult.SUCCESS;
    }
}
