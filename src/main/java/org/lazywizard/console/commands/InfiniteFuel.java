package org.lazywizard.console.commands;

import java.lang.ref.WeakReference;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class InfiniteFuel implements BaseCommand
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
            Global.getSector().removeTransientScriptsOfClass(InfiniteFuelScript.class);
            lastSector.clear();
            Console.showMessage("Infinite fuel disabled.");
            return CommandResult.SUCCESS;
        }

        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        Global.getSector().addTransientScript(new InfiniteFuelScript(player));
        lastSector = new WeakReference<>(Global.getSector());
        Console.showMessage("Infinite fuel enabled.");
        return CommandResult.SUCCESS;
    }

    private static class InfiniteFuelScript implements EveryFrameScript
    {
        private final CampaignFleetAPI fleet;
        private float fuel;

        private InfiniteFuelScript(CampaignFleetAPI fleet)
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
            float currentFuel = fleet.getCargo().getFuel();

            // Support for market transactions and quest rewards
            if (Global.getSector().isPaused() || currentFuel > fuel)
            {
                fuel = currentFuel;
                return;
            }

            fleet.getCargo().addFuel(fuel - fleet.getCargo().getFuel());
        }
    }
}
