package data.scripts.console;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.StarSystemAPI;

@SuppressWarnings("unchecked")
public final class AddConsole implements SectorGeneratorPlugin
{
    @Override
    public void generate(SectorAPI sector)
    {
        StarSystemAPI system = sector.getStarSystem("Corvus");

        ConsoleManager consoleManager = new ConsoleManager();
        system.addSpawnPoint(consoleManager);

        Console.setManager(consoleManager);

        ConsoleTests.runTests(consoleManager);
    }
}