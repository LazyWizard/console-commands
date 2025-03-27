package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.CustomRepImpact;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel.MissionResult;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel.MissionState;
import com.fs.starfarer.api.impl.campaign.intel.FactionCommissionIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.Commission;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class SetCommission implements BaseCommand
{
    /**
     * Ends and returns the player's current commission (if one exists; returns {@code null} otherwise).
     *
     * @return The faction the player used to work with, or {@code null} if they didn't have a commission.
     */
    public static FactionAPI endCurrentCommission()
    {
        final FactionAPI curFaction = Misc.getCommissionFaction();
        if (curFaction != null)
        {
            final FactionCommissionIntel intel = Misc.getCommissionIntel();
            final MissionResult result = intel.createResignedCommissionResult(true, true, null);
            intel.setMissionResult(result);
            intel.setMissionState(MissionState.ABANDONED);
            intel.endMission();
        }

        return curFaction;
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

        if (args.equalsIgnoreCase("none"))
        {
            final FactionAPI curFaction = endCurrentCommission();
            if (curFaction == null)
            {
                Console.showMessage("You aren't under a commission!");
                return CommandResult.ERROR;
            }

            Console.showMessage("Ended existing commission with " + curFaction.getDisplayNameWithArticle() + ".");
            return CommandResult.SUCCESS;
        }

        FactionAPI newFaction = CommandUtils.findBestFactionMatch(args);
        if (newFaction == null)
        {
            Console.showMessage("No such faction '" + args + "'!");
            return CommandResult.ERROR;
        }
        else if (newFaction.isPlayerFaction())
        {
            Console.showMessage("You can't work for yourself! If you want to end"
                    + " your current commission, pass in the argument \"none\".");
            return CommandResult.ERROR;
        }
        else if (!newFaction.getCustomBoolean(Factions.CUSTOM_OFFERS_COMMISSIONS))
        {
            Console.showMessage("That faction doesn't offer commissions!");
            return CommandResult.ERROR;
        }
        else if (newFaction == Misc.getCommissionFaction())
        {
            Console.showMessage("You already have a commission with that faction!");
            return CommandResult.ERROR;
        }

        final FactionAPI oldFaction = endCurrentCommission();
        if (oldFaction != null)
        {
            Console.showMessage("Ended existing commission with " + oldFaction.getDisplayNameWithArticle());
        }

        if (!newFaction.getRelToPlayer().isAtWorst(Commission.COMMISSION_REQ))
        {
            final CustomRepImpact impact = new CustomRepImpact();
            impact.ensureAtWorst = Commission.COMMISSION_REQ;
            Global.getSector().adjustPlayerReputation(new RepActionEnvelope(RepActions.CUSTOM, impact), newFaction.getId());
            Console.showMessage("Relationship with " + newFaction.getDisplayNameWithArticle()
                    + " boosted to minimum commission rep.");
        }

        new FactionCommissionIntel(newFaction).missionAccepted();
        Console.showMessage("Began commission with " + newFaction.getDisplayNameWithArticle() + ".");
        return CommandResult.SUCCESS;
    }
}
