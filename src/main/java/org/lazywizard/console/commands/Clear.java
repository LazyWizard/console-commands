package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

// Only included for the helpfile and will never run - Clear support is hardcoded into the overlay
public class Clear implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        Console.showMessage("This is only a dummy implementation. How did you get this to run?");
        return CommandResult.SUCCESS;
    }
}
