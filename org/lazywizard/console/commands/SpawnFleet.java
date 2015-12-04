package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV2;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

// TODO: Add SpawnLeveledFleet command
public class SpawnFleet implements BaseCommand
{
    private static final String DEFAULT_QUALITY = " 0.6";
    private static final String DEFAULT_NAME = " Fleet";

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
            return runCommand(args + DEFAULT_QUALITY + DEFAULT_NAME, context);
        }

        if (tmp.length == 3)
        {
            return runCommand(args + DEFAULT_NAME, context);
        }

        FactionAPI faction = CommandUtils.findBestFactionMatch(tmp[0]);
        if (faction == null)
        {
            Console.showMessage("No such faction '" + tmp[0] + "'!");
            return CommandResult.ERROR;
        }

        int totalFP;
        try
        {
            totalFP = Integer.parseInt(tmp[1]);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Fleet points must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        float quality;
        try
        {
            quality = Float.parseFloat(tmp[2]);
        }
        // TODO: If not a number, use default quality and assume tmp[2] is part of name/crew level
        catch (NumberFormatException ex)
        {
            Console.showMessage("Quality must be a decimal, preferably between 0 and 1.");
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
            // TODO: Rip this out and replace entirey with FleetFactoryV2
            final CampaignFleetAPI toSpawn
                    = FleetFactory.createGenericFleet(faction.getId(), name, quality, totalFP);
            FleetFactoryV2.addCommanderAndOfficers(Math.max(1,
                    (toSpawn.getFleetSizeCount() - toSpawn.getNumFighters()) / 3),
                    1f, Math.min(20f, 15f * quality), toSpawn, null, MathUtils.getRandom());

            /*FleetFactoryV2.createFleet(new FleetParams(
                    hyperspaceLocation,
                    market,
                    faction.getId(),
                    fleetType,
                    combatFP,
                    freighterPts,
                    tankerPts,
                    transportPts,
                    linerPts,
                    civilianPts,
                    utilityPts,
                    qualityBonus,
                    qualityOverride))
            );*/
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
            toSpawn.getFleetData().sort();
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
                    + faction.getId() + "'!");
            return CommandResult.ERROR;
        }

        Console.showMessage("Spawned a " + totalFP + "FP "
                + crewLevel.getPrefix().toLowerCase()
                + " fleet aligned with faction " + faction.getId() + ".");
        return CommandResult.SUCCESS;
    }
}
