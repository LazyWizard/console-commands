package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddSupplies implements BaseCommand
{
    public static int addSupplies(CampaignFleetAPI fleet, float maxCargoFraction)
    {
        // Calculate number of supplies needed to reach maxCargoFraction
        // Cap quantity to 100% remaining cargo space, don't go below 0 either
        final CargoAPI cargo = fleet.getCargo();
        int total = (int) Math.min(cargo.getSpaceLeft(), Math.max(0f,
                ((cargo.getMaxCapacity() * maxCargoFraction) - cargo.getSupplies())));

        // Adjust for cargo space supplies take up (if modded, only use 1 in vanilla)
        final float spacePerSupply = Global.getSector().getEconomy()
                .getCommoditySpec("supplies").getCargoSpace();
        if (spacePerSupply > 0)
        {
            total /= spacePerSupply;
        }

        if (total > 0)
        {
            cargo.addSupplies(total);
        }

        return total;
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
            int amount = addSupplies(Global.getSector().getPlayerFleet(), 0.5f);
            Console.showMessage("Added " + (amount > 0 ? amount : "no")
                    + " supplies to player inventory.");
            return CommandResult.SUCCESS;
        }

        int amount;
        try
        {
            amount = Integer.parseInt(args);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: supply amount must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        Global.getSector().getPlayerFleet().getCargo().addSupplies(amount);
        Console.showMessage("Added " + amount + " supplies to player inventory.");
        return CommandResult.SUCCESS;
    }
}
