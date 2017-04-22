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
        final MutableCharacterStatsAPI player
                = Global.getSector().getPlayerPerson().getStats();
        int aptRefunded = 0;
        for (final String aptitude : Global.getSettings().getAptitudeIds())
        {
            final int total = (int) player.getAptitudeLevel(aptitude);
            if (total > 0)
            {
                Console.showMessage(" - removed " + total + " points from aptitude " + aptitude);
                player.setAptitudeLevel(aptitude, 0f);
                player.addPoints(total);
                aptRefunded += total;
            }
        }

        // Refund skills
        int skillRefunded = 0;
        for (final String skill : Global.getSettings().getSortedSkillIds())
        {
            // Ignore aptitudes (included in list because officers treat them as skills)
            if (Global.getSettings().getSkillSpec(skill).isAptitudeEffect())
            {
                continue;
            }

            final int total = (int) player.getSkillLevel(skill);
            if (total > 0)
            {
                Console.showMessage(" - removed " + total + " points from skill " + skill);
                player.setSkillLevel(skill, 0f);
                player.addPoints(total);
                skillRefunded += total;
            }
        }

        Console.showMessage("Respec complete, refunded " + aptRefunded
                + " aptitude and " + skillRefunded + " skill points.");
        return CommandResult.SUCCESS;
    }
}
