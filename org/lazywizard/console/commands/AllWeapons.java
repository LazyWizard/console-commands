package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AllWeapons implements BaseCommand
{
    private static final int MAX_STACK_SIZE = 10;

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.COMBAT_MISSION)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        CargoAPI target;
        String targetName;
        int total = 0;

        if (args == null || args.isEmpty())
        {
            target = Storage.retrieveStorage(Global.getSector());
            targetName = "storage (use 'storage' to retrieve)";
        }
        else if ("player".equalsIgnoreCase(args))
        {
            target = Global.getSector().getPlayerFleet().getCargo();
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

            target = tmp.getCargo();
            targetName = tmp.getFullName();
        }

        for (String id : Global.getSector().getAllWeaponIds())
        {
            int amount = MAX_STACK_SIZE - target.getNumWeapons(id);
            target.addItems(CargoItemType.WEAPONS, id, amount);
            total += amount;
        }

        Console.showMessage("Added " + total + " items to " + targetName + ".");
        return CommandResult.SUCCESS;
    }
}
