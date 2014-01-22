package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandContext;
import org.lazywizard.console.Console;
import org.lazywizard.console.Strings;

public class AddSkillPoints implements BaseCommand
{
    @Override
    public boolean runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.COMBAT_MISSION)
        {
            Console.showMessage(Strings.ERROR_CAMPAIGN_ONLY);
            return false;
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
            return false;
        }

        character.addSkillPoints(amount);
        Console.showMessage("Added " + amount + " skill points to character.");
        return true;
    }
}
