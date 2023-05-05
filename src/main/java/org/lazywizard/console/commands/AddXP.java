package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.plugins.LevelupPlugin;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.format;
import static org.lazywizard.console.CommandUtils.isLong;

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

        final MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();
        final LevelupPlugin plugin = Global.getSettings().getLevelupPlugin();

        final long amount;
        if (args.isEmpty())
        {
            amount = Math.max(0L, plugin.getXPForLevel(Math.min(plugin.getMaxLevel(),
                    player.getLevel() + 1)) - player.getXP());
        }
        else
        {
            if (!isLong(args))
            {
                Console.showMessage("Error: experience must be a whole number!");
                return CommandResult.BAD_SYNTAX;
            }

            amount = Long.parseLong(args);
        }

        if (amount >= 0L)
        {
            final long added = Math.min(amount, plugin.getXPForLevel(plugin.getMaxLevel()));// - player.getXP());
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
