package org.lazywizard.console.commands;

import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class SetHome implements BaseCommand
{
    // Distance from a token before the command doesn't consider it
    private static final float DISTANCE_CUTOFF_SQUARED = 300f * 300f;

    private static boolean isInRange(Vector2f loc, SectorEntityToken token)
    {
        return (MathUtils.getDistanceSquared(loc, token.getLocation())
                < DISTANCE_CUTOFF_SQUARED);
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        SectorEntityToken newHome = null;
        final LocationAPI system = Global.getSector().getCurrentLocation();

        // Manually set a home by name
        if (!args.isEmpty())
        {
            newHome = CommandUtils.findTokenInLocation(args,
                    Global.getSector().getCurrentLocation());

            if (newHome == null)
            {
                Console.showMessage("Couldn't find a token by the name '" + args + "'!");
                return CommandResult.ERROR;
            }
        }

        // Home priorities: stations, then relays, then planets, then empty space (raw coords)
        else
        {
            final Vector2f playerLocation = Global.getSector().getPlayerFleet().getLocation();

            List<SectorEntityToken> potentialHomes = system.getEntitiesWithTag(Tags.STATION);
            potentialHomes.addAll(system.getEntitiesWithTag(Tags.COMM_RELAY));
            potentialHomes.addAll(system.getEntitiesWithTag(Tags.PLANET));
            for (SectorEntityToken tmp : potentialHomes)
            {
                if (isInRange(playerLocation, tmp))
                {
                    newHome = tmp;
                    break;
                }
            }

            // No stations, relays or planets in range, use raw coordinates
            if (newHome == null)
            {
                newHome = system.createToken(playerLocation.x, playerLocation.y);
            }
        }

        Global.getSector().getPersistentData().put(CommonStrings.DATA_HOME_ID, newHome);
        Console.showMessage("Home set to " + newHome.getFullName() + " in "
                + system.getName() + ".");
        return CommandResult.SUCCESS;
    }
}
