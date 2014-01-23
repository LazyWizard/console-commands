package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.Console;

public class Status implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        StringBuilder sb = new StringBuilder(64)
                .append("Console status:\n - Current context: ")
                .append(context.toString())
                .append("\n - Loaded commands: ")
                .append(CommandStore.getLoadedCommands().size());
        String status = sb.toString();
        Console.showMessage(status);
        System.out.println(status);
        return CommandResult.SUCCESS;
    }
}
