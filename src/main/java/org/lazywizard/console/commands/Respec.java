package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.List;
import java.util.Set;

import static org.lazywizard.console.CommandUtils.findBestOfficerMatch;

public class Respec implements BaseCommand
{
    private static void respecOfficer(OfficerDataAPI toRespec)
    {
        // Technically clone officer, but whatever...
        final PersonAPI oldPerson = toRespec.getPerson(), newPerson = Global.getFactory().createPerson();

        // Copy the old person's memory to the new person
        final MemoryAPI oldMemory = oldPerson.getMemory(), newMemory = newPerson.getMemory();
        newMemory.clear();
        for (String key : oldMemory.getKeys())
        {
            if (oldMemory.getExpire(key) != 0f)
            {
                newMemory.set(key, oldMemory.get(key), oldMemory.getExpire(key));
            }
            else
            {
                newMemory.set(key, oldMemory.get(key));
            }
        }

        // Copy required status of any memory keys
        for (String key : oldMemory.getKeys())
        {
            final Set<String> required = oldMemory.getRequired(key);
            if (!required.isEmpty())
            {
                for (String rKey : required) newMemory.addRequired(key, rKey);
            }
        }

        // Copy traits from old person
        newPerson.setAICoreId(oldPerson.getAICoreId());
        newPerson.setContactWeight(oldPerson.getContactWeight());
        newPerson.setFaction(oldPerson.getFaction().getId());
        newPerson.setName(oldPerson.getName());
        newPerson.setPersonality(oldPerson.getPersonalityAPI().getId());
        newPerson.setPortraitSprite(oldPerson.getPortraitSprite());
        newPerson.setPostId(oldPerson.getPostId());
        newPerson.setRankId(oldPerson.getRankId());
        newPerson.getRelToPlayer().setRel(oldPerson.getRelToPlayer().getRel());

        // Copy any tags from the old person
        newPerson.getTags().clear();
        for (String tag : oldPerson.getTags()) newPerson.addTag(tag);

        // Set the officer's person to the new copy and reset its skill picks
        toRespec.setPerson(newPerson);
        newPerson.getStats().addXP(oldPerson.getStats().getXP());
        newPerson.getStats().addPoints(oldPerson.getStats().getPoints());
        newPerson.getStats().levelUpIfNeeded();
        newPerson.getStats().refreshCharacterStatsEffects();
        toRespec.makeSkillPicks();
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        // Arguments = respec an officer
        if (!args.isEmpty())
        {
            final OfficerDataAPI officer;
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

            final FleetMemberAPI ship = playerFleet.getFleetData().getMemberWithCaptain(officer.getPerson());
            Console.showMessage("Performing respec of " + officer.getPerson().getNameString() + "...");
            respecOfficer(officer);
            if (ship != null) ship.setCaptain(officer.getPerson());
            Console.showMessage("Respec complete.");
            return CommandResult.SUCCESS;
        }

        // No arguments = respec the player
        Console.showMessage("Performing respec...");

        // Refund aptitudes
        final MutableCharacterStatsAPI player = Global.getSector().getPlayerPerson().getStats();
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

        player.refreshCharacterStatsEffects();
        Console.showMessage("Respec complete, refunded " + aptRefunded
                + " aptitude and " + skillRefunded + " skill points.");
        return CommandResult.SUCCESS;
    }
}
