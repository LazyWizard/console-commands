package org.lazywizard.console.commands;

import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommandUtils;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollectionUtils;

public class SetFaction implements BaseCommand
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

        final FactionAPI towardsFaction = CommandUtils.findBestFactionMatch(args);
        if (towardsFaction == null)
        {
            final List<String> ids = new ArrayList<>();
            for (FactionAPI faction : Global.getSector().getAllFactions())
            {
                ids.add(faction.getId());
            }

            Console.showMessage("Error: no such faction '" + args
                    + "'! Valid factions: " + CollectionUtils.implode(ids) + ".");
            return CommandResult.ERROR;
        }

        Global.getSector().getPlayerFleet().setFaction(towardsFaction.getId());

        if (towardsFaction.isPlayerFaction())
        {
            Console.showMessage("Reset player faction.");
            Global.getSector().removeScriptsOfClass(FactionRelationMirror.class);
        }
        else
        {
            Console.showMessage("Set player faction to " + towardsFaction.getDisplayName() + ".");
            Global.getSector().addTransientScript(new FactionRelationMirror());
        }

        return CommandResult.SUCCESS;
    }

    private static class FactionRelationMirror implements EveryFrameScript
    {
        private float nextCheck = 0f;

        @Override
        public boolean isDone()
        {
            return false;
        }

        @Override
        public boolean runWhilePaused()
        {
            return true;
        }

        @Override
        public void advance(float amount)
        {
            nextCheck -= amount;
            if (nextCheck <= 0f)
            {
                nextCheck = 2f;

                final FactionAPI playerFaction = Global.getSector().getFaction("player"),
                        actualPlayerFaction = Global.getSector().getPlayerFleet().getFaction();
                for (FactionAPI faction : Global.getSector().getAllFactions())
                {
                    if (faction == playerFaction || faction == actualPlayerFaction)
                    {
                        continue;
                    }

                    playerFaction.setRelationship(faction.getId(),
                            actualPlayerFaction.getRelationship(faction.getId()));
                }
            }
        }
    }
}
