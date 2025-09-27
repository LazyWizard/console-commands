package org.lazywizard.console;

import org.lazywizard.console.listeners.AddSpecialSuggestionsListener;

import java.util.List;

public interface CommandListenerWithSuggestion extends CommandListener {

    /**
     * Returns values to suggest when using the reworked console UI. Suggestions will be combined with others, if there are any.
     * See {@link AddSpecialSuggestionsListener} for an example
     *
     * @param command The current command
     * @param parameter The current parameter that is being suggested, starts at [0]
     * @param previous A {@link List <String>} of previous parameters entered for this command. Useful if a previous parameter excludes the use of later ones. Set to lowercase by default
     * @param context Where this command will be called from (campaign, combat, mission, simulation, etc).
     *
     * @return Returns a {@link List<String>} of suggested parameters. Return an empty list if nothing should be suggested.
     */
    public List<String> getSuggestions(String command, int parameter, List<String> previous, BaseCommand.CommandContext context);

}
