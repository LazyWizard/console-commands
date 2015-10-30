package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoAPI.CrewXPLevel;
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

        if (tmp.length < 3)
        {
            return CommandResult.BAD_SYNTAX;
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
        String name = CollectionUtils.implode(subNames, " ");

        try
        {
            final CampaignFleetAPI toSpawn = FleetFactory.createGenericFleet(faction, name, quality, totalFP);
            final CargoAPI cargo = toSpawn.getCargo();
            final int totalCrew = cargo.getTotalCrew();
            cargo.removeCrew(CrewXPLevel.GREEN, cargo.getCrew(CrewXPLevel.GREEN));
            cargo.removeCrew(CrewXPLevel.REGULAR, cargo.getCrew(CrewXPLevel.REGULAR));
            cargo.removeCrew(CrewXPLevel.VETERAN, cargo.getCrew(CrewXPLevel.VETERAN));
            cargo.removeCrew(CrewXPLevel.ELITE, cargo.getCrew(CrewXPLevel.ELITE));
            cargo.addCrew(crewLevel, totalCrew);

            final Vector2f offset = MathUtils.getRandomPointOnCircumference(null, 150f);
            Global.getSector().getCurrentLocation().spawnFleet(
                    Global.getSector().getPlayerFleet(), offset.x, offset.y, toSpawn);
        }
        catch (Exception ex)
        {
            Console.showMessage("Unable to spawn generic fleet for faction '"
                    + faction + "'!");
            return CommandResult.ERROR;
        }

        Console.showMessage("Spawned a " + totalFP + "FP fleet aligned with faction " + faction + ".");
        return CommandResult.SUCCESS;
    }
}
