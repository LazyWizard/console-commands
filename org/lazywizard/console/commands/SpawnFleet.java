package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

// TODO: Add crew level argument
// TODO: Add SpawnLeveledFleet command
public class SpawnFleet implements BaseCommand
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

        if (tmp.length < 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        if (tmp.length == 2)
        {
            return runCommand(args + " 0.6 Fleet", context);
        }

        if (tmp.length == 3)
        {
            return runCommand(args + " Fleet", context);
        }

        String faction = tmp[0];

        int totalFP;
        try
        {
            totalFP = Integer.parseInt(tmp[1]);
        }
        catch (NumberFormatException ex)
        {

            return CommandResult.BAD_SYNTAX;
        }

        float quality;
        try
        {
            quality = Float.parseFloat(tmp[2]);
        }
        catch (NumberFormatException ex)
        {
            return CommandResult.BAD_SYNTAX;
        }

        List<String> subNames = new ArrayList<>(tmp.length - 3);
        CrewXPLevel crewLevel = CrewXPLevel.REGULAR;
        for (int x = 3; x < tmp.length; x++)
        {
            // Support for crew XP level argument
            // If it's not a valid XP level, assume it's part of the name
            if (x == 3)
            {
                switch (tmp[x].toLowerCase())
                {
                    case "green":
                        crewLevel = CrewXPLevel.GREEN;
                        break;
                    case "regular":
                        crewLevel = CrewXPLevel.REGULAR;
                        break;
                    case "veteran":
                        crewLevel = CrewXPLevel.VETERAN;
                        break;
                    case "elite":
                        crewLevel = CrewXPLevel.ELITE;
                        break;
                    default:
                        subNames.add(tmp[x]);
                        break;
                }
            }
            else
            {
                subNames.add(tmp[x]);
            }
        }

        final String name = (subNames.isEmpty() ? "Fleet" : CollectionUtils.implode(subNames, " "));

        try
        {
            // Create fleet
            final CampaignFleetAPI toSpawn = FleetFactory.createGenericFleet(faction, name, quality, totalFP);

            // Set crew XP level
            for (FleetMemberAPI member : toSpawn.getFleetData().getMembersListCopy())
            {
                member.setCrewXPLevel(crewLevel);
            }

            // Spawn fleet around player
            final Vector2f offset = MathUtils.getRandomPointOnCircumference(null,
                    Global.getSector().getPlayerFleet().getRadius() + 150f);
            Global.getSector().getCurrentLocation().spawnFleet(
                    Global.getSector().getPlayerFleet(), offset.x, offset.y, toSpawn);
            Global.getSector().addPing(toSpawn, "danger");

            // Update combat readiness
            toSpawn.forceSync();
            for (FleetMemberAPI member : toSpawn.getFleetData().getMembersListCopy())
            {
                final RepairTrackerAPI repairs = member.getRepairTracker();
                repairs.setCR(repairs.getMaxCR());
            }
        }
        catch (Exception ex)
        {
            Console.showMessage("Unable to spawn generic fleet for faction '"
                    + faction + "'!");
            return CommandResult.ERROR;
        }

        Console.showMessage("Spawned a " + totalFP + "FP "
                + crewLevel.getPrefix().toLowerCase()
                + " fleet aligned with faction " + faction + ".");
        return CommandResult.SUCCESS;
    }
}
