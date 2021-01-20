package org.lazywizard.console.commands;

import java.util.Collections;
import java.util.List;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.console.Console;

// TODO: Extend to cover ships/weapons/modspecs/commodities/etc
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
            Console.showMessage("Loaded commands come from the following mods:");
            List<String> allCommands = CommandStore.getLoadedCommands();
            Collections.sort(allCommands, String.CASE_INSENSITIVE_ORDER);
            for (String tmp : allCommands)
            {
                StoredCommand command = CommandStore.retrieveCommand(tmp);
                Console.showMessage(" - "+ tmp + ": " + command.getSource());
            }

            return CommandResult.SUCCESS;
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
