package org.lazywizard.console.commands;

import java.util.Map;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Storage implements BaseCommand
{
    private static final boolean IS_BROKEN = true;

    public static CargoAPI retrieveStorage(SectorAPI sector)
    {
        // TODO: Remove this after Storage is fixed
        if (IS_BROKEN)
        {
            // Check for abandoned station
            MarketAPI storage = Global.getSector().getEconomy().getMarket(
                    "corvus_abandoned_station_market");
            if (storage != null)
            {
                return storage.getSubmarket(Submarkets.SUBMARKET_STORAGE).getCargo();
            }

            // If abandoned station isn't found, return player fleet's cargo
            return sector.getPlayerFleet().getCargo();
        }

        Map<String, Object> data = sector.getPersistentData();
        if (!data.containsKey(CommonStrings.DATA_STORAGE_ID))
        {
            FactoryAPI fac = Global.getFactory();
            CargoAPI storage = fac.createCargo(true);
            storage.initMothballedShips("player");
            data.put(CommonStrings.DATA_STORAGE_ID, storage);

            return storage;
        }

        return (CargoAPI) data.get(CommonStrings.DATA_STORAGE_ID);
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context != CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        //Console.showDialogOnClose(getStorageContainer());
        //Console.showDialogOnClose(new StorageInteractionDialogPlugin());
        //Console.showMessage("Storage will be shown when you next unpause on the campaign map.");
        //return CommandResult.SUCCESS;
        Console.showMessage("The Storage command is currently non-functional."
                + " Any commands that normally place items in storage will instead"
                + " place them in the Abandoned Terraforming Platform in Corvus,"
                + " or the player fleet if the ATP is not found.");
        return CommandResult.ERROR;
    }
}
