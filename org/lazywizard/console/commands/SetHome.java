package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import java.util.List;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
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
        SectorEntityToken newHome = null;
        LocationAPI system = Global.getSector().getCurrentLocation();

        // Manually set a home by name
        if (!args.isEmpty())
        {
            newHome = system.getEntityByName(args);
            if (newHome == null)
            {
                Console.showMessage("Couldn't find a token by the name '" + args + "'!");
                return CommandResult.ERROR;
            }
        }
        // Home priorities: stations, then planets, then empty space (raw coords)
        else
        {
            List toSearch = system.getOrbitalStations();
            Vector2f playerLocation = Global.getSector().getPlayerFleet().getLocation();
            SectorEntityToken tmp;

            for (int x = 0; x < toSearch.size(); x++)
            {
                tmp = (SectorEntityToken) toSearch.get(x);
                if (isInRange(playerLocation, tmp))
                {
                    newHome = tmp;
                    break;
                }
            }

            // No stations in range, check planets next
            if (newHome == null)
            {
                toSearch = system.getPlanets();

                for (int y = 0; y < toSearch.size(); y++)
                {
                    tmp = (SectorEntityToken) toSearch.get(y);
                    if (isInRange(playerLocation, tmp))
                    {
                        newHome = tmp;
                        break;
                    }
                }

                // No stations or planets in range, use raw coordinates
                if (newHome == null)
                {
                    newHome = system.createToken(playerLocation.x, playerLocation.y);
                }
            }
        }

        Global.getSector().getPersistentData()
                .put(CommonStrings.DATA_HOME_ID, newHome);
        Console.showMessage("Home set to " + newHome.getFullName() + ".");
        return CommandResult.SUCCESS;
    }
}