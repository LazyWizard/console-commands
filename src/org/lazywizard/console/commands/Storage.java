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

// TODO: TEST THIS! A lot has changed under the hood
public class Storage implements BaseCommand
{
    private static boolean isValidStorage(SectorEntityToken token)
    {
        return (token != null && token.getMarket() != null
                && token.getMarket().hasSubmarket(Submarkets.SUBMARKET_STORAGE));
    }

    private static SectorEntityToken pickDefaultStorage()
    {
        // First check if we're in a vanilla sector setup
        // If so, try to find the Abandoned Terraforming Platform
        SectorEntityToken storageStation = null;
        StarSystemAPI corvus = Global.getSector().getStarSystem("corvus");
        if (corvus != null)
        {
            storageStation = corvus.getEntityById("corvus_abandoned_station");
            if (storageStation != null)
            {
                return storageStation;
            }
        }

        // ATP not found? Find first available station with the 'abandoned' condition
        // If no abandoned stations are found, find an unlocked storage submarket
        SectorEntityToken firstUnlockedStation = null;
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

                // Prefer abandoned stations above all else
                if (market.hasCondition(Conditions.ABANDONED_STATION))
                {
                    return station;
                }

                // Otherwise, prefer a station with unlocked storage
                if (firstUnlockedStation == null && storage.getPlugin()
                        .getOnClickAction(null) == OnClickAction.OPEN_SUBMARKET)
                {
                    firstUnlockedStation = station;
                }
            }
        }

        // No abandoned stations found, use first available station with unlocked storage (may be null!)
        return firstUnlockedStation;
    }

    public static SectorEntityToken getStorageEntity()
    {
        final Map<String, Object> data = Global.getSector().getPersistentData();
        final SectorEntityToken storageStation;
        if (data.containsKey(CommonStrings.DATA_STORAGE_ID))
        {
            storageStation = (SectorEntityToken) data.get(CommonStrings.DATA_STORAGE_ID);

            // Check if we have the setting enabled to automatically transfer storage to Home
            final SectorEntityToken home = Home.getHome();
            if (Console.getSettings().getUseHomeForStorage() && isValidStorage(home) && (storageStation != home))
            {
                // Transfer old Storage contents to new station
                CargoAPI toTransfer = storageStation.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
                if (toTransfer != null)
                {
                    Console.showMessage("Transferring existing storage from " + storageStation.getFullName()
                            + " in " + storageStation.getContainingLocation().getName()
                            + " to Home.\nTo disable this behavior, use the Settings command.");
                    setStorageEntity(home);
                    return home;
                }
                else
                {
                    Console.showMessage("Couldn't transfer Storage to Home: no storage submarket found!\n");
                }
            }
            else
            {
                return storageStation;
            }
        }
        else
        {
            storageStation = pickDefaultStorage();
            if (storageStation != null)
            {
                setStorageEntity(storageStation);
                return storageStation;
            }
        }

        // Emergency fallback
        Console.showMessage("Couldn't find a valid storage! Falling back to player fleet...");
        return Global.getSector().getPlayerFleet();
    }

    public static boolean setStorageEntity(SectorEntityToken storageEntity)
    {
        if (!isValidStorage(storageEntity)) return false;

        final Map<String, Object> data = Global.getSector().getPersistentData();
        final SectorEntityToken oldStorage = (SectorEntityToken) data.get(CommonStrings.DATA_STORAGE_ID);
        data.put(CommonStrings.DATA_STORAGE_ID, storageEntity);

        // Transfer old Storage contents to new station
        if (oldStorage != null)
        {
            CargoAPI toTransfer = oldStorage.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
            if (toTransfer != null)
            {
                CargoAPI transferTo = storageEntity.getMarket().getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
                CargoUtils.moveCargo(toTransfer, transferTo);
                CargoUtils.moveMothballedShips(toTransfer, transferTo);
                Console.showMessage("Storage set to " + storageEntity.getFullName()
                        + " in " + storageEntity.getContainingLocation().getName() + ".");
            }
        }

        return true;
    }

    /**
     * @deprecated Use {{@link #getStorageEntity()}} instead.
     */
    @Deprecated
    public static SectorEntityToken getStorageStation()
    {
        return getStorageEntity();
    }

    public static CargoAPI retrieveStorage()
    {
        // Check for abandoned station
        SectorEntityToken storage = getStorageEntity();
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
        SectorEntityToken storage = getStorageEntity();
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

        final SectorEntityToken storageEntity = getStorageEntity();
        if (storageEntity == null)
        {
            Console.showMessage("A valid storage station was not found! Any commands that"
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
                final int numShips = storedShips.getNumMembers();
                storedShips.clear();
                Console.showMessage("Storage fleet cleared. " + numShips + " ships deleted.");
                return CommandResult.SUCCESS;
            }
            else
            {
                final CargoAPI storage = retrieveStorage();
                final int numStacks = storage.getStacksCopy().size();
                storage.clear();
                Console.showMessage("Storage cargo cleared. " + numStacks + " item stacks deleted.");
                return CommandResult.SUCCESS;
            }
        }
        else if ("set".equals(args))
        {
            if (!context.isInMarket() || !context.getMarket().hasSubmarket(Submarkets.SUBMARKET_STORAGE))
            {
                Console.showMessage("Invalid storage entity! Dock with a market with a storage tab and try again!");
                return CommandResult.ERROR;
            }

            setStorageEntity(context.getEntityInteractedWith());
            if (Console.getSettings().getUseHomeForStorage())
            {
                new SetHome().runCommand("", context);
            }

            return CommandResult.SUCCESS;
        }

        Console.showDialogOnClose(storageEntity);
        Console.showMessage("Storage will be shown when you close the console overlay.");
        return CommandResult.SUCCESS;
    }
}
