package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddWeapon implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.COMBAT_MISSION)
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

        Global.getSector().getPlayerFleet().getCargo().addItems(
                CargoItemType.WEAPONS, tmp[0], amt);

        Console.showMessage("Added " + amt + " of weapon " + tmp[0] + " to player inventory.");
        return CommandResult.SUCCESS;
    }
}
