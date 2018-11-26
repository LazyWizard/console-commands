package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Alias implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        final String[] tmp = args.split(" ", 2);
        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        tmp[0] = tmp[0].toLowerCase();
        if ("remove".equals(tmp[0]))
        {
            if (!CommandStore.getAliases().containsKey(tmp[1]))
            {
                Console.showMessage("No alias registered under '" + tmp[1] + "'!");
                return CommandResult.ERROR;
            }

            try
            {
                CommandStore.registerAlias(tmp[1], null);
                Console.showMessage("Unregistered alias '" + tmp[1] + "'.");
                return CommandResult.SUCCESS;
            }
            catch (Exception ex)
            {
                Console.showException("Failed to unregister alias '" + tmp[1] + "'!", ex);
                return CommandResult.ERROR;
            }
        }

        try
        {
            CommandStore.registerAlias(tmp[0], tmp[1]);
            Console.showMessage("Registered alias '" + tmp[0] + " -> " + tmp[1] + "'.");
            return CommandResult.SUCCESS;
        }
        catch (Exception ex)
        {
            Console.showException("Failed to register alias '" + tmp[0] + " -> " + tmp[1] + "'.", ex);
            return CommandResult.ERROR;
        }
    }
}
