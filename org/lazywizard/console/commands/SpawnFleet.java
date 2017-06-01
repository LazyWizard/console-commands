package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV2;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParams;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.loading.FleetCompositionDoctrineAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class SpawnFleet implements BaseCommand
{
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
            return runCommand(args + DEFAULT_NAME, context);
        }

        final FactionAPI faction = CommandUtils.findBestFactionMatch(tmp[0]);
        if (faction == null)
        {
            Console.showMessage("No such faction '" + tmp[0] + "'!");
            return CommandResult.ERROR;
        }

        if (!CommandUtils.isInteger(tmp[1]))
        {
            Console.showMessage("Fleet points must be a whole number!");
            return CommandResult.ERROR;
        }

        List<String> subNames = new ArrayList<>(tmp.length - 2);
        for (int x = 2; x < tmp.length; x++)
        {
            subNames.add(tmp[x]);
        }

        final String name = (subNames.isEmpty() ? "Fleet" : CollectionUtils.implode(subNames, " "));
        try
        {
            // Create fleet
            final FleetCompositionDoctrineAPI doctrine = faction.getCompositionDoctrine();
            final int totalFP = Integer.parseInt(tmp[1]);
            final float freighterFP = totalFP * doctrine.getCombatFreighterProbability(); // TEMP
            final CampaignFleetAPI toSpawn = FleetFactoryV2.createFleet(new FleetParams(
                    null, // Hyperspace location
                    null, // Market
                    faction.getId(), // Faction ID
                    FleetTypes.PATROL_LARGE, // Fleet type
                    totalFP, // Combat FP
                    freighterFP * .3f, // Freighter FP
                    freighterFP * .3f, // Tanker FP
                    freighterFP * .1f, // Transport FP
                    freighterFP * .1f, // Liner FP
                    freighterFP * .1f, // Civilian FP
                    freighterFP * .1f, // Utility FP
                    0f, // Quality bonus
                    -1f) // Quality override (negative disables)
            );
            // TODO: Properly determine number and level of officers using doctrine
            final int totalOfficers = Math.min(15, Math.max(2, (int) (toSpawn.getFleetData()
                    .getCombatReadyMembersListCopy().size() / 8f)));
            FleetFactoryV2.addCommanderAndOfficers(totalOfficers, 1f,
                    15f, toSpawn, null, MathUtils.getRandom());
            toSpawn.setName(name);

            // Spawn fleet around player
            final Vector2f offset = MathUtils.getRandomPointOnCircumference(null,
                    Global.getSector().getPlayerFleet().getRadius()
                    + toSpawn.getRadius() + 150f);
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

            Console.showMessage("Spawned a " + totalFP + "FP  fleet with "
                    + totalOfficers + " officers, aligned with faction " + faction.getId() + ".");
            return CommandResult.SUCCESS;
        }
        catch (Exception ex)
        {
            Console.showMessage("Unable to spawn generic fleet for faction '"
                    + faction.getId() + "'!");
            return CommandResult.ERROR;
        }
    }
}
