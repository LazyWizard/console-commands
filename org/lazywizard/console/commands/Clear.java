package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

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

        InteractionDialogAPI dialog = Global.getSector().getCampaignUI()
                .getCurrentInteractionDialog();
        if (dialog == null)
        {
            Console.showMessage("No dialog found!");
            return CommandResult.ERROR;
        }

        dialog.getTextPanel().clear();
        return CommandResult.SUCCESS;
    }
}
