package org.lazywizard.console.commands;

import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Storage implements BaseCommand
{
    private static SectorEntityToken getAbandonedStation()
    {
        // Which abandoned station Storage uses is only set once, ever
        Map<String, Object> data = Global.getSector().getPersistentData();
        if (data.containsKey(CommonStrings.DATA_STORAGE_ID))
        {
            // Compatibility fix for those who used the broken Storage command
            Object tmp = data.get(CommonStrings.DATA_STORAGE_ID);
            if (tmp instanceof CargoAPI)
            {
                Console.showMessage("Removing old Storage data.");
                data.remove(CommonStrings.DATA_STORAGE_ID);
            }
            else
            {
                return (SectorEntityToken) tmp;
            }
        }

        // First check if we're in a vanilla-like sector setup
        // If so, try to find the Abandoned Terraforming Platform
        SectorEntityToken abandonedStation = null;
        StarSystemAPI corvus = Global.getSector().getStarSystem("corvus");
        if (corvus != null)
        {
            abandonedStation = corvus.getEntityById("corvus_abandoned_station");
        }

        // ATP not found? Find first available station with the 'abandoned' condition
        if (abandonedStation == null)
        {
            for (LocationAPI loc : Global.getSector().getStarSystems())
            {
                for (SectorEntityToken station : loc.getEntitiesWithTag(Tags.STATION))
                {
                    MarketAPI market = station.getMarket();
                    if (market != null && market.hasCondition(Conditions.ABANDONED_STATION))
                    {
                        SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
                        if (storage != null)
                        {
                            abandonedStation = station;
                        }
                    }
                }
            }
        }

        data.put(CommonStrings.DATA_STORAGE_ID, abandonedStation);
        return abandonedStation;
    }

    public static CargoAPI retrieveStorage()
    {
        // Check for abandoned station
        SectorEntityToken storage = getAbandonedStation();
        if (storage != null)
        {
            return storage.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
        }

        // If abandoned station isn't found, return player fleet's cargo
        return Global.getSector().getPlayerFleet().getCargo();
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context != CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        SectorEntityToken station = getAbandonedStation();
        if (station == null)
        {
            Console.showMessage("Abandoned station not found! Any commands that"
                    + " normally place items in storage will instead"
                    + " place them in the player's cargo.");
            return CommandResult.ERROR;
        }

        Console.showDialogOnClose(station);
        Console.showMessage("Storage will be shown when you next unpause on the campaign map.");
        return CommandResult.SUCCESS;
    }
}
