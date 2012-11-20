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
        deferredAdd.schedule(new DeferredAdd(), 100);
    }

    private static class DeferredAdd extends TimerTask
    {
        @Override
        public void run()
        {
            StarSystemAPI system = (StarSystemAPI) Global.getSector().getPlayerFleet().getContainingLocation();

            if (system == null)
            {
                throw new RuntimeException("Console could not find a starsystem!");
            }

            Console console = new Console();
            system.addSpawnPoint(console);
            ConsoleTests.runTests(console);
        }
    }
}