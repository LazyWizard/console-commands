package org.lazywizard.console.commands;

import java.util.Arrays;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;

import static com.fs.starfarer.api.impl.campaign.ids.Personalities.*;
import static org.lazywizard.console.CommandUtils.*;

public class AddOfficer implements BaseCommand
{
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
            return runCommand(STEADY + " 1 " + Factions.PLAYER, context);
        }

        final String[] tmp = args.split(" ");
        FullName name = null;
        switch (tmp.length)
        {
            case 1:
                return runCommand(args + " 1 " + Factions.PLAYER, context);
            case 2:
                return runCommand(args + " " + Factions.PLAYER, context);
            case 3:
                break;
            // Custom name support
            case 4:
                name = new FullName(tmp[3], "", Gender.MALE);
                break;
            default:
                name = new FullName(tmp[3], CollectionUtils.implode(Arrays.asList(
                        Arrays.copyOfRange(tmp, 4, tmp.length)), " "), Gender.MALE);
        }

        // Verify personality
        String personality;
        switch (tmp[0].toLowerCase())
        {
            case TIMID:
                personality = TIMID;
                break;
            case CAUTIOUS:
                personality = CAUTIOUS;
                break;
            case STEADY:
                personality = STEADY;
                break;
            case AGGRESSIVE:
                personality = AGGRESSIVE;
                break;
            case RECKLESS:
                personality = RECKLESS;
                break;
            default:
                Console.showMessage("Unsupported personality: '" + tmp[0] + "'.");
                return CommandResult.ERROR;
        }

        if (!isInteger(tmp[1]))
        {
            Console.showMessage("Error: starting level must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        final int level = Integer.parseInt(tmp[1]);
        final FactionAPI faction = findBestFactionMatch(tmp[2]);
        if (faction == null)
        {
            Console.showMessage("No faction found with id '" + tmp[2] + "'!");
            return CommandResult.ERROR;
        }

        final PersonAPI person = OfficerManagerEvent.createOfficer(faction, 1, null, false, null, false, false, -1, MathUtils.getRandom());;
        final FleetDataAPI fleetData = Global.getSector().getPlayerFleet().getFleetData();
        final OfficerLevelupPlugin plugin
                = (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");
        person.setPersonality(personality);
        if (name != null)
        {
            person.setName(name);
        }
        fleetData.addOfficer(person);
        fleetData.getOfficerData(person).addXP(plugin.getXPForLevel(level));

        Console.showMessage("Created " + personality + " officer " + person.getName().getFullName() + ".");
        return CommandResult.SUCCESS;
    }
}
