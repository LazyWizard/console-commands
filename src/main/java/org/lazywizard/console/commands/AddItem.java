package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.*;

public class AddItem implements BaseCommand
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

        String[] tmp = args.split(" ");

        if (tmp.length == 1)
        {
            return runCommand(args + " 1", context);
        }

        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        // Support for reversed arguments
        int amount;
        if (isInteger(tmp[1]))
        {
            amount = Integer.parseInt(tmp[1]);
        }
        else
        {
            if (!isInteger(tmp[0]))
            {
                return CommandResult.BAD_SYNTAX;
            }

            amount = Integer.parseInt(tmp[0]);
            tmp[0] = tmp[1];
        }

        String id = findBestStringMatch(tmp[0], Global.getSector().getEconomy().getAllCommodityIds());
        if (id == null)
        {
            id = findBestStringMatch(tmp[0], AddSpecial.getSpecialItemIds());
            if (id == null)
            {
                Console.showMessage("No commodity found with id '" + tmp[0]
                        + "'! Use 'list items' for a complete list of valid ids.");
                return CommandResult.ERROR;
            }

            return new AddSpecial().runCommand(tmp[0], context);
        }

        Global.getSector().getPlayerFleet().getCargo().addItems(
                CargoItemType.RESOURCES, id, amount);
        Console.showMessage("Added " + format(amount) + " of commodity '"
                + id + "' to player inventory.");
        return CommandResult.SUCCESS;
    }
}
