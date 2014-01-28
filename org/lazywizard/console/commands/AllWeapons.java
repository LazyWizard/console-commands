package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AllWeapons implements BaseCommand
{
    private static final float STACK_SIZE = 10f;

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.COMBAT_MISSION)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        SectorEntityToken target;
        float amount;
        float total = 0;

        if (args == null || args.isEmpty())
        {
            target = Global.getSector().getPlayerFleet();
            amount = 1f;
        }
        else
        {
            target = Global.getSector().getCurrentLocation().getEntityByName(args);
            amount = STACK_SIZE;

            if (target == null)
            {
                Console.showMessage(args + " not found!");
                return CommandResult.ERROR;
            }
        }

        for (String id : Global.getSector().getAllWeaponIds())
        {
            target.getCargo().addItems(CargoItemType.WEAPONS, id, amount);
            total += amount;
        }

        Console.showMessage("Added " + total + " items to "
                + target.getFullName() + ".");
        return CommandResult.SUCCESS;
    }
}
