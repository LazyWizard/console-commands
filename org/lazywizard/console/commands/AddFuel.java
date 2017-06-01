package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import static org.lazywizard.console.CommandUtils.*;

public class AddFuel implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            final CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
            return runCommand("" + cargo.getFreeFuelSpace(), context);
        }

        if (!isInteger(args))
        {
            Console.showMessage("Error: fuel amount must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        final int amount = Integer.parseInt(args);
        final CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
        if (amount >= 0)
        {
            cargo.addFuel(amount);
            Console.showMessage("Added " + format(amount) + " fuel to player stores.");
        }
        else
        {
            final int removed = Math.min(-amount, (int) cargo.getFuel());
            cargo.removeFuel(removed);
            Console.showMessage("Removed " + format(removed) + " fuel from player stores.");
        }

        return CommandResult.SUCCESS;
    }
}
