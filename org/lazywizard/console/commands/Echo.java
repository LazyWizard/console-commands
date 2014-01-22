package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

public class Echo implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        Console.showMessage(args);
        return CommandResult.SUCCESS;
    }
}
