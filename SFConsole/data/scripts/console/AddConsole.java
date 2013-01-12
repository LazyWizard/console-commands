package data.scripts.console;

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
        if (sector.getStarSystems().isEmpty())
        {
            Timer deferredAdd = new Timer(true);
            deferredAdd.scheduleAtFixedRate(new DeferredAdd(sector), 1000, 1000);
        }
        else
        {
            StarSystemAPI system = (StarSystemAPI) sector.getStarSystems().get(0);
            system.addSpawnPoint(new Console());
            Console.showMessage("Console successfully activated for this save.");
        }
    }

    private static class DeferredAdd extends TimerTask
    {
        private SectorAPI sector;

        public DeferredAdd(SectorAPI sector)
        {
            this.sector = sector;
        }

        @Override
        public void run()
        {
            if (!sector.getStarSystems().isEmpty())
            {
                StarSystemAPI system = (StarSystemAPI) sector.getStarSystems().get(0);
                system.addSpawnPoint(new Console());
                Console.showMessage("Console successfully activated for this save.");
                cancel();
            }
        }
    }
}