package org.lazywizard.console.commands;

import org.apache.log4j.Level;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandContext;
import org.lazywizard.console.Console;

public class Echo implements BaseCommand
{
    @Override
    public boolean runCommand(String args, CommandContext context)
    {
        Console.showMessage(args);
        return true;
    }
}
