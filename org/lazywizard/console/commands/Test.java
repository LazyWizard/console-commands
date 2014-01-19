package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;

public class Test implements BaseCommand
{
    @Override
    public boolean runCommand(String args)
    {
        System.out.println("\n\tRan test command with arguments: " + args + "\n");
        return true;
    }
}
