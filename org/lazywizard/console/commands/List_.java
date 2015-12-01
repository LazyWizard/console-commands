package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.StringUtils;

public class List_ implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        // Get all valid IDs for the specified type
        args = args.toLowerCase();
        final SectorAPI sector = Global.getSector();
        final LocationAPI loc = sector.getCurrentLocation();
        final CampaignFleetAPI player = sector.getPlayerFleet();
        boolean newLinePerItem = false;
        List<String> ids;
        switch (args)
        {
            case "ships":
            case "hulls":
                ids = new ArrayList<>();
                for (String id : sector.getAllEmptyVariantIds())
                {
                    ids.add(id.substring(0, id.lastIndexOf("_Hull")));
                }
                break;
            case "variants":
                ids = new ArrayList<>(Global.getSettings().getAllVariantIds());
                break;
            case "wings":
            case "fighters":
            case "squadrons":
                ids = new ArrayList<>(sector.getAllFighterWingIds());
                break;
            case "weapons":
                ids = new ArrayList<>(sector.getAllWeaponIds());
                break;
            case "commodities":
            case "items":
                ids = new ArrayList<>(sector.getEconomy().getAllCommodityIds());
                break;
            case "planets":
                ids = new ArrayList<>();
                for (PlanetAPI planet : loc.getPlanets())
                {
                    ids.add(planet.getId() + (planet.isStar() ? " (star)" : ""));
                }
                break;
            case "systems":
            case "locations":
                ids = new ArrayList<>();
                ids.add(sector.getHyperspace().getId());
                for (LocationAPI location : sector.getStarSystems())
                {
                    ids.add(location.getId());
                }
                break;
            case "factions":
                ids = new ArrayList<>();
                for (FactionAPI faction : sector.getAllFactions())
                {
                    ids.add(faction.getId());
                }
                break;
            case "stations":
                args = "stations in current system";
                ids = new ArrayList<>();
                for (SectorEntityToken station : sector.getCurrentLocation()
                        .getEntitiesWithTag(Tags.STATION))
                {
                    ids.add(station.getId());
                }
                break;
            case "markets":
                newLinePerItem = true;
                ids = new ArrayList<>();
                for (MarketAPI market : sector.getEconomy().getMarketsCopy())
                {
                    ids.add(market.getId() + " in " + market.getContainingLocation().getName()
                            + " (" + market.getFaction().getDisplayName() + ", "
                            + (market.getFaction() == null ? "no faction)"
                                    : market.getFaction().getRelationshipLevel(
                                            player.getFaction()).getDisplayName() + ")"));
                }
                break;
            default:
                return CommandResult.BAD_SYNTAX;
        }

        // Format and print the list of valid IDs
        Collections.sort(ids);
        final String results = CollectionUtils.implode(ids, (newLinePerItem ? "\n" : ", "));
        Console.showMessage("Known " + args + " (" + ids.size() + "):\n"
                + StringUtils.indent(StringUtils.wrapString(results,
                        Console.getSettings().getMaxOutputLineLength() - 3), "   "));
        return CommandResult.SUCCESS;
    }
}
