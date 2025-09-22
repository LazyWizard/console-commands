package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import static org.lazywizard.console.CommandUtils.*;

public class AddHullmod implements BaseCommandWithSuggestion
{
    public static List<String> getHullMods()
    {
        final List<String> hullmods = new ArrayList<>(Global.getSettings().getAllHullModSpecs().size());
        for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs())
        {
            if (!spec.isHidden())
            {
                hullmods.add(spec.getId());
            }
        }

        return hullmods;
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        final String id = findBestStringMatch(args, getHullMods());
        if (id == null)
        {
            Console.showMessage("No modspec found with id '" + args
                    + "'! Use 'list hullmods' for a complete list of valid ids.");
            return CommandResult.ERROR;
        }

        Global.getSector().getPlayerFleet().getCargo().addHullmods(id, 1);
        Console.showMessage("Added modspec " + id + " to player inventory.");
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        if (parameter != 0) return new ArrayList<>();
        return Global.getSettings().getAllHullModSpecs().stream().map(it -> it.getId()).toList();
    }
}
