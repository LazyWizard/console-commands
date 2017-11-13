package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CharacterDataAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

public class AllHullmods implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final List<String> unlocked = new ArrayList<>();
        final CharacterDataAPI player = Global.getSector().getCharacterData();
        for (HullModSpecAPI spec : Global.getSettings().getAllHullModSpecs())
        {
            if (!spec.isHidden() && !spec.isAlwaysUnlocked()
                    && !player.knowsHullMod(spec.getId()))
            {
                player.addHullMod(spec.getId());
                unlocked.add(spec.getDisplayName());
            }
        }

        if (unlocked.isEmpty())
        {
            Console.showMessage("You already know all unlockable hullmods!");
            return CommandResult.SUCCESS;
        }

        Collections.sort(unlocked);
        Console.showMessage("Unlocked " + unlocked.size() + " hullmods: "
                + CollectionUtils.implode(unlocked) + ".");
        return CommandResult.SUCCESS;
    }
}
