package org.lazywizard.console.commands;

import java.util.HashMap;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import static org.lazywizard.console.CommandUtils.*;

public class AddCrew implements BaseCommand
{
    public static int getNeededCrew(CampaignFleetAPI fleet)
    {
        int total = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy())
        {
            if (!member.isMothballed())
            {
                total += member.getMaxCrew();
            }
        }

        return total - fleet.getCargo().getCrew();
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
            return runCommand("" + getNeededCrew(
                    Global.getSector().getPlayerFleet()), context);
        }

        if (!isInteger(args))
        {
            return CommandResult.BAD_SYNTAX;
        }

        final int amount = Integer.parseInt(args);
        final CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        final CargoAPI cargo = player.getCargo();

        final Map<FleetMemberAPI, Float> crMap = new HashMap<>();
        for (FleetMemberAPI member : player.getFleetData().getMembersListCopy())
        {
            if (!member.isMothballed())
            {
                crMap.put(member, member.getRepairTracker().getMaxCR());
            }
        }

        if (amount >= 0)
        {
            cargo.addCrew(amount);
            Console.showMessage("Added " + format(amount) + " crew to player fleet.");
        }
        else
        {
            final int removed = Math.min(-amount, cargo.getCrew());
            cargo.removeCrew(removed);
            Console.showMessage("Removed " + format(removed) + " crew from player fleet.");
        }

        // Restore only as much CR as the crew adds
        for (Map.Entry<FleetMemberAPI, Float> entry : crMap.entrySet())
        {
            final RepairTrackerAPI tracker = entry.getKey().getRepairTracker();
            tracker.setCR(Math.max(tracker.getCR(),
                    tracker.getCR() + (tracker.getMaxCR() - entry.getValue())));
        }

        player.forceSync();
        return CommandResult.SUCCESS;
    }
}
