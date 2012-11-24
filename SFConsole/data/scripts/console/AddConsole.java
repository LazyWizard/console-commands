package data.scripts.console;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
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
        //JOptionPane.showMessageDialog(null, "Entered generate().");
        Timer deferredAdd = new Timer(true);
        deferredAdd.scheduleAtFixedRate(new DeferredAdd(sector), 1000, 1000);
    }

    private static class DeferredAdd extends TimerTask
    {
        SectorAPI sector;

        public DeferredAdd(SectorAPI sector)
        {
            //JOptionPane.showMessageDialog(null, "Created task.");
            this.sector = sector;
        }

        @Override
        public void run()
        {
            //JOptionPane.showMessageDialog(null, "Entered run().");
            CampaignFleetAPI player = sector.getPlayerFleet();

            if (player != null)
            {
                //JOptionPane.showMessageDialog(null, "Player was not null.");
                StarSystemAPI system = (StarSystemAPI) player.getContainingLocation();

                if (system != null)
                {
                    system.addSpawnPoint(new Console());
                    Console.showMessage("Console successfully activated for this save.");
                    //JOptionPane.showMessageDialog(null, "Console added to system "
                    //        + system.getName() + ".");
                    this.cancel();
                }
            }
        }
    }
}