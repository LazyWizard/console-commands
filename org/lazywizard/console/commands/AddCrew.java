package org.lazywizard.console.commands;

import java.util.HashMap;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddCrew implements BaseCommand
{
    public static int addNeededCrew(CampaignFleetAPI fleet, CrewXPLevel level)
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

        cargo.addCrew(level, total);

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
            return runCommand("regular", context);
        }

        args = args.toLowerCase();
        String[] tmp = args.split(" ");

        if (tmp.length == 1)
        {
            if (CommandUtils.isInteger(tmp[0]))
            {
                return runCommand(tmp[0] + " regular", context);
            }

            try
            {
                final CrewXPLevel level = Enum.valueOf(
                        CrewXPLevel.class, tmp[0].toUpperCase());
                int amt = addNeededCrew(Global.getSector().getPlayerFleet(), level);
                Console.showMessage("Added " + (amt <= 0 ? "no additional"
                        : amt + " " + level.name().toLowerCase())
                        + " crew to player fleet.");
                return CommandResult.SUCCESS;
            }
            catch (IllegalArgumentException ex)
            {
                return CommandResult.BAD_SYNTAX;
            }
        }

        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        int amt;
        try
        {
            amt = Integer.parseInt(tmp[0]);
        }
        catch (NumberFormatException ex)
        {
            // Support for reversed arguments
            try
            {
                amt = Integer.parseInt(tmp[1]);
                tmp[1] = tmp[0];
            }
            catch (NumberFormatException ex2)
            {
                return CommandResult.BAD_SYNTAX;
            }
        }

        CrewXPLevel level;
        switch (tmp[1])
        {
            case "green":
                level = CrewXPLevel.GREEN;
                break;
            case "regular":
                level = CrewXPLevel.REGULAR;
                break;
            case "veteran":
                level = CrewXPLevel.VETERAN;
                break;
            case "elite":
                level = CrewXPLevel.ELITE;
                break;
            default:
                return CommandResult.BAD_SYNTAX;
        }

        if (amt >= 0)
        {
            Global.getSector().getPlayerFleet().getCargo().addCrew(level, amt);
            Console.showMessage("Added " + amt + " " + level.getPrefix()
                    + " crew to player fleet.");
        }
        else
        {
            Global.getSector().getPlayerFleet().getCargo().removeCrew(level, -amt);
            Console.showMessage("Removed " + -amt + " " + level.getPrefix()
                    + " crew from player fleet.");
        }

        return CommandResult.SUCCESS;
    }
}
