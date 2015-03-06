package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

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

        int amt;
        try
        {
            amt = Integer.parseInt(tmp[1]);
        }
        catch (NumberFormatException ex)
        {
            // Support for reversed arguments
            try
            {
                amt = Integer.parseInt(tmp[0]);
                tmp[0] = tmp[1];
            }
            catch (NumberFormatException ex2)
            {
                return CommandResult.BAD_SYNTAX;
            }
        }

        try
        {
            tmp[0] = CommandUtils.findBestStringMatch(tmp[0],
                    Global.getSector().getEconomy().getAllCommodityIds());
            Global.getSector().getPlayerFleet().getCargo().addItems(
                    CargoItemType.RESOURCES, tmp[0], amt);
        }
        catch (Exception ex)
        {
            Console.showMessage("No item found with id '" + tmp[0] + "'!");
            return CommandResult.ERROR;
        }

        Console.showMessage("Added " + amt + " of item " + tmp[0] + " to player inventory.");
        return CommandResult.SUCCESS;
    }
}
