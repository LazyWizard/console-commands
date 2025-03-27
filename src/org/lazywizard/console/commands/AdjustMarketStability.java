package org.lazywizard.console.commands;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.findBestMarketMatch;
import static org.lazywizard.console.CommandUtils.isInteger;

// TODO: Test whether this sticks, add to changelog
public class AdjustMarketStability implements BaseCommand
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
            return CommandResult.BAD_SYNTAX;
        }

        String[] tmp = args.split(" ");
        if (tmp.length != 2)
        {
            return CommandResult.BAD_SYNTAX;
        }

        // Support for reversed arguments
        int amount;
        if (isInteger(tmp[1]))
        {
            amount = Integer.parseInt(tmp[1]);
        }
        else
        {
            if (!isInteger(tmp[0]))
            {
                return CommandResult.BAD_SYNTAX;
            }

            amount = Integer.parseInt(tmp[0]);
            tmp[0] = tmp[1];
        }

        final MarketAPI market = findBestMarketMatch(tmp[0]);
        if (market == null)
        {
            Console.showMessage("No market found with id '" + tmp[0]
                    + "'! Use 'list markets' for a complete list of valid ids.");
            return CommandResult.ERROR;
        }

        final MutableStat stability = market.getStability();
        final MutableStat.StatMod existingBonus = stability.getFlatStatMod(CommonStrings.MOD_ID);
        stability.modifyFlat(CommonStrings.MOD_ID, amount + (existingBonus != null ? existingBonus.value : 0f));
        Console.showMessage("Stability of market '" + market.getId() + "' adjusted by " + amount
                + ", now at " + Math.min(10, Math.max(0, (int) stability.getModifiedValue())) + ".");
        return CommandResult.SUCCESS;
    }
}
