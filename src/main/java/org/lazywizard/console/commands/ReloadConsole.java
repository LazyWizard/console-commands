package org.lazywizard.console.commands;

import org.json.JSONException;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.ui.FontException;

import java.io.IOException;

public class ReloadConsole implements BaseCommand
{
    public static void reloadConsole() throws JSONException, IOException, FontException
    {
        Console.reloadSettings();
        CommandStore.reloadAll();
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
        catch (IOException | JSONException | FontException ex)
        {
            Console.showException("Failed to reload console!", ex);
            return CommandResult.ERROR;
        }

        Console.showMessage("Reloaded console settings and commands.");
        return CommandResult.SUCCESS;
    }
}
