package org.lazywizard.console.commands;

import java.lang.ref.WeakReference;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class InfiniteSupplies implements BaseCommand
{
    private static WeakReference<SectorAPI> lastSector = new WeakReference<>(null);

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        SectorAPI tmp = lastSector.get();
        if (tmp != null && tmp == Global.getSector())
        {
            Global.getSector().removeTransientScriptsOfClass(InfiniteSuppliesScript.class);
            lastSector.clear();
            Console.showMessage("Infinite supplies disabled.");
            return CommandResult.SUCCESS;
        }

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        Global.getSector().addTransientScript(new InfiniteSuppliesScript(player));
        lastSector = new WeakReference<>(Global.getSector());
        Console.showMessage("Infinite supplies enabled.");
        return CommandResult.SUCCESS;
    }

    private static class InfiniteSuppliesScript implements EveryFrameScript
    {
        private final CampaignFleetAPI fleet;
        private float supplies;

        private InfiniteSuppliesScript(CampaignFleetAPI fleet)
        {
            this.fleet = fleet;
        }

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
            float currentSupplies = fleet.getCargo().getSupplies();

            // Support for market transactions and quest rewards
            if (Global.getSector().isPaused() || currentSupplies > supplies)
            {
                supplies = currentSupplies;
                return;
            }

            fleet.getCargo().addSupplies(supplies - fleet.getCargo().getSupplies());
        }
    }
}
