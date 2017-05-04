package org.lazywizard.console.commands;

import java.util.LinkedHashSet;
import java.util.Set;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AllWings implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final CargoAPI target;
        String targetName;
        int total = 0;

        if (args == null || args.isEmpty())
        {
            target = Storage.retrieveStorage();
            targetName = "storage (use 'storage' to retrieve)";
        }
        else if ("player".equalsIgnoreCase(args))
        {
            target = Global.getSector().getPlayerFleet().getCargo();
            targetName = "player fleet";
        }
        else
        {
            SectorEntityToken token = CommandUtils.findTokenInLocation(args,
                    Global.getSector().getCurrentLocation());

            if (token == null)
            {
                Console.showMessage(args + " not found!");
                return CommandResult.ERROR;
            }

            target = CommandUtils.getUsableCargo(token);
            targetName = token.getFullName();
        }

        final Set<String> ids = new LinkedHashSet<>(Global.getSector().getAllFighterWingIds());
        for (CargoStackAPI tmp : target.getStacksCopy())
        {
            if (!tmp.isNull() && tmp.isFighterWingStack())
            {
                ids.remove(tmp.getFighterWingSpecIfWing().getId());
            }
        }

        for (String id : ids)
        {
            target.addFighters(id, 1);
            total++;
        }

        Console.showMessage("Added " + total + " wing LPCs to " + targetName + ".");
        return CommandResult.SUCCESS;
    }
}
