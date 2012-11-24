package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("unchecked")
public final class AddConsole implements SectorGeneratorPlugin
{
    @Override
    public void generate(SectorAPI sector)
    {
        Timer deferredAdd = new Timer(true);
        deferredAdd.scheduleAtFixedRate(new DeferredAdd(sector), 100, 100);
    }

    private static class DeferredAdd extends TimerTask
    {
        SectorAPI sector;

        public DeferredAdd(SectorAPI sector)
        {
            this.sector = sector;
        }

        @Override
        public void run()
        {
            StarSystemAPI system = (StarSystemAPI) Global.getSector().getPlayerFleet().getContainingLocation();

            if (system != null)
            {
                system.addSpawnPoint(new Console());
                Console.showMessage("Console successfully activated for this save.");
                this.cancel();
            }
        }
    }
}