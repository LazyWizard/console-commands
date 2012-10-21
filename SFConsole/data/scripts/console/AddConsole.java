package data.scripts.console;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("unchecked")
public final class AddConsole implements SectorGeneratorPlugin
{
    private static final String[] SYSTEM_NAMES =
    {
        "Corvus", // Vanilla
        "Caelus", // Project CAELUS
        "Barnard's Star", // Project Ironclads
        "Xplo", // Fight for Universe: Sector Xplo
        "God" // Ascendency
    };

    @Override
    public void generate(SectorAPI sector)
    {
        Timer deferredAdd = new Timer(true);
        deferredAdd.schedule(new DeferredAdd(sector), 100);
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
            StarSystemAPI system;

            for (int x = 0; x < SYSTEM_NAMES.length; x++)
            {
                system = sector.getStarSystem(SYSTEM_NAMES[x]);

                if (system == null)
                {
                    continue;
                }

                ConsoleManager consoleManager = new ConsoleManager(system);
                system.addSpawnPoint(consoleManager);
                return;
            }

            throw new RuntimeException("Console could not find a starsystem!");
        }
    }
}