package org.lazywizard.console.commands;

import java.io.IOException;
import org.json.JSONException;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.Console;

public class ReloadConsole implements BaseCommand
{
    public static void reloadConsole() throws JSONException, IOException
    {
        Console.reloadSettings();
        CommandStore.reloadCommands();
        RunCode.reloadImports();
        RunCode.reloadMacros();
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        try
        {
            reloadConsole();
        }
        catch (IOException | JSONException ex)
        {
            Console.showException("Failed to reload console settings!", ex);
            return CommandResult.ERROR;
        }

        Console.showMessage("Reloaded console settings.");
        return CommandResult.SUCCESS;
    }
}
