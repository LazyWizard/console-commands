package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CoreInteractionListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.util.Map;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

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

            // TODO: find way to use showCore cargo/fleet tab w/ infinite stacks
            // LocationAPI.createToken() doesn't have cargo so won't work,
            // and I'd rather not muck up the save with an invisible station
            dialog.getVisualPanel().showLoot("Storage",
                    Storage.retrieveStorage(Global.getSector()), listener);

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
    }
}
