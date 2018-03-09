package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.*;

public class Status implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        Set<String> rawSources = new HashSet<>();
        String commands, tags, sources, aliases;

        for (String tmp : CommandStore.getLoadedCommands())
        {
            rawSources.add(CommandStore.retrieveCommand(tmp).getSource());
        }

        // Commands
        List<String> tmp = CommandStore.getLoadedCommands();
        Collections.sort(tmp);
        commands = "(" + tmp.size() + "):\n" + CommandUtils.indent(CollectionUtils.implode(tmp), 3);

        // Tags
        tmp = CommandStore.getKnownTags();
        Collections.sort(tmp);
        tags = "(" + tmp.size() + "):\n" + CommandUtils.indent(CollectionUtils.implode(tmp), 3);

        // Command sources
        tmp = new ArrayList<>(rawSources);
        Collections.sort(tmp);
        sources = "(" + tmp.size() + "):\n" + CommandUtils.indent(CollectionUtils.implode(tmp, "\n"), 3);

        // Aliases
        tmp = new ArrayList<>();
        for (Map.Entry<String, String> entry : CommandStore.getAliases().entrySet())
        {
            tmp.add(entry.getKey() + ": " + entry.getValue());
        }
        Collections.sort(tmp);
        aliases = "(" + tmp.size() + "):\n" + CommandUtils.indent(CollectionUtils.implode(tmp, "\n"), 3);

        String status = "Console status:"
                + "\n - Current context: " + context.name()
                + "\n - Loaded commands " + commands
                + "\n - Loaded tags " + tags
                + "\n - Loaded aliases " + aliases
                + "\n - Mods that added commands " + sources;

        Console.showMessage(status);
        return CommandResult.SUCCESS;
    }
}
