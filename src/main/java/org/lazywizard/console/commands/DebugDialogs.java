package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class DebugDialogs implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final MemoryAPI memory = Global.getSector().getMemory();
        final boolean debugEnabled = !memory.getBoolean("$consoleDebug");
        Global.getSector().getMemory().set("$consoleDebug", debugEnabled);

        if (debugEnabled)
        {
            Console.showMessage("All rule-based dialogs will now show any memory map changes.");
        }
        else
        {
            Console.showMessage("Rule-based dialogs will no longer show memory map changes.");
        }

        return CommandResult.SUCCESS;
    }
}
