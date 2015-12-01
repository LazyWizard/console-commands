package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import static com.fs.starfarer.api.impl.campaign.ids.Personalities.*;

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
            return runCommand("steady 1 player", context);
        }

        final String[] tmp = args.split(" ");
        // TODO: Add support for custom officer name/gender
        if (tmp.length > 3)
        {
            return CommandResult.BAD_SYNTAX;
        }

        if (tmp.length == 1)
        {
            return runCommand(args + " 1 player", context);
        }

        if (tmp.length == 2)
        {
            return runCommand(args + " player", context);
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
            default:
                Console.showMessage("Unsupported personality: '" + tmp[0] + "'.");
                return CommandResult.ERROR;
        }

        int level;
        try
        {
            level = Integer.parseInt(tmp[1]);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: starting level must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        final FactionAPI faction = CommandUtils.findBestFactionMatch(tmp[2]);
        if (faction == null)
        {
            Console.showMessage("No faction found with id '" + tmp[2] + "'!");
            return CommandResult.ERROR;
        }

        final PersonAPI person = OfficerManagerEvent.createOfficer(faction, level, false);
        person.setPersonality(personality);
        Global.getSector().getPlayerFleet().getFleetData().addOfficer(person);

        Console.showMessage("Created officer " + person.getName().getFullName() + ".");
        return CommandResult.SUCCESS;
    }
}
