package org.lazywizard.console;

import org.lazywizard.console.BaseCommand.CommandContext;

interface BaseConsoleListener
{
    public CommandContext getContext();
    public void showOutput(String output);
}
