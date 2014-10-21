package org.lazywizard.console.commands;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.OrbitAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
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

        Console.showDialogOnClose(new StorageInteractionDialogPlugin());
        Console.showMessage("Storage will be shown when you next unpause on the campaign map.");
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
            // TODO: Fix this (broken after .65a with the introduction of markets)
            //dialog.getVisualPanel().showCore(CoreUITabId.CARGO,
            //        new FakeToken(), listener);
            dialog.getVisualPanel().showLoot("Storage",
                    retrieveStorage(Global.getSector()), listener);

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

        @Override
        public Map<String, MemoryAPI> getMemoryMap()
        {
            return null;
        }

        private static class StorageInteractionDialogListener implements CoreInteractionListener
        {
            boolean isDismissed = false;

            @Override
            public void coreUIDismissed()
            {
                isDismissed = true;
            }
        }

        private static class FakeToken implements SectorEntityToken
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
            public String getName()
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

            @Override
            public boolean isPlayerFleet()
            {
                return false;
            }

            @Override
            public MarketAPI getMarket()
            {
                return null;
                //return Global.getFactory().createMarket("fake", "Console Storage", 1);
            }

            @Override
            public void setMarket(MarketAPI mapi)
            {
            }

            @Override
            public Vector2f getLocationInHyperspace()
            {
                return new Vector2f(0f, 0f);
            }

            @Override
            public String getId()
            {
                return "console-storage";
            }

            @Override
            public boolean hasTag(String string)
            {
                return false;
            }

            @Override
            public void addTag(String string)
            {
            }

            @Override
            public void removeTag(String string)
            {
            }

            @Override
            public Collection<String> getTags()
            {
                return Collections.<String>emptyList();
            }

            @Override
            public void clearTags()
            {
            }

            @Override
            public void setFixedLocation(float f, float f1)
            {
            }

            @Override
            public void setCircularOrbit(SectorEntityToken set, float f, float f1, float f2)
            {
            }

            @Override
            public void setCircularOrbitPointingDown(SectorEntityToken set, float f, float f1, float f2)
            {
            }

            @Override
            public void setCircularOrbitWithSpin(SectorEntityToken set, float f, float f1, float f2, float f3, float f4)
            {
            }

            @Override
            public MemoryAPI getMemory()
            {
                return null;
            }

            @Override
            public MemoryAPI getMemoryWithoutUpdate()
            {
                return null;
            }

            @Override
            public float getFacing()
            {
                return 0f;
            }

            @Override
            public void setFacing(float f)
            {
            }

            @Override
            public boolean isInHyperspace()
            {
                return false;
            }

            @Override
            public void addScript(EveryFrameScript efs)
            {
            }

            @Override
            public void removeScript(EveryFrameScript efs)
            {
            }

            @Override
            public void removeScriptsOfClass(Class type)
            {
            }

            @Override
            public boolean isInOrNearSystem(StarSystemAPI ssapi)
            {
                return false;
            }

            @Override
            public boolean isInCurrentLocation()
            {
                return true;
            }

            @Override
            public Vector2f getVelocity()
            {
                return new Vector2f(0f, 0f);
            }

            @Override
            public void setInteractionImage(String string, String string1)
            {
            }

            @Override
            public void setName(String string)
            {
            }
        }
    }
}
