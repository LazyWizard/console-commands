package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static org.lazywizard.console.CommandUtils.findBestLocationMatch;

public class Survey implements BaseCommand
{
    private static int surveyLocation(LocationAPI loc)
    {
        int total = 0;
        final StringBuilder sb = new StringBuilder("Surveying " + loc.getName() + "...");
        for (SectorEntityToken token : loc.getAllEntities())
        {
            final MarketAPI market = token.getMarket();
            if (market != null && market.getSurveyLevel() != SurveyLevel.FULL)
            {
                //market.setSurveyLevel(SurveyLevel.FULL);
                total++;
                Misc.setFullySurveyed(market, null, true);
                sb.append("\n - " + token.getFullName() + " in " + loc.getName());
            }
        }

        if (total > 0) Console.showMessage(sb.toString());
        return total;
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if ("all".equals(args.toLowerCase()))
        {
            int totalSurveyed = 0;
            for (LocationAPI loc : Global.getSector().getAllLocations())
            {
                final int surveyedInLoc = surveyLocation(loc);
                if (surveyedInLoc > 0)
                {
                    Console.showMessage("Surveyed " + surveyedInLoc + " markets in " + loc.getName() + ".");
                    totalSurveyed += surveyedInLoc;
                }
            }

            Console.showMessage("Surveyed " + totalSurveyed + " markets in " + Global.getSector().getAllLocations().size() + " systems.");
            return CommandResult.SUCCESS;
        }

        // No argument = survey current location
        if (args.isEmpty()) args = Global.getSector().getCurrentLocation().getId();

        final LocationAPI loc = findBestLocationMatch(args);
        if (loc == null)
        {
            Console.showMessage("No system found with name or id '" + args + "'! Use 'list systems' for a complete list.");
            return CommandResult.ERROR;
        }

        Console.showMessage("Surveyed " + surveyLocation(loc) + " markets in " + loc.getName() + ".");
        return CommandResult.SUCCESS;
    }
}
