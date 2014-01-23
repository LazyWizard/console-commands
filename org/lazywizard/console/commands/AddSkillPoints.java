package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lazywizard.console.CommonStrings;

public class AddSkillPoints implements BaseCommand
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
        int amount;

        try
        {
            amount = Integer.parseInt(args);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: Skill points must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        character.addSkillPoints(amount);
        Console.showMessage("Added " + amount + " skill points to character.");
        return CommandResult.SUCCESS;
    }
}
