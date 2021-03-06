package org.lazywizard.console.commands;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.characters.MarketConditionSpecAPI;
import com.fs.starfarer.api.impl.campaign.procgen.ConditionGenDataSpec;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lazywizard.console.CommandUtils.findBestMarketConditionMatch;

public class AddCondition implements BaseCommand
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

            Collections.sort(conditions, String.CASE_INSENSITIVE_ORDER);
            Console.showMessage("Conditions of current market: " + CollectionUtils.implode(conditions) + ".");
            return CommandResult.SUCCESS;
        }

        final MarketConditionSpecAPI spec = findBestMarketConditionMatch(args);
        if (spec == null)
        {
            Console.showMessage("No market condition found with id '" + args + "'! Use 'list conditions' for a list of valid IDs.");
            return CommandResult.ERROR;
        }

        // Check if condition already exists
        final String id = spec.getId();
        if (market.hasCondition(id))
        {
            Console.showMessage("Condition '" + id + "' already exists in market '" + market.getName()
                    + "'. Use RemoveCondition if you want to remove it.");
            return CommandResult.ERROR;
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
                final String otherId = otherCon.getId();
                if (mutuallyExclusive.contains(otherCon.getId()))
                {
                    toRemove.add(otherId);
                    Console.showMessage("Removed mutually-exclusive condition '" + otherId
                            + "' from market '" + market.getName() + "'.");
                    continue;
                }

                // Only allow one condition from the same condition group
                final ConditionGenDataSpec otherGen = otherCon.getGenSpec();
                if (otherGen != null && gen.getGroup().equals(otherGen.getGroup()))
                {
                    toRemove.add(otherId);
                    Console.showMessage("Removed existing condition '" + otherId
                            + "' of same type from market '" + market.getName() + "'.");
                }
            }
        }

        // The population is the same as the market size, so redirect that condition to SetMarketSize
        final Pattern populationRegex = Pattern.compile("^population_(\\d+)$");
        final Matcher matcher = populationRegex.matcher(id);
        if (matcher.matches())
        {
            return new SetMarketSize().runCommand(matcher.group(1), context);
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
