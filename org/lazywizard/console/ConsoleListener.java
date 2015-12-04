package org.lazywizard.console;

import org.lazywizard.console.BaseCommand.CommandContext;

interface ConsoleListener
{
    public CommandContext getContext();
    public boolean showOutput(String output);
}
