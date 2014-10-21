package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AsteroidAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.OrbitalStationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.StringUtils;
import org.lwjgl.util.vector.Vector2f;

public class Find implements BaseCommand
{
    private static String getName(LocationAPI location)
    {
        if (location instanceof StarSystemAPI)
        {
            return ((StarSystemAPI) location).getName();
        }

        return (location == Global.getSector().getHyperspace()
                ? "Hyperspace" : "Unknown location");
    }

    private static String getType(SectorEntityToken token)
    {
        if (token instanceof AsteroidAPI)
        {
            return "(asteroid)";
        }

        if (token instanceof CampaignFleetAPI)
        {
            return "(fleet)";
        }

        if (token instanceof PlanetAPI)
        {
            return ((PlanetAPI) token).isStar() ? "(star)" : "(planet)";
        }

        if (token instanceof OrbitalStationAPI)
        {
            return "(station)";
        }

        if (token instanceof JumpPointAPI)
        {
            return "(jump point)";
        }

        return "(misc)";
    }

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

        String searchFor = args.toLowerCase();
        boolean includeAsteroids = "asteroid".contains(searchFor);
        List<LocationAPI> locations = new ArrayList<>();
        locations.addAll(Global.getSector().getStarSystems());
        locations.add(Global.getSector().getHyperspace());
        StringBuilder results = new StringBuilder();
        int totalResults = 0;
        for (LocationAPI location : locations)
        {
            List<SectorEntityToken> tokens = new ArrayList<>();
            tokens.addAll(location.getEntities(CampaignFleetAPI.class));
            tokens.addAll(location.getEntities(OrbitalStationAPI.class));
            tokens.addAll(location.getEntities(PlanetAPI.class));
            tokens.addAll(location.getEntities(JumpPointAPI.class));

            // Let's allow the player to do this if they're actually crazy enough to try
            if (includeAsteroids)
            {
                tokens.addAll(location.getEntities(AsteroidAPI.class));
            }

            int numResults = 0;
            results.setLength(256);
            for (SectorEntityToken token : tokens)
            {
                // Avoid potential NPE crash here with certain entities
                String tokenName = token.getName();
                if (tokenName != null && tokenName.toLowerCase().contains(searchFor))
                {
                    Vector2f loc = token.getLocation();
                    results.append("- ").append(tokenName).append(" ")
                            .append(getType(token)).append("\n   at {")
                            .append(loc.x).append(", ").append(loc.y).append("}\n");
                    numResults++;
                }
            }

            if (numResults > 0)
            {
                totalResults += numResults;
                Console.showMessage("Found " + numResults + " matches in "
                        + location.getName() + ":\n" + StringUtils.indent(
                                results.toString(), " "));
            }
        }

        Console.showMessage("Found " + totalResults + " total entities with tag \""
                + args + "\".");
        return CommandResult.SUCCESS;
    }
}
