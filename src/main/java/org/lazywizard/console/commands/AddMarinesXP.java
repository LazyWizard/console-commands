package org.lazywizard.console.commands;

import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;

import static org.lazywizard.console.CommandUtils.*;

public class AddMarinesXP implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty() || !isFloat(args))
        {
            return CommandResult.BAD_SYNTAX;
        }

        final float amount = Float.parseFloat(args);

        // Ripped the code from starfarer.api.impl.campaign.intel.bar.events.MercsOnTheRunBarEvent.class
        PlayerFleetPersonnelTracker.getInstance().update();
        if(PlayerFleetPersonnelTracker.getInstance().getMarineData().num <= 0){
            Console.showMessage("You don't own any marines. Please get some marines first.");
            return CommandResult.ERROR;
        }
        Console.showMessage("Changed marines XP by " + format(amount));
        PlayerFleetPersonnelTracker.getInstance().getMarineData().addXP(amount);

        return CommandResult.SUCCESS;
    }
}
