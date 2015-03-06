package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

// TODO: Fix this to work with new .65a fleet spawning behavior
public class SpawnFleet implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context != CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        String[] tmp = args.split(" ");

        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        String faction = tmp[0];
        String fleet = tmp[1];

        try
        {

            final CampaignFleetAPI toSpawn
                    //= Global.getSector().createFleet(faction, fleet);
                    = FleetFactory.createGenericFleet(faction, fleet, 1f, 100);
            final Vector2f offset = MathUtils.getRandomPointOnCircumference(null, 150f);
            Global.getSector().getCurrentLocation().spawnFleet(
                    Global.getSector().getPlayerFleet(), offset.x, offset.y, toSpawn);
        }
        catch (Exception ex)
        {
            Console.showMessage("No such fleet '" + fleet + "' for faction '"
                    + faction + "'!");
            return CommandResult.ERROR;
        }

        Console.showMessage("Fleet '" + fleet + "' successfully spawned!");
        return CommandResult.SUCCESS;
    }
}
