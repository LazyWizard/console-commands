package org.lazywizard.console.commands;

import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommandStore;
import org.lazywizard.console.CommandStore.StoredCommand;
import org.lazywizard.console.Console;
import org.lazywizard.console.overlay.v2.panels.ConsoleOverlayPanel;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class History implements BaseCommandWithSuggestion
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (ConsoleOverlayPanel.getInstance() == null) {
            Console.showMessage("Error: The history command does not work within the legacy console. ");
            return CommandResult.ERROR;
        }
        else if (args.isBlank())
        {
            List<String> history = ConsoleOverlayPanel.getLastCommands();

            for (int i = history.size(); i > 0; i--) {
                String toShow = history.get(i-1);
                Console.showMessage(i+": " + toShow);
            }

            Console.showMessage("Printed command history. Type \"history [index]\" to copy the command in to the input, or use the arrow keys to scroll towards them.");
            return CommandResult.SUCCESS;
        }
        else
        {
            try {
                int index = Integer.parseInt(args);

                if (index <= 0) {
                    Console.showMessage("Theres no command under this index in the history. ");
                    return CommandResult.ERROR;
                }

                List<String> history = ConsoleOverlayPanel.getLastCommands();
                if (index <= history.size()) {
                    String pick = history.get(index-1);
                    if (!pick.endsWith(" ")) {
                        pick += " ";
                    }
                    ConsoleOverlayPanel.getInstance().setInput(pick);
                    ConsoleOverlayPanel.getInstance().setCursorIndex(pick.length());
                    //ConsoleOverlayPanel.getInstance().setRequiresRecreation(true);
                } else {
                    Console.showMessage("Theres no command under this index in the history. ");
                    return CommandResult.ERROR;
                }

                return CommandResult.SUCCESS;
            } catch (Exception e) {
                int index = Integer.parseInt(args);
                Console.showMessage("Error: Index must be a whole number!");
                return CommandResult.ERROR;
            }

        }
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        if (parameter != 0) return new ArrayList<>();
        return CommandStore.getLoadedCommands();
    }
}
