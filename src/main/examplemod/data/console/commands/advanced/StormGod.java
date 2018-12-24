package data.console.commands.advanced;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin.CellState;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin.CellStateTracker;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.Iterator;

public class StormGod implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        for (Iterator iter = Global.getSector().getTransientScripts().iterator(); iter.hasNext(); )
        {
            EveryFrameScript script = (EveryFrameScript) iter.next();
            if (script instanceof StormGodScript)
            {
                Global.getSector().removeTransientScript(script);
                Console.showMessage("Hyperspace storms will now challenge you once again.");
                return CommandResult.SUCCESS;
            }
        }

        Global.getSector().addTransientScript(new StormGodScript());
        Console.showMessage("All hyperspace storms will now allow you free passage, as is your right.");
        return CommandResult.SUCCESS;
    }

    private class StormGodScript implements EveryFrameScript
    {
        @Override
        public boolean isDone()
        {
            return false;
        }

        @Override
        public boolean runWhilePaused()
        {
            return false;
        }

        @Override
        public void advance(float amount)
        {
            final CampaignFleetAPI player = Global.getSector().getPlayerFleet();
            if (!player.getContainingLocation().isHyperspace()) return;

            for (Iterator iterator = Global.getSector().getHyperspace().getTerrainCopy().iterator(); iterator.hasNext(); )
            {
                CampaignTerrainAPI terrain = (CampaignTerrainAPI) iterator.next();
                if (terrain.getPlugin() instanceof HyperspaceTerrainPlugin)
                {
                    final HyperspaceTerrainPlugin storms = (HyperspaceTerrainPlugin) terrain.getPlugin();
                    final CellStateTracker cell = storms.getCellAt(player, 50f);
                    if (cell != null) cell.state = CellState.OFF;
                }
            }
        }
    }
}
