package data.scripts.console.commands;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import data.scripts.console.BaseCommand;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

public class SetHome extends BaseCommand
{
    // Distance from a token before the command doesn't consider it
    private static final float DISTANCE_CUTOFF = 300f;

    @Override
    protected String getHelp()
    {
        return "Sets the point to be teleported to with the 'home' command.\n"
                + "If an argument is entered, the command will try to find"
                + " a token with that name in the system. Otherwise, it"
                + " will select a nearby object (or raw coordinates if no"
                + " valid station or planet is found).";
    }

    @Override
    protected String getSyntax()
    {
        return "sethome <optionalHome>";
    }

    private static float getDistance(Vector2f vector1, Vector2f vector2)
    {
        float a = vector1.x - vector2.x;
        float b = vector1.y - vector2.y;

        return (float) Math.hypot(a, b);
    }

    @Override
    public boolean runCommand(String args)
    {
        SectorEntityToken newHome = null;
        StarSystemAPI system = getStarSystem();

        // Manually set a home by name
        if (!args.isEmpty())
        {
            newHome = system.getEntityByName(args);
            if (newHome == null)
            {
                showMessage("Couldn't find a token by the name '" + args + "'!");
                return false;
            }
        }
        // Home priorities: stations, then planets, then empty space (raw coords)
        else
        {
            List toSearch = system.getOrbitalStations();
            Vector2f playerLocation = getSector().getPlayerFleet().getLocation();
            SectorEntityToken tmp;

            for (int x = 0; x < toSearch.size(); x++)
            {
                tmp = (SectorEntityToken) toSearch.get(x);
                if (getDistance(playerLocation, tmp.getLocation()) < DISTANCE_CUTOFF)
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
                    if (getDistance(playerLocation, tmp.getLocation()) < DISTANCE_CUTOFF)
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

        showMessage("Home set to " + newHome.getFullName() + ".");
        setVar("Home", newHome);
        return true;
    }
}
