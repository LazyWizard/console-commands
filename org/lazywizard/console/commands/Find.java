package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
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
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.StringUtils;
import org.lwjgl.util.vector.Vector2f;

public class Find implements BaseCommand
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

        String searchFor = args.toLowerCase();
        List<LocationAPI> locations = new ArrayList<>();
        locations.addAll(Global.getSector().getStarSystems());
        locations.add(Global.getSector().getHyperspace());
        int totalResults = 0;
        for (LocationAPI location : locations)
        {
            // TODO: Replace this with LocationAPI.getName() after .6.5a is released
            String name;
            if (location instanceof StarSystemAPI)
            {
                name = ((StarSystemAPI) location).getName();
            }
            else if (location == Global.getSector().getHyperspace())
            {
                name = "Hyperspace";
            }
            else
            {
                name = "Unknown location";
            }

            List<SectorEntityToken> tokens = new ArrayList<>();
            tokens.addAll(location.getEntities(CampaignFleetAPI.class));
            tokens.addAll(location.getEntities(OrbitalStationAPI.class));
            tokens.addAll(location.getEntities(PlanetAPI.class));
            tokens.addAll(location.getEntities(JumpPointAPI.class));
            //tokens.addAll(location.getEntities(AsteroidAPI.class));
            List<String> results = new ArrayList<>();
            for (Object tmp : tokens)
            {
                System.out.println(tmp.getClass());
                SectorEntityToken token = (SectorEntityToken) tmp;
                String tokenName = token.getFullName();
                if (tokenName != null && tokenName.toLowerCase().contains(searchFor))
                {
                    Vector2f loc = token.getLocation();
                    results.add("- " + token.getFullName() + "\n   at {" + loc.x
                            + ", " + loc.y + "}");
                    totalResults++;
                }
            }

            if (!results.isEmpty())
            {
                Console.showMessage("Found " + results.size() + " matches in "
                        + name + ":\n" + StringUtils.indent(CollectionUtils.implode(results,
                                        "\n"), " "));
            }
        }

        Console.showMessage("Found " + totalResults + " total entities with tag \""
                + args + "\".");
        return CommandResult.SUCCESS;
    }
}
