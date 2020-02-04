package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.List;

import static org.lazywizard.console.CommandUtils.findBestOfficerMatch;

public class AllOfficerSkills implements BaseCommand
{
    private static void giveSkills(OfficerDataAPI officer)
    {
        final MutableCharacterStatsAPI stats = officer.getPerson().getStats();

        for (final String skill : Global.getSettings().getSkillIds())
        {
            // Ignore skills that NPC officers can't learn
            if (Global.getSettings().getSkillSpec(skill).isCombatOfficerSkill())
            {
                stats.setSkillLevel(skill, 3f);
            }
        }

        stats.refreshCharacterStatsEffects();
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
            Console.showMessage("Enter the name or number of the officer to give skills to. Use 'list officers' for a list.");
            return CommandResult.BAD_SYNTAX;
        }

        OfficerDataAPI officer;
        final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (CommandUtils.isInteger(args))
        {
            final int num = Integer.parseInt(args);
            final List<OfficerDataAPI> officers = playerFleet.getFleetData().getOfficersCopy();
            if (num > officers.size())
            {
                Console.showMessage("There are only " + officers.size() + " officers in your fleet!");
                return CommandResult.ERROR;
            }

            officer = officers.get(num - 1);
        }
        else
        {
            officer = findBestOfficerMatch(args, playerFleet);
            if (officer == null)
            {
                Console.showMessage("No officer found with name '" + args + "'! Use 'list officers' for a complete list.");
                return CommandResult.ERROR;
            }
        }

        officer = playerFleet.getFleetData().getOfficerData(officer.getPerson());
        giveSkills(officer);
        Console.showMessage("Granted all skills to " + officer.getPerson().getNameString() + ".");
        return CommandResult.SUCCESS;
    }
}
