package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;

public class GC implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        System.gc();
        return CommandResult.SUCCESS;
    }
}
