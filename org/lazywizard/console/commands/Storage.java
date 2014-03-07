package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.util.Map;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

public class Storage implements BaseCommand
{
    public static CargoAPI retrieveStorage(SectorAPI sector)
    {
        Map<String, Object> data = sector.getPersistentData();
        if (!data.containsKey(CommonStrings.DATA_STORAGE_ID))
        {
            CargoAPI storage = Global.getFactory().createCargo(true);
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

        Global.getSector().getCampaignUI().showInteractionDialog(
                new StorageInteractionDialogPlugin(), null);
        return CommandResult.SUCCESS;
    }

    private static class StorageInteractionDialogPlugin implements InteractionDialogPlugin
    {
        private InteractionDialogAPI dialog;
        private StorageInteractionDialogListener listener;

        @Override
        public void init(InteractionDialogAPI dialog)
        {
            this.dialog = dialog;
            listener = new StorageInteractionDialogListener();

            // Use a fake SectorEntityToken to open the infinite-stack cargo
            // created in the main command, prone to breaking after an update
            dialog.getVisualPanel().showCore(CoreUITabId.CARGO,
                    new FakeToken(), listener);

            // Use this if FakeToken fails to compile, no ship storage support
            //dialog.getVisualPanel().showLoot("Storage",
            //        Storage.retrieveStorage(Global.getSector()), listener);

            // Only here as emergency backup if listener fails
            dialog.getOptionPanel().addOption("Leave", null);
            dialog.setOptionOnEscape("Leave", null);
        }

        @Override
        public void optionSelected(String optionText, Object optionData)
        {
            // Only here as emergency backup if listener fails
            dialog.dismiss();
        }

        @Override
        public void optionMousedOver(String optionText, Object optionData)
        {
        }

        @Override
        public void advance(float amount)
        {
            if (listener.isDismissed)
            {
                dialog.dismiss();
            }
        }

        @Override
        public void backFromEngagement(EngagementResultAPI battleResult)
        {
        }

        @Override
        public Object getContext()
        {
            return null;
        }

        private class StorageInteractionDialogListener implements CoreInteractionListener
        {
            boolean isDismissed = false;

            @Override
            public void coreUIDismissed()
            {
                isDismissed = true;
            }
        }

        private class FakeToken implements SectorEntityToken
        {
            @Override
            public CargoAPI getCargo()
            {
                return Storage.retrieveStorage(Global.getSector());
            }

            @Override
            public Vector2f getLocation()
            {
                return Global.getSector().getPlayerFleet().getLocation();
            }

            @Override
            public OrbitAPI getOrbit()
            {
                return null;
            }

            @Override
            public void setOrbit(OrbitAPI orbit)
            {
            }

            @Override
            public Object getName()
            {
                return "Storage";
            }

            @Override
            public String getFullName()
            {
                return "Console Storage";
            }

            @Override
            public void setFaction(String factionId)
            {
            }

            @Override
            public LocationAPI getContainingLocation()
            {
                return Global.getSector().getPlayerFleet().getContainingLocation();
            }

            @Override
            public float getRadius()
            {
                return 0;
            }

            @Override
            public FactionAPI getFaction()
            {
                return Global.getSector().getFaction("player");
            }

            @Override
            public String getCustomDescriptionId()
            {
                return "";
            }

            @Override
            public void setCustomDescriptionId(String customDescriptionId)
            {
            }

            @Override
            public void setCustomInteractionDialogImageVisual(InteractionDialogImageVisual visual)
            {
            }

            @Override
            public InteractionDialogImageVisual getCustomInteractionDialogImageVisual()
            {
                return null;
            }

            @Override
            public void setFreeTransfer(boolean freeTransfer)
            {
            }

            @Override
            public boolean isFreeTransfer()
            {
                return true;
            }
        }
    }
}
