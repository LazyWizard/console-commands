package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.apache.log4j.Logger;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AllHullmods implements BaseCommand
{
    private static final Logger Log = Logger.getLogger(AllHullmods.class);

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

        // TODO: Check with Alex that this is the correct method (campaign UI? WTF)
        for (String id : Global.getSector().getCampaignUI().getAvailableHullModIds())
        {
            target.addHullmods(id, 1);
            total++;
        }

        Console.showMessage("Added " + total + " hullmods to " + targetName + ".");
        return CommandResult.SUCCESS;
    }
}