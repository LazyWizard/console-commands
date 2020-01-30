package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.util.MutableValue;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import static org.lazywizard.console.CommandUtils.*;

public class AddCredits implements BaseCommand
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
            return CommandResult.BAD_SYNTAX;
        }

        if (!isInteger(args))
        {
            Console.showMessage("Error: credit amount must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        final MutableValue credits = Global.getSector().getPlayerFleet().getCargo().getCredits();
        final int amount = Integer.parseInt(args);
        if (amount >= 0)
        {
            credits.add(amount);
            Console.showMessage("Added " + format(amount) + " credits to player inventory.");
        }
        else
        {
            final int removed = Math.min(-amount, (int) credits.get());
            credits.subtract(removed);
            Console.showMessage("Removed " + format(removed) + " credits from player inventory.");
        }

        return CommandResult.SUCCESS;
    }
}
