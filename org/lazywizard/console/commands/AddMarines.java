package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddMarines implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.COMBAT_MISSION)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        int amt;
        try
        {
            amt = Integer.parseInt(args);
        }
        catch (NumberFormatException ex)
        {
            return CommandResult.BAD_SYNTAX;
        }

        Global.getSector().getPlayerFleet().getCargo().addMarines(amt);

        Console.showMessage("Added " + amt + " marines to player fleet.");
        return CommandResult.SUCCESS;
    }
}
