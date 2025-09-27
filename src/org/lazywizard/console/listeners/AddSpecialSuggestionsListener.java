package org.lazywizard.console.listeners;

import com.fs.starfarer.api.Global;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandListener;
import org.lazywizard.console.CommandListenerWithSuggestion;

import java.util.ArrayList;
import java.util.List;

public class AddSpecialSuggestionsListener implements CommandListenerWithSuggestion {

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
    @Override
    public List<String> getSuggestions(String command, int parameter, List<String> previous, BaseCommand.CommandContext context) {
        //This listener wants to add arguments to the "AddSpecial" command, and only for the 2nd parameter.
        if (!command.equalsIgnoreCase("AddSpecial") || parameter != 1) return null;
        if (previous.isEmpty()) return null;
        List<String> suggestions = new ArrayList<>();
        String specialItemID = previous.get(0);

        //Check for matching special item
        switch (specialItemID.toLowerCase()) {
            case "ship_bp" -> suggestions.addAll(Global.getSettings().getAllShipHullSpecs().stream().filter(it -> !it.isDHull()).map(it -> it.getHullId()).toList());
            case "weapon_bp" -> suggestions.addAll(Global.getSettings().getAllWeaponSpecs().stream().map(it -> it.getWeaponId()).toList());
            case "fighter_bp" -> suggestions.addAll(Global.getSettings().getAllFighterWingSpecs().stream().map(it -> it.getId()).toList());
            case "industry_bp" -> suggestions.addAll(Global.getSettings().getAllIndustrySpecs().stream().map(it -> it.getId()).toList());
            case "modspec" -> suggestions.addAll(Global.getSettings().getAllHullModSpecs().stream().map(it -> it.getId()).toList());
        }

        return suggestions;
    }

    @Override
    public boolean onPreExecute(@NotNull String command, @NotNull String args, @NotNull BaseCommand.CommandContext context, boolean alreadyIntercepted) {
        return false;
    }

    @Override
    public BaseCommand.CommandResult execute(@NotNull String command, @NotNull String args, @NotNull BaseCommand.CommandContext context) {
        return null;
    }

    @Override
    public void onPostExecute(@NotNull String command, @NotNull String args, @NotNull BaseCommand.CommandResult result, @NotNull BaseCommand.CommandContext context, @Nullable CommandListener interceptedBy) {

    }
}
