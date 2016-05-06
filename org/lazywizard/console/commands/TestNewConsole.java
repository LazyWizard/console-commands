package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.ConsoleOverlay;

/**
 *
 * @author LazyWizard
 */
public class TestNewConsole implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        ConsoleOverlay.show(context);
        return CommandResult.SUCCESS;
    }
}
