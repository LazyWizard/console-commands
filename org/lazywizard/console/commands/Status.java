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

// TODO: Uncomment alias details when aliases are implemented
public class Status implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        Set<String> rawSources = new HashSet<>();
        String commands, tags, sources; //, aliases;

        for (String tmp : CommandStore.getLoadedCommands())
        {
            rawSources.add(CommandStore.retrieveCommand(tmp).getSource());
        }

        if (args.isEmpty())
        {
            commands = Integer.toString(CommandStore.getLoadedCommands().size());
            tags = Integer.toString(CommandStore.getKnownTags().size());
            sources = Integer.toString(rawSources.size());
            //aliases = Integer.toString(CommandStore.getAliases().size());
        }
        else if ("detailed".equalsIgnoreCase(args))
        {
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

            /*tmp = CommandStore.getAliases();
            Collections.sort(tmp);
            aliases = "(" + tmp.size() + ")\n"
                    + StringUtils.indent(CollectionUtils.implode(tmp, "\n"), "   ");*/
        }
        else
        {
            return CommandResult.BAD_SYNTAX;
        }

        String status = "Console status:"
                + "\n - Current context: " + context.toString()
                + "\n - Loaded commands: " + commands
                + "\n - Loaded tags: " + tags
                //+ "\n - Loaded aliases: " + aliases
                + "\n - Mods that added commands: " + sources;

        Console.showMessage(status);
        return CommandResult.SUCCESS;
    }
}
