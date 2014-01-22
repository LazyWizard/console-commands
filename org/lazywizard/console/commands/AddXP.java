package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddXP implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.COMBAT_MISSION)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        MutableCharacterStatsAPI character =
                Global.getSector().getPlayerFleet().getCommanderStats();
        long amount;

        try
        {
            amount = Long.parseLong(args);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: experience must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        character.addXP(amount);
        Console.showMessage("Added " + amount + " experience points to character.");
        return CommandResult.SUCCESS;
    }
}