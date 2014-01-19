package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandContext;

public class Test implements BaseCommand
{
    @Override
    public boolean runCommand(String args, CommandContext context)
    {
        System.out.println("\n\tRan test command with arguments: " + args + "\n");
        return true;
    }
}
