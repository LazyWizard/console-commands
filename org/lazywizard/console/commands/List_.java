package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.StringUtils;

public class List_ implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context != CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        // Get all valid IDs for the specified type
        args = args.toLowerCase();
        SectorAPI sector = Global.getSector();
        List<String> ids;
        switch (args)
        {
            case "ships":
            case "hulls":
                ids = new ArrayList<>();
                for (String id : sector.getAllEmptyVariantIds())
                {
                    ids.add(id.substring(0, id.lastIndexOf("_Hull")));
                }
                break;
            case "wings":
            case "fighters":
            case "squadrons":
                ids = sector.getAllFighterWingIds();
                break;
            case "weapons":
                ids = sector.getAllWeaponIds();
                break;
            case "systems":
            case "locations":
                ids = new ArrayList<>();
                ids.add(sector.getHyperspace().getId());
                for (LocationAPI location : sector.getStarSystems())
                {
                    ids.add(location.getId());
                }
                break;
            case "factions":
                ids = new ArrayList<>();
                for (FactionAPI faction : sector.getAllFactions())
                {
                    ids.add(faction.getId());
                }
                break;
            default:
                return CommandResult.BAD_SYNTAX;
        }

        // Format and print the list of valid IDs
        Collections.sort(ids);
        String results = CollectionUtils.implode(ids);
        Console.showMessage("Known " + args + ":\n"
                + StringUtils.indent(StringUtils.wrapString(results,
                                Console.getSettings().getMaxOutputLineLength() - 3), "   "));
        return CommandResult.SUCCESS;
    }
}
