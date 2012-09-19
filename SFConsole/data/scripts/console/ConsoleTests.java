package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import data.scripts.console.commands.AddSWShips;

class ConsoleTests
{
    static void runTests(ConsoleManager consoleManager)
    {
        consoleManager.registerCommand(AddSWShips.class);
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
