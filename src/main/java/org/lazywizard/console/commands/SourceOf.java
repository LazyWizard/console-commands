package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.console.Console;

import java.util.Collections;
import java.util.List;

public class SourceOf implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        if ("all".equalsIgnoreCase(args) || "commands".equalsIgnoreCase(args))
        {
            Console.showMessage("Loaded commands come from the following mods:");
            final List<String> allCommands = CommandStore.getLoadedCommands();
            Collections.sort(allCommands, String.CASE_INSENSITIVE_ORDER);
            for (String tmp : allCommands)
            {
                final StoredCommand command = CommandStore.retrieveCommand(tmp);
                Console.showMessage(" - " + tmp + ": " + command.getSource());
            }

            return CommandResult.SUCCESS;
        }

        final StoredCommand command = CommandStore.retrieveCommand(args);
        if (command != null)
        {
            Console.showMessage("Command '" + args + "' is from the following mod:\n"
                    + " - " + command.getSource());
            return CommandResult.SUCCESS;
        }

        // TODO: Extend to cover ships/weapons/modspecs/commodities/classes/files/etc
        Console.showMessage("No command with the name '" + args + "' was found!");
        return CommandResult.ERROR;
    }
}
