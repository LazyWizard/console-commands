package org.lazywizard.console.testing;

import org.jetbrains.annotations.Nullable;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandListener;
import org.lazywizard.console.Console;

public class ListOverrideTest2 implements CommandListener
{
    @Override
    public boolean onPreExecute(String command, String args, CommandContext context, boolean alreadyIntercepted)
    {
        if ("list".equals(command))
        {
            Console.showMessage("onPreExecute lowPriority");
            return true;
        }

        return false;
    }

    @Override
    public CommandResult execute(String command, String args, CommandContext context)
    {
        Console.showMessage("execute lowPriority");
        return CommandResult.SUCCESS;
    }

    @Override
    public void onPostExecute(String command, String args, CommandResult result, CommandContext context, @Nullable CommandListener interceptedBy)
    {
        if ("list".equals(command))
        {
            Console.showMessage("onPostExecute lowPriority");
        }
    }
}
