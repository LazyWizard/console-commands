package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionDoctrineAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

// TODO: Rewrite to match vanilla fleet spawning behavior
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

        String[] tmp = args.split(" ", 3);

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

        final String name = (tmp[2].isEmpty() ? "Fleet" : tmp[2]);
        try
        {
            // Create fleet
            final FactionDoctrineAPI doctrine = faction.getDoctrine();
            final int totalFP = Integer.parseInt(tmp[1]);
            final float freighterFP = totalFP * doctrine.getCombatFreighterProbability(); // TEMP
            final FleetParamsV3 params = new FleetParamsV3(
                    null, // Hyperspace location
                    faction.getId(), // Faction ID
                    null, // Quality override (null disables)
                    FleetTypes.PATROL_LARGE, // Fleet type
                    totalFP, // Combat FP
                    freighterFP * .3f, // Freighter FP
                    freighterFP * .3f, // Tanker FP
                    freighterFP * .1f, // Transport FP
                    freighterFP * .1f, // Liner FP
                    freighterFP * .1f, // Utility FP
                    0f); // Quality bonus
            final CampaignFleetAPI toSpawn = FleetFactoryV3.createFleet(params);
            // TODO: Properly determine number of officers using doctrine
            final int totalOfficers = Math.min(15, Math.max(2, (int) (toSpawn.getFleetData()
                    .getCombatReadyMembersListCopy().size() / 8f)));
            FleetFactoryV3.addCommanderAndOfficers(toSpawn, params, MathUtils.getRandom());
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

            Console.showMessage("Spawned a " + totalFP + "FP patrol fleet with "
                    + totalOfficers + " officers, aligned with faction " + faction.getId() + ".");
            return CommandResult.SUCCESS;
        }
        catch (Exception ex)
        {
            Console.showMessage("Unable to spawn generic patrol fleet for faction '" + faction.getId() + "'!");
            return CommandResult.ERROR;
        }
    }
}
