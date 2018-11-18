package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.SpecialItemSpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lazywizard.console.CommandUtils.findBestStringMatch;

public class AddSpecial implements BaseCommand
{
    public static List<String> getSpecialItemIds()
    {
        final List<String> items = new ArrayList<>();
        for (SpecialItemSpecAPI spec : Global.getSettings().getAllSpecialItemSpecs())
        {
            items.add(spec.getId());
        }
        return items;
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

        String[] tmp = args.split(" ");
        final String data = (tmp.length > 1 ? CollectionUtils.implode(Arrays.asList(
                Arrays.copyOfRange(tmp, 1, tmp.length)), " ") : null);

        String id = findBestStringMatch(tmp[0], getSpecialItemIds());
        if (id == null)
        {
            Console.showMessage("No special item found with id '" + tmp[0]
                    + "'! Use 'list specials' for a complete list of valid ids.");
            return CommandResult.ERROR;
        }

        Global.getSector().getPlayerFleet().getCargo().addSpecial(new SpecialItemData(id, data), 1f);
        Console.showMessage("Added " + id + "'" + (data != null ? " with data '" + data + "'" : "") + " to player inventory.");
        return CommandResult.SUCCESS;
    }
}
