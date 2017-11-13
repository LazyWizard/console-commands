package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AllCommodities implements BaseCommand
{
    private static final int MAX_STACK_SIZE = 10_000;

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        CargoAPI target;
        String targetName;
        int total = 0, stackSize = MAX_STACK_SIZE;

        if (args == null || args.isEmpty())
        {
            target = Storage.retrieveStorage();
            targetName = "storage (use 'storage' to retrieve)";
        }
        else if ("player".equalsIgnoreCase(args))
        {
            target = Global.getSector().getPlayerFleet().getCargo();
            stackSize = 100;
            targetName = "player fleet";
        }
        else
        {
            SectorEntityToken tmp = CommandUtils.findTokenInLocation(args,
                    Global.getSector().getCurrentLocation());

            if (tmp == null)
            {
                Console.showMessage(args + " not found!");
                return CommandResult.ERROR;
            }

            target = CommandUtils.getUsableCargo(tmp);
            targetName = tmp.getFullName();
        }

        for (String id : Global.getSector().getEconomy().getAllCommodityIds())
        {
            int amount = (int) (stackSize - target.getQuantity(CargoItemType.RESOURCES, id));
            if (amount > 0)
            {
                target.addItems(CargoItemType.RESOURCES, id, amount);
                total += amount;
            }
        }

        Console.showMessage("Added " + total + " trade items to " + targetName + ".");
        return CommandResult.SUCCESS;
    }
}
