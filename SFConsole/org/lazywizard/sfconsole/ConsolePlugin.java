package org.lazywizard.sfconsole;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.apache.log4j.Level;

public class ConsolePlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        CommandStore.loadCommandsFromCSV("data/console/console_commands.csv");
    }

    @Override
    public void onGameLoad()
    {
        Global.getSector().addScript(new ConsoleScript());
        Global.getLogger(ConsolePlugin.class).log(Level.DEBUG,
                "Added console campaign script.");
    }

    @Override
    public void beforeGameSave()
    {
        Global.getLogger(ConsolePlugin.class).log(Level.DEBUG,
                "Removed console campaign script.");
        Global.getSector().removeScriptsOfClass(ConsoleScript.class);
    }

    @Override
    public void afterGameSave()
    {
        Global.getSector().addScript(new ConsoleScript());
        Global.getLogger(ConsolePlugin.class).log(Level.DEBUG,
                "Added console campaign script.");
    }
}
