package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.Collections;
import java.util.List;

public class Help implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (args.isEmpty() || args.equalsIgnoreCase("all"))
        {
            List<String> commands = (args.isEmpty() ? CommandStore.getApplicableCommands(context)
                    : CommandStore.getLoadedCommands());
            Collections.sort(commands, String.CASE_INSENSITIVE_ORDER);
            Console.showMessage((args.isEmpty() ? "Applicable commands for context " + context.name()
                    + " (use \"help all\" for a full list):\n" : "Loaded commands:\n")
                    + CollectionUtils.implode(commands));
            Console.showMessage("\nYou can chain multiple commands together by separating them with '"
                    + Console.getSettings().getCommandSeparator() + "'. Use the 'List' command to obtain the various" +
                    " IDs needed for commands. The console also supports tab completion for commands (press tab and the" +
                    " console will cycle through all commands starting with what you've already input)." +
                    " You can also input a newline with shift+enter to break your command into multiple lines.");
            Console.showMessage("\nYou can use 'help <command>' for more information"
                    + " on a specific command or 'help <tag>' to only list"
                    + " commands that have that tag. Console settings can be changed with the 'Settings' command.");
            List<String> tags = CommandStore.getKnownTags();
            Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);
            Console.showMessage("\nValid tags: " + CollectionUtils.implode(tags));
            return CommandResult.SUCCESS;
        }
        else
        {
            args = args.toLowerCase();

            if (CommandStore.getKnownTags().contains(args))
            {
                List<String> commands = CommandStore.getCommandsWithTag(args);
                Collections.sort(commands, String.CASE_INSENSITIVE_ORDER);
                Console.showMessage("Commands with tag '" + args + "':\n"
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
