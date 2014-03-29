package org.lazywizard.console;

import org.lazywizard.console.BaseCommand.CommandContext;

/**
 *
 * @author LazyWizard
 * @since 2.0
 */
public interface BaseConsoleListener
{
    public CommandContext getContext();
    public void showOutput(String output);
}
