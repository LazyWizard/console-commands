package org.lazywizard.console;

import org.lazywizard.console.BaseCommand.CommandContext;

public interface ConsoleListener
{
    CommandContext getContext();
    boolean showOutput(String output);
}
