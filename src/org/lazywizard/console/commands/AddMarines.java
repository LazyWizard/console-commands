package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.format;
import static org.lazywizard.console.CommandUtils.isInteger;

public class AddMarines implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty() || !isInteger(args))
        {
            return CommandResult.BAD_SYNTAX;
        }

        final int amount = Integer.parseInt(args);
        final CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        if (amount >= 0)
        {
            Console.showMessage("Added " + format(amount) + " marines to player fleet.");
            cargo.addMarines(amount);
        }
        else
        {
            final int removed = Math.min(-amount, cargo.getMarines());
            cargo.removeMarines(removed);
            Console.showMessage("Removed " + format(removed) + " marines from player fleet.");
        }

        return CommandResult.SUCCESS;
    }
}
