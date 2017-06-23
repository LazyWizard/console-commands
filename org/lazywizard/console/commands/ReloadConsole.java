package org.lazywizard.console.commands;

import org.json.JSONException;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.Console;
import org.lazywizard.console.ConsoleOverlay;
import org.lazywizard.console.font.FontException;

import java.io.IOException;

public class ReloadConsole implements BaseCommand
{
    public static void reloadConsole(boolean onApplicationLoad) throws JSONException, IOException, FontException
    {
        Console.reloadSettings();
        CommandStore.reloadCommands();
        RunCode.reloadImports();
        RunCode.reloadMacros();

        // The overlay will load the font itself at startup, no need to do so twice
        if (!onApplicationLoad) ConsoleOverlay.reloadFont();
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        try
        {
            reloadConsole(false);
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
