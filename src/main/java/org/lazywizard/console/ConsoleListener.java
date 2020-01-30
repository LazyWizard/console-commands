package org.lazywizard.console;

import org.lazywizard.console.BaseCommand.CommandContext;

interface ConsoleListener
{
    CommandContext getContext();
    boolean showOutput(String output);
}