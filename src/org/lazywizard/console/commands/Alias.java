package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import org.lazywizard.console.*;

import java.util.ArrayList;
import java.util.List;

public class Alias implements BaseCommandWithSuggestion
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        final String[] tmp = args.split(" ", 2);
        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        tmp[0] = tmp[0].toLowerCase();
        if ("remove".equals(tmp[0]))
        {
            if (!CommandStore.getAliases().containsKey(tmp[1]))
            {
                Console.showMessage("No alias registered under '" + tmp[1] + "'!");
                return CommandResult.ERROR;
            }

            try
            {
                CommandStore.registerAlias(tmp[1], null);
                Console.showMessage("Unregistered alias '" + tmp[1] + "'.");
                return CommandResult.SUCCESS;
            }
            catch (Exception ex)
            {
                Console.showException("Failed to unregister alias '" + tmp[1] + "'!", ex);
                return CommandResult.ERROR;
            }
        }

        try
        {
            CommandStore.registerAlias(tmp[0], tmp[1]);
            Console.showMessage("Registered alias '" + tmp[0] + " -> " + tmp[1] + "'.");
            return CommandResult.SUCCESS;
        }
        catch (Exception ex)
        {
            Console.showException("Failed to register alias '" + tmp[0] + " -> " + tmp[1] + "'.", ex);
            return CommandResult.ERROR;
        }
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        List<String> suggestions = new ArrayList<>();

        if (parameter >= 1) {
            suggestions.addAll(CommandStore.getLoadedCommands());
        }

        return suggestions;
    }
}
