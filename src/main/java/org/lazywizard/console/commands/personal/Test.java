package org.lazywizard.console.commands.personal;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class Test implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        Console.showMessage("This command has successfully done nothing!");
        return CommandResult.SUCCESS;
    }
}
