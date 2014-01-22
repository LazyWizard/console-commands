package org.lazywizard.console.commands;

import java.io.IOException;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.Console;

public class Reload implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        try
        {
            Console.reloadSettings();
            CommandStore.reloadCommands();
        }
        catch (IOException | JSONException ex)
        {
            Console.showMessage("Failed to reload console settings!", Level.ERROR);
            return CommandResult.ERROR;
        }

        Console.showMessage("Reloaded console settings.");
        return CommandResult.SUCCESS;
    }
}