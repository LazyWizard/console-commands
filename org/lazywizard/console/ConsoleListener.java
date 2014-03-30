package org.lazywizard.console;

import org.lazywizard.console.BaseCommand.CommandContext;

interface ConsoleListener
{
    public CommandContext getContext();
    public void showOutput(String output);
}
