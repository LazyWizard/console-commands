package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

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
        for (int x = 3; x < tmp.length; x++)
        {
            subNames.add(tmp[x]);
        }
        String name = CollectionUtils.implode(subNames, " ");

        try
        {
            final CampaignFleetAPI toSpawn = FleetFactory.createGenericFleet(faction, name, quality, totalFP);
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
