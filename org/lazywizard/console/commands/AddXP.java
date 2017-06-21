package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.plugins.LevelupPlugin;
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
            final LevelupPlugin plugin = Global.getSettings().getLevelupPlugin();
            final long added = Math.min(amount, plugin.getXPForLevel(plugin.getMaxLevel()) - player.getXP());
            Console.showMessage("Added " + format(added) + " experience points to player.");
            player.addXP(added);
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
