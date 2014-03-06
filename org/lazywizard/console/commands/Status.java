package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.StringUtils;

public class Status implements BaseCommand
{
    private static String filterModPath(String fullPath)
    {
        String modPath = fullPath.replace("/", "\\");
        modPath = modPath.substring(modPath.lastIndexOf("\\mods\\"));
        modPath = modPath.substring(0, modPath.indexOf("\\", 6)) + "\\";
        return modPath;
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        Set<String> rawSources = new HashSet<>();
        String commands, tags, sources;
        if (args.isEmpty())
        {
            for (String tmp : CommandStore.getLoadedCommands())
            {
                rawSources.add(CommandStore.retrieveCommand(tmp).getSource());
            }

            commands = Integer.toString(CommandStore.getLoadedCommands().size());
            tags = Integer.toString(CommandStore.getKnownTags().size());
            sources = Integer.toString(rawSources.size());
        }
        else if ("detailed".equalsIgnoreCase(args))
        {
            for (String tmp : CommandStore.getLoadedCommands())
            {
                rawSources.add(filterModPath(CommandStore.retrieveCommand(tmp).getSource()));
            }

            List<String> tmp = CommandStore.getLoadedCommands();
            Collections.sort(tmp);
            commands = "(" + tmp.size() + ")\n" + CollectionUtils.implode(tmp);

            tmp = CommandStore.getKnownTags();
            Collections.sort(tmp);
            tags = "(" + tmp.size() + ")\n" + CollectionUtils.implode(tmp);

            tmp = new ArrayList<>(rawSources);
            Collections.sort(tmp);
            sources = "(" + tmp.size() + ")\n"
                    + StringUtils.indent(CollectionUtils.implode(tmp, "\n"), "   ");
        }
        else
        {
            return CommandResult.BAD_SYNTAX;
        }

        StringBuilder status = new StringBuilder(160)
                .append("Console status:\n - Current context: ").append(context.toString())
                .append("\n - Loaded commands: ").append(commands)
                .append("\n - Loaded tags: ").append(tags)
                //.append("\n - Loaded aliases: ")
                //.append(CommandStore.getAliases().size());
                .append("\n - Mods that added commands: ").append(sources);

        Console.showMessage(status.toString());
        return CommandResult.SUCCESS;
    }
}
