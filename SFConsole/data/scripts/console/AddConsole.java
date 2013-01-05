package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;

@SuppressWarnings("unchecked")
public final class AddConsole implements SectorGeneratorPlugin
{
    @Override
    public void generate(SectorAPI sector)
    {
        StarSystemAPI system = (StarSystemAPI) Global.getSector().getStarSystems().get(0);

        if (system != null)
        {
            system.addSpawnPoint(new Console());
            Console.showMessage("Console successfully activated for this save.");
        }
    }
}