package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.*;

public class AddStoryPoints implements BaseCommand
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
            Console.showMessage("Error: Story points must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        final int amount = Integer.parseInt(args);
        final MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();
        if (amount >= 0)
        {
            player.addStoryPoints(amount);
            Console.showMessage("Added " + format(amount)
                    + " story points to your character.");
        }
        else
        {
            final int removed = Math.min(-amount, player.getStoryPoints());
            player.addStoryPoints(-removed);
            Console.showMessage("Removed " + format(removed) + " story points from your character.");
        }

        return CommandResult.SUCCESS;
    }
}
