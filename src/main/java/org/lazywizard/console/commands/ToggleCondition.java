package org.lazywizard.console.commands;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.impl.campaign.procgen.ConditionGenDataSpec;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.*;

import static org.lazywizard.console.CommandUtils.findBestMarketConditionMatch;

public class ToggleCondition implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInMarket())
        {
            Console.showMessage(CommonStrings.ERROR_MARKET_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final MarketAPI market = context.getMarket();
        if (args.isEmpty())
        {
            final List<String> conditions = new ArrayList<>();
            for (MarketConditionAPI condition : market.getConditions())
            {
                conditions.add(condition.getId());
            }

            Collections.sort(conditions);
            Console.showMessage("Conditions of current market: " + CollectionUtils.implode(conditions) + ".");
            return CommandResult.SUCCESS;
        }

        final MarketConditionSpecAPI spec = findBestMarketConditionMatch(args);
        if (spec == null)
        {
            Console.showMessage("No market condition found with id '" + args + "'! Use 'list conditions' for a list of valid IDs.");
            return CommandResult.ERROR;
        }

        // If condition already exists, remove it
        final String id = spec.getId();
        if (market.hasCondition(id))
        {
            market.removeCondition(id);
            market.reapplyConditions();
            Console.showMessage("Removed condition '" + id + "' from market '" + market.getName() + "'.");
            return CommandResult.SUCCESS;
        }

        // Create condition and mark any existing conditions that conflict with it for later removal
        final MarketConditionAPI condition = market.getSpecificCondition(market.addCondition(id));
        final ConditionGenDataSpec gen = condition.getGenSpec();
        final Set<String> toRemove = new HashSet<>();
        if (gen != null)
        {
            final Set<String> mutuallyExclusive = gen.getRequiresNotAny();
            for (MarketConditionAPI otherCon : market.getConditions())
            {
                if (otherCon == condition) continue;

                // Automatically remove any mutually exclusive conditions
                if (mutuallyExclusive.contains(otherCon.getId()))
                {
                    toRemove.add(otherCon.getId());
                    Console.showMessage("Removed mutually-exclusive condition '" + otherCon.getId()
                            + "' from market '" + market.getName() + "'.");
                    continue;
                }

                // Only allow one condition from the same condition group
                final ConditionGenDataSpec otherGen = otherCon.getGenSpec();
                if (otherGen != null && gen.getGroup().equals(otherGen.getGroup()))
                {
                    toRemove.add(otherCon.getId());
                    Console.showMessage("Removed existing condition '" + otherCon.getId()
                            + "' of same type from market '" + market.getName() + "'.");
                }
            }
        }

        // Only allow one population condition
        if (id.startsWith("population_"))
        {
            for (MarketConditionAPI otherCon : market.getConditions())
            {
                if ((otherCon != condition) && otherCon.getId().startsWith("population_"))
                {
                    toRemove.add(otherCon.getId());
                    Console.showMessage("Removed existing population condition '" + otherCon.getId()
                            + "' from market '" + market.getName() + "'.");
                }
            }
        }

        // Remove all conflicting conditions
        for (String tmp : toRemove) market.removeCondition(tmp);

        // Ensure new condition is visible if market has already been surveyed
        if (market.getSurveyLevel() == SurveyLevel.FULL && condition.requiresSurveying())
        {
            condition.setSurveyed(true);
        }

        market.reapplyConditions();
        Console.showMessage("Added condition '" + id + "' to market '" + market.getName() + "'.");
        return CommandResult.SUCCESS;
    }
}
