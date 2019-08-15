package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author TMPhoenix
 */
// TODO: add to changelog, update commands.csv with syntax
public class PlanetList implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final SectorAPI sector = Global.getSector();
        args = args.toLowerCase();
        String[] tmp = args.split(" ");
        boolean newLinePerItem = false;
        if (tmp.length > 1)
        {
            if (tmp[1].equals("true"))
            {
                newLinePerItem = true;
            }
        }
        List<String> ids = new ArrayList<>();

        for (LocationAPI location : sector.getStarSystems())
        {
            for (PlanetAPI planet : location.getPlanets())
            {
                MarketAPI market = planet.getMarket();
                ids.add(String.format("[%s {%s}] %s (%s) <%s> |%s|", location.getName(), location.getId(),
                        planet.getName(), planet.getId(), planet.getTypeNameWithWorld(),
                        (market != null) ? ((market.isPlanetConditionMarketOnly()) ? "Unclaimed" : "Inhabited") : "Non-Market"));
            }
        }

        // Support for further filtering results
        if (tmp.length > 0)
        {
            final String filter = tmp[0];
            for (Iterator<String> iter = ids.iterator(); iter.hasNext(); )
            {
                String id = iter.next().toLowerCase();
                if (!id.contains(filter))
                {
                    iter.remove();
                }
            }
        }

        // Format and print the list of valid IDs
        Collections.sort(ids);
        final String results = CollectionUtils.implode(ids, (newLinePerItem ? "\n" : ", "));
        Console.showIndentedMessage("Known planets (" + ids.size() + "):\n", results, 3);
        return CommandResult.SUCCESS;
    }
}
