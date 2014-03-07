package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.console.Console;

public class SourceOf implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        if ("all".equalsIgnoreCase(args))
        {
        }

        StoredCommand command = CommandStore.retrieveCommand(args);
        if (command == null)
        {
            Console.showMessage("No command with the name '" + args + "' was found!");
            return CommandResult.ERROR;
        }

        Console.showMessage("Command '" + args + "' is from the following mod:\n"
                + " - " + command.getSource());
        return CommandResult.SUCCESS;
    }
}
