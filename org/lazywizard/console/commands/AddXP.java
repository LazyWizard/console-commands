package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import static org.lazywizard.console.CommandUtils.*;

public class AddXP implements BaseCommand
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

        if (!isLong(args))
        {
            Console.showMessage("Error: experience must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        final long amount = Long.parseLong(args);
        final MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();

        if (amount >= 0)
        {
            Console.showMessage("Added " + format(amount) + " experience points to player.");
            player.addXP(amount);
        }
        else
        {
            final long removed = Math.min(-amount, player.getXP());
            Console.showMessage("Removed " + format(removed) + " experience points from player.");
            player.addXP(-removed);
        }

        return CommandResult.SUCCESS;
    }
}
