package org.lazywizard.console.commands;

import java.util.HashSet;
import java.util.Set;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.Console;

public class Status implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        StringBuilder status = new StringBuilder(160)
                .append("Console status:\n - Current context: ")
                .append(context.toString())
                .append("\n - Loaded commands: ")
                .append(CommandStore.getLoadedCommands().size())
                .append("\n - Loaded tags: ")
                .append(CommandStore.getKnownTags().size());
                //.append("\n - Loaded aliases: ")
        //.append(CommandStore.getAliases().size());

        Set<String> sources = new HashSet();
        for (String tmp : CommandStore.getLoadedCommands())
        {
            sources.add(CommandStore.retrieveCommand(tmp).getSource());
        }

        status.append("\n - Number of mods that added commands: " + sources.size());

        Console.showMessage(status.toString());
        return CommandResult.SUCCESS;
    }
}
