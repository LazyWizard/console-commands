package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import data.scripts.console.commands.AddSWShips;

abstract class ConsoleTests
{
    static void runTests(Console console)
    {
        try
        {
            console.registerCommand(AddSWShips.class);
        }
        catch (Exception ex)
        {
        }
        
        Console.addScript("Test", new Script()
        {
            @Override
            public void run()
            {
                Global.getSector().addMessage("Scriiiiiipt!");
            }
        });
    }
}
