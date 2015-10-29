package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Respec implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        Console.showMessage("Performing respec...");

        // Refund aptitudes
        int total;
        MutableCharacterStatsAPI player = Global.getSector().getPlayerFleet().getCommanderStats();
        for (String currId : Global.getSettings().getSortedAbilityIds())
        {
            total = Math.round(player.getAptitudeLevel(currId));
            if (total > 0)
            {
                Console.showMessage(" - removed " + total + " points from aptitude " + currId);
                player.setAptitudeLevel(currId, 0f);
                player.addAptitudePoints(total);
            }
        }

        // Refund skills
        for (String currId : Global.getSettings().getSortedSkillIds())
        {
            total = Math.round(player.getSkillLevel(currId));
            if (total > 0)
            {
                Console.showMessage(" - removed " + total + " points from skill " + currId);
                player.setSkillLevel(currId, 0f);
                player.addSkillPoints(total);
            }
        }

        Console.showMessage("Respec complete.");
        return CommandResult.SUCCESS;
    }
}
