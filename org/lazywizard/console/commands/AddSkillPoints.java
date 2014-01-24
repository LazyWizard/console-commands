package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
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

        Global.getSector().getPlayerFleet().getCommanderStats().addSkillPoints(amount);
        Console.showMessage("Added " + amount + " skill points to your character.");
        return CommandResult.SUCCESS;
    }
}
