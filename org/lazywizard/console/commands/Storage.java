package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.SubmarketPlugin.OnClickAction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.campaign.CargoUtils;

import java.util.Map;

// TODO: Automatically move storage to Home if it has an unlocked storage tab
public class Storage implements BaseCommand
{
    public static SectorEntityToken getStorageStation()
    {
        // Which abandoned station Storage uses is only set once, ever
        Map<String, Object> data = Global.getSector().getPersistentData();
        CargoAPI toTransfer = null;
        if (data.containsKey(CommonStrings.DATA_STORAGE_ID))
        {
            // Compatibility fix for those who used the broken Storage command
            Object tmp = data.get(CommonStrings.DATA_STORAGE_ID);
            if (tmp instanceof CargoAPI)
            {
                toTransfer = (CargoAPI) tmp;
            }
            else
            {
                return (SectorEntityToken) tmp;
            }
        }

        // First check if we're in a vanilla sector setup
        // If so, try to find the Abandoned Terraforming Platform
        SectorEntityToken storageStation = null;
        StarSystemAPI corvus = Global.getSector().getStarSystem("corvus");
        if (corvus != null)
        {
            storageStation = corvus.getEntityById("corvus_abandoned_station");
        }

        // ATP not found? Find first available station with the 'abandoned' condition
        // If no abandoned stations are found, find an unlocked storage submarket
        SectorEntityToken firstUnlockedStation = null;
        if (storageStation == null)
        {
            for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy())
            {
                SubmarketAPI storage = market.getSubmarket(Submarkets.SUBMARKET_STORAGE);
                if (storage != null)
                {
                    // Ensure there's a token attached to this market
                    SectorEntityToken station = market.getPrimaryEntity();
                    if (station == null)
                    {
                        continue;
                    }

                    // Prefer the abandoned station above all else
                    if (market.hasCondition(Conditions.ABANDONED_STATION))
                    {
                        storageStation = station;
                        break;
                    }

                    // Otherwise, prefer a station with unlocked storage
                    if (firstUnlockedStation == null && storage.getPlugin()
                            .getOnClickAction(null) == OnClickAction.OPEN_SUBMARKET)
                    {
                        firstUnlockedStation = station;
                    }
                }
            }
        }

        // No abandoned station found, use a station with unlocked storage
        if (storageStation == null)
        {
            storageStation = firstUnlockedStation;
        }

        // No station found, will search again next time the command is used
        if (storageStation == null)
        {
            return null;
        }

        // Transfer old Storage contents to new station
        if (toTransfer != null)
        {
            Console.showMessage("Transferred old Storage data.");
            CargoAPI transferTo = storageStation.getMarket().getSubmarket(
                    Submarkets.SUBMARKET_STORAGE).getCargo();
            CargoUtils.moveCargo(toTransfer, transferTo);
            CargoUtils.moveMothballedShips(toTransfer, transferTo);
            data.remove(CommonStrings.DATA_STORAGE_ID);
        }

        data.put(CommonStrings.DATA_STORAGE_ID, storageStation);
        Console.showMessage("Storage set to " + storageStation.getFullName()
                + " in " + storageStation.getContainingLocation().getName() + ".");
        return storageStation;
    }

    public static CargoAPI retrieveStorage()
    {
        // Check for abandoned station
        SectorEntityToken storage = getStorageStation();
        if (storage != null)
        {
            return storage.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
        }

        // If abandoned station isn't found, return player fleet's cargo
        return Global.getSector().getPlayerFleet().getCargo();
    }

    public static FleetDataAPI retrieveStorageFleetData()
    {
        // Check for abandoned station
        SectorEntityToken storage = getStorageStation();
        if (storage != null)
        {
            return storage.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE)
                    .getCargo().getMothballedShips();
        }

        // If abandoned station isn't found, return player's fleet data
        return Global.getSector().getPlayerFleet().getFleetData();
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final SectorEntityToken station = getStorageStation();
        if (station == null)
        {
            Console.showMessage(
                    "A valid storage station was not found! Any commands that"
                            + " normally place items in storage will instead"
                            + " place them in the player's cargo.");
            return CommandResult.ERROR;
        }

        args = args.toLowerCase();
        if (args.startsWith("clear"))
        {
            if ("clear ships".equals(args))
            {
                final FleetDataAPI storedShips = retrieveStorage().getMothballedShips();
                final int size = storedShips.getNumMembers();
                storedShips.clear();
                Console.showMessage("Storage fleet cleared. " + size + " ships deleted.");
                return CommandResult.SUCCESS;
            }
            else
            {
                final CargoAPI storage = retrieveStorage();
                int numStacks = storage.getStacksCopy().size();
                storage.clear();
                Console.showMessage("Storage cargo cleared. " + numStacks + " item stacks deleted.");
                return CommandResult.SUCCESS;
            }
        }

        Console.showDialogOnClose(station);
        Console.showMessage("Storage will be shown when you close the console overlay.");
        return CommandResult.SUCCESS;
    }
}
