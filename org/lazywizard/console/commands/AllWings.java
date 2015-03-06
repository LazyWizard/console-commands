package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
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

        FleetDataAPI target;
        String targetName;
        int total = 0;

        if (args == null || args.isEmpty())
        {
            target = Storage.retrieveStorage().getMothballedShips();
            targetName = "storage (use 'storage' to retrieve)";
        }
        else if ("player".equalsIgnoreCase(args))
        {
            target = Global.getSector().getPlayerFleet().getCargo().getMothballedShips();
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

            if (tmp.getCargo().getMothballedShips() == null)
            {
                tmp.getCargo().initMothballedShips(tmp.getFaction().getId());
            }

            target = tmp.getCargo().getMothballedShips();
            targetName = tmp.getFullName();
        }

        for (String id : Global.getSector().getAllFighterWingIds())
        {
            FleetMemberAPI tmp = Global.getFactory().createFleetMember(
                    FleetMemberType.FIGHTER_WING, id);
            tmp.getRepairTracker().setMothballed(true);
            target.addFleetMember(tmp);
            total++;
        }

        Console.showMessage("Added " + total + " ships to " + targetName + ".");
        return CommandResult.SUCCESS;
    }
}
