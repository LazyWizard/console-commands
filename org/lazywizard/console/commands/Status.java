package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.Console;

public class Status implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        StringBuilder status = new StringBuilder(128)
                .append("Console status:\n - Current context: ")
                .append(context.toString())
                .append("\n - Loaded commands: ")
                .append(CommandStore.getLoadedCommands().size())
                .append("\n - Loaded tags: ")
                .append(CommandStore.getKnownTags().size());
                //.append("\n - Loaded aliases: ")
                //.append(CommandStore.getAliases().size());
        Console.showMessage(status.toString());
        return CommandResult.SUCCESS;
    }
}