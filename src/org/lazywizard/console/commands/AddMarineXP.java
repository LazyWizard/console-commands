package org.lazywizard.console.commands;

import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker;
import com.fs.starfarer.api.impl.PlayerFleetPersonnelTracker.PersonnelData;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.format;
import static org.lazywizard.console.CommandUtils.isFloat;

public class AddMarineXP implements BaseCommand
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
            final PlayerFleetPersonnelTracker tracker = PlayerFleetPersonnelTracker.getInstance();
            tracker.update();
            final PersonnelData marines = tracker.getMarineData();
            final int numMarines = (int) marines.num;

            if (numMarines <= 0)
            {
                Console.showMessage("You currently have no marines in your fleet.");
                return CommandResult.SUCCESS;
            }

            Console.showMessage("You currently have " + numMarines + " marines in your fleet.\n" +
                    "Their combined rank is " + marines.getRank().name + ", with a total XP of " +
                    marines.xp + " out of a maximum of " + marines.num + ".");
            return CommandResult.SUCCESS;
        }

        if (!isFloat(args))
        {
            return CommandResult.BAD_SYNTAX;
        }

        float amount = Float.parseFloat(args);
        final PlayerFleetPersonnelTracker tracker = PlayerFleetPersonnelTracker.getInstance();
        tracker.update();

        final PersonnelData marines = tracker.getMarineData();
        if (marines.num <= 0)
        {
            Console.showMessage("There are no marines in your fleet!");
            return CommandResult.ERROR;
        }

        // Cap XP gain/loss
        amount = Math.max(0, Math.min(amount, marines.num - marines.xp));
        if (amount == 0)
        {
            Console.showMessage("Marine experience unchanged.");
            return CommandResult.SUCCESS;
        }

        final String oldRank = marines.getRank().name;
        marines.addXP(amount);
        tracker.update();
        final String newRank = marines.getRank().name;

        Console.showMessage("Changed marines XP by " + format(amount) + ", current rank is " +
                newRank + " (" + (oldRank == newRank ? "unchanged" : "was " + oldRank) + ").");
        return CommandResult.SUCCESS;
    }
}
