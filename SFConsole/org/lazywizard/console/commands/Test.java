package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;

public class Test implements BaseCommand
{
    @Override
    public boolean runCommand(String args)
    {
        System.out.println("\n\tSuccess!\n");
        return true;
    }
}
