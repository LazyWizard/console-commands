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
import org.lazywizard.console.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO: Rewrite to match vanilla fleet spawning behavior
public class SpawnFleet implements BaseCommandWithSuggestion
{
    private static final String DEFAULT_NAME = " Fleet";
    private static final List<String> FLEET_TYPES;

    static
    {
        final Set<String> tmp = new HashSet<>();
        tmp.add(FleetTypes.BATTLESTATION);
        tmp.add(FleetTypes.FOOD_RELIEF_FLEET);
        tmp.add(FleetTypes.INSPECTION_FLEET);
        tmp.add(FleetTypes.MERC_ARMADA);
        tmp.add(FleetTypes.MERC_BOUNTY_HUNTER);
        tmp.add(FleetTypes.MERC_PATROL);
        tmp.add(FleetTypes.MERC_PRIVATEER);
        tmp.add(FleetTypes.MERC_SCOUT);
        tmp.add(FleetTypes.PATROL_LARGE);
        tmp.add(FleetTypes.PATROL_MEDIUM);
        tmp.add(FleetTypes.PATROL_SMALL);
        tmp.add(FleetTypes.PERSON_BOUNTY_FLEET);
        tmp.add(FleetTypes.SCAVENGER_LARGE);
        tmp.add(FleetTypes.SCAVENGER_MEDIUM);
        tmp.add(FleetTypes.SCAVENGER_SMALL);
        tmp.add(FleetTypes.TASK_FORCE);
        tmp.add(FleetTypes.TRADE);
        tmp.add(FleetTypes.TRADE_LINER);
        tmp.add(FleetTypes.TRADE_SMALL);
        tmp.add(FleetTypes.TRADE_SMUGGLER);
        FLEET_TYPES = new ArrayList<>(tmp);
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

        final String type = (FLEET_TYPES.contains(tmp[2]) ? tmp[2] : FleetTypes.PATROL_LARGE);
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
                    type, // Fleet type
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

            Console.showMessage("Spawned a " + totalFP + "FP " + faction.getFleetTypeName(type) +
                    " fleet with " + totalOfficers + " officers, aligned with faction " + faction.getId() + ".");
            return CommandResult.SUCCESS;
        }
        catch (Exception ex)
        {
            Console.showMessage("Unable to spawn generic patrol fleet for faction '" + faction.getId() + "'!");
            return CommandResult.ERROR;
        }
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        List<String> suggestions = new ArrayList<>();
        if (parameter == 0) {
            suggestions.addAll(Global.getSettings().getAllFactionSpecs().stream().map(it -> it.getId()).toList());
        }
        return suggestions;
    }
}
