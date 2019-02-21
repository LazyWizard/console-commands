package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;
import org.lazywizard.console.rulecmd.ConsoleShouldIntercept;

public class DebugDialogs implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        final boolean debugEnabled = !ConsoleShouldIntercept.shouldIntercept();
        ConsoleShouldIntercept.setIntercepting(debugEnabled);

        if (debugEnabled)
        {
            Console.showMessage("All rule-based dialogs will now show any memory map changes.");
        }
        else
        {
            Console.showMessage("Rule-based dialogs will no longer show memory map changes.");
        }

        return CommandResult.SUCCESS;
    }
}
