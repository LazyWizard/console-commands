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

public class AddCrew implements BaseCommand
{
    public static int addNeededCrew(CampaignFleetAPI fleet)
    {
        final CargoAPI cargo = fleet.getCargo();
        final Map<FleetMemberAPI, Float> crMap = new HashMap<>();
        int total = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy())
        {
            if (!member.isMothballed() && member.getNeededCrew() > 0)
            {
                crMap.put(member, member.getRepairTracker().getMaxCR());
                total += member.getNeededCrew();
            }
        }

        cargo.addCrew(total);

        // Restore only as much CR as the crew adds
        for (Map.Entry<FleetMemberAPI, Float> entry : crMap.entrySet())
        {
            final RepairTrackerAPI tracker = entry.getKey().getRepairTracker();
            tracker.setCR(tracker.getCR() + (tracker.getMaxCR() - entry.getValue()));
        }

        fleet.forceSync();
        return total;
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
            int amt = addNeededCrew(Global.getSector().getPlayerFleet());
            Console.showMessage("Added " + (amt <= 0 ? "no additional"
                    : amt) + " crew to player fleet.");
            return CommandResult.SUCCESS;
        }

        int amt;
        try
        {
            amt = Integer.parseInt(args);
        }
        catch (NumberFormatException ex)
        {
            return CommandResult.BAD_SYNTAX;
        }

        if (amt >= 0)
        {
            Global.getSector().getPlayerFleet().getCargo().addCrew(amt);
            Console.showMessage("Added " + amt + " crew to player fleet.");
        }
        else
        {
            Global.getSector().getPlayerFleet().getCargo().removeCrew(-amt);
            Console.showMessage("Removed " + -amt + " crew from player fleet.");
        }

        return CommandResult.SUCCESS;
    }
}
