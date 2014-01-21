package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandContext;
import org.lazywizard.console.Console;

public class Test implements BaseCommand
{
    @Override
    public boolean runCommand(String args, CommandContext context)
    {
        Console.showMessage("\n\tRan test command with arguments: " + args + "\n");
        return true;
    }
}
