package org.lazywizard.console.commands.personal;

import org.jetbrains.annotations.Nullable;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommandListener;
import org.lazywizard.console.Console;

public class ListOverrideTest implements CommandListener
{
    @Override
    public boolean onPreExecute(String command, String args, CommandContext context, boolean alreadyIntercepted)
    {
        Console.showMessage("onPreExecute");
        return true;
    }

    @Override
    public CommandResult onExecute(String command, String args, CommandContext context)
    {
        Console.showMessage("onExecute");
        return CommandResult.SUCCESS;
    }

    @Override
    public void onPostExecute(String command, String args, CommandResult result, CommandContext context, @Nullable CommandListener interceptedBy)
    {
        Console.showMessage("onPostExecute");

    }
}
