package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.OfficerDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.lazywizard.console.CommandUtils.findBestOfficerMatch;

public class Respec implements BaseCommand
{
    private static int respecOfficerTest(OfficerDataAPI toRespec, CampaignFleetAPI sourceFleet)
    {
        final PersonAPI person = toRespec.getPerson();
        final MutableCharacterStatsAPI stats = person.getStats();
        final FleetMemberAPI ship = sourceFleet.getFleetData().getMemberWithCaptain(person);

        // Show skills that were reset
        final List<SkillLevelAPI> skills = stats.getSkillsCopy();
        Collections.sort(skills, new SkillLevelComparator());

        // Notify player of respecced skills
        int totalRefunded = 0;
        for (SkillLevelAPI skill : skills)
        {
            final int refunded = (int) skill.getLevel();
            if (refunded > 0)
            {
                Console.showMessage(" - removed " + refunded + " points from " + (skill.getSkill().isAptitudeEffect()
                        ? "aptitude " : "skill ") + skill.getSkill().getId());
                totalRefunded += refunded;
                stats.setSkillLevel(skill.getSkill().getId(), 0);
                //skill.setLevel(0);
            }
        }

        final long xp = stats.getXP(), bxp = stats.getBonusXp();
        stats.addXP(-xp);
        stats.setBonusXp(0);
        stats.setLevel(0);
        stats.refreshCharacterStatsEffects();
        stats.addXP(xp);
        stats.setBonusXp(bxp);
        stats.addPoints(totalRefunded);
        stats.refreshCharacterStatsEffects();

        // Refresh any commander skills affecting this officer
        if (sourceFleet.getCommanderStats() != null)
        {
            sourceFleet.getCommanderStats().refreshCharacterStatsEffects();
        }

        return totalRefunded;
    }

    private static int respecOfficer(OfficerDataAPI toRespec, CampaignFleetAPI sourceFleet)
    {
        // Technically it should be called cloneOfficer(), but whatever...
        final PersonAPI oldPerson = toRespec.getPerson(),
                newPerson = OfficerManagerEvent.createOfficer(oldPerson.getFaction(), 1, SkillPickPreference.ANY,
                        false, null, true, false, -1, MathUtils.getRandom());
        final FleetMemberAPI ship = sourceFleet.getFleetData().getMemberWithCaptain(oldPerson);

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
        newPerson.setFleet(oldPerson.getFleet());
        newPerson.setGender(oldPerson.getGender());
        newPerson.setId(oldPerson.getId());
        newPerson.setImportance(oldPerson.getImportance());
        newPerson.setMarket(oldPerson.getMarket());
        newPerson.setName(oldPerson.getName());
        newPerson.setPersonality(oldPerson.getPersonalityAPI().getId());
        newPerson.setPortraitSprite(oldPerson.getPortraitSprite());
        newPerson.setPostId(oldPerson.getPostId());
        newPerson.setRankId(oldPerson.getRankId());
        newPerson.getRelToPlayer().setRel(oldPerson.getRelToPlayer().getRel());
        newPerson.setVoice(oldPerson.getVoice());

        // Copy any tags from the old person
        newPerson.getTags().clear();
        for (String tag : oldPerson.getTags()) newPerson.addTag(tag);

        // Show skills that were reset
        final List<SkillLevelAPI> skills = oldPerson.getStats().getSkillsCopy();
        Collections.sort(skills, new SkillLevelComparator());

        // Notify player of respecced skills
        int totalRefunded = 0;
        for (SkillLevelAPI skill : skills)
        {
            final int skillLevel = (int) skill.getLevel();
            if (skillLevel <= 0) continue;

            totalRefunded++;
            final String skillId = skill.getSkill().getId();
            Console.showMessage(" - removed " + (skill.getSkill().isAptitudeEffect() ? "aptitude " : "skill ") + skillId);
            if (skillLevel > 1)
            {
                Console.showMessage(" - refunded story point for elite skill " + skillId);
                Global.getSector().getPlayerStats().addStoryPoints(1);
            }
        }

        // Set the officer's person to the new copy and give it the proper amount of experience
        toRespec.setPerson(newPerson);
        if (ship != null) ship.setCaptain(newPerson);
        toRespec.addXP(oldPerson.getStats().getXP());
        newPerson.getStats().refreshCharacterStatsEffects();

        // Refresh any commander skills affecting this officer
        if (sourceFleet.getCommanderStats() != null)
        {
            sourceFleet.getCommanderStats().refreshCharacterStatsEffects();
        }

        return totalRefunded;
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


            Console.showMessage("Performing respec of " + officer.getPerson().getNameString() + "...");
            final int totalRefunded = respecOfficer(officer, playerFleet);
            Console.showMessage("Respec complete, refunded " + totalRefunded + " skill points.");
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

    private static class SkillLevelComparator implements Comparator<SkillLevelAPI>
    {
        @Override
        public int compare(SkillLevelAPI o1, SkillLevelAPI o2)
        {
            final SkillSpecAPI skill1 = o1.getSkill(), skill2 = o2.getSkill();
            if (skill1.isAptitudeEffect() && !skill2.isAptitudeEffect())
            {
                return -1;
            }
            else if (skill2.isAptitudeEffect() && !skill1.isAptitudeEffect())
            {
                return 1;
            }
            else
            {
                return skill1.getId().compareTo(skill2.getId());
            }
        }
    }
}
