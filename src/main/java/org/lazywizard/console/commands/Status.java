package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.StringUtils;

// TODO: Remove string wrapping once overlay breaks strings automatically
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
        commands = "(" + tmp.size() + ")\n" + CollectionUtils.implode(tmp);

        // Tags
        tmp = CommandStore.getKnownTags();
        Collections.sort(tmp);
        tags = "(" + tmp.size() + ")\n" + CollectionUtils.implode(tmp);

        // Command sources
        tmp = new ArrayList<>(rawSources);
        Collections.sort(tmp);
        sources = "(" + tmp.size() + ")\n"
                + StringUtils.indent(CollectionUtils.implode(tmp, "\n"), "   ");

        // Aliases
        tmp = new ArrayList<>();
        for (Map.Entry<String, String> entry : CommandStore.getAliases().entrySet())
        {
            tmp.add(entry.getKey() + ": " + entry.getValue());
        }
        Collections.sort(tmp);
        aliases = "(" + tmp.size() + ")\n"
                + StringUtils.indent(CollectionUtils.implode(tmp, "\n"), "   ");

        String status = "Console status:"
                + "\n - Current context: " + context.name()
                + "\n - Loaded commands: " + commands
                + "\n - Loaded tags: " + tags
                + "\n - Loaded aliases: " + aliases
                + "\n - Mods that added commands: " + sources;

        Console.showMessage(status);
        return CommandResult.SUCCESS;
    }
}
