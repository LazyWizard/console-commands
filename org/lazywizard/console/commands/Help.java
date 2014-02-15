package org.lazywizard.console.commands;

import java.util.Collections;
import java.util.List;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

public class Help implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (args.isEmpty())
        {
            List<String> commands = CommandStore.getLoadedCommands();
            Collections.sort(commands);
            Console.showMessage("Loaded commands:\n"
                    + CollectionUtils.implode(commands));
            Console.showMessage("\nYou can use 'help <command>' for more information"
                    + " on a specific command or 'help <tag>' to only list"
                    + " commands that have that tag.");
            List<String> tags = CommandStore.getKnownTags();
            Collections.sort(tags);
            Console.showMessage("\nValid tags: "
                    + CollectionUtils.implode(tags));
            return CommandResult.SUCCESS;
        }
        else
        {
            args = args.toLowerCase();

            if (CommandStore.getKnownTags().contains(args))
            {
                List<String> commands = CommandStore.getCommandsWithTag(args);
                Collections.sort(commands);
                Console.showMessage("Commands with tag '"+args+"':\n"
                        + CollectionUtils.implode(commands));
                return CommandResult.SUCCESS;
            }

            StoredCommand command = CommandStore.retrieveCommand(args);
            if (command == null)
            {
                Console.showMessage("No such command '" + args + "'!");
                return CommandResult.ERROR;
            }

            Console.showMessage("HELP - " + command.getName().toUpperCase());
            if (!command.getTags().isEmpty())
            {
                Console.showMessage("Tags: " + CollectionUtils.implode(command.getTags()));
            }
            if (!command.getSyntax().isEmpty())
            {
                Console.showMessage("Syntax: " + command.getSyntax());
            }
            if (!command.getHelp().isEmpty())
            {
                Console.showMessage("Detailed use: " + command.getHelp());
            }
            return CommandResult.SUCCESS;
        }
    }
}
