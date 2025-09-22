package org.lazywizard.console;

import java.util.List;

/**
 * A {@link BaseCommand} that can return parameters for the reworked console UI.
 *
 * @author Lukas04
 */
public interface BaseCommandWithSuggestion extends BaseCommand {

    /**
     * Returns values to suggest when using the reworked console UI.
     *
     * @param parameter The current parameter that is being suggested, starts at [0]
     * @param previous A {@link List<String>} of previous parameters entered for this command. Useful if a previous parameter excludes the use of later ones. Set to lowercase by default
     * @param context Where this command will be called from (campaign, combat, mission, simulation, etc).
     *
     * @return Returns a {@link List<String>} of suggested parameters. Return an empty list if nothing should be suggested.
     */
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context);

}
