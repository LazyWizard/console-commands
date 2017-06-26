package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.console.ConsoleOverlay;

// TODO: Update for new overlay
public class Clear implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog == null)
        {
            Console.showMessage("No dialog found!");
            return CommandResult.ERROR;
        }

        final float oldXOffset = dialog.getXOffset(),
                oldYOffset = dialog.getYOffset(),
                oldTextWidth = dialog.getTextWidth(),
                oldTextHeight = dialog.getTextHeight();
        dialog.getTextPanel().clear();
        dialog.setTextWidth(oldTextWidth);
        dialog.setTextHeight(oldTextHeight);
        dialog.setXOffset(oldXOffset);
        dialog.setYOffset(oldYOffset);
        return CommandResult.SUCCESS;
    }
}
