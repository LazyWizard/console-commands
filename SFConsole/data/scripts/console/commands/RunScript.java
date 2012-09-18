package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import data.scripts.console.BaseCommand;
import java.util.*;

public class RunScript extends BaseCommand
{
    private static Map allowedScripts = Collections.synchronizedMap(new HashMap());

    static
    {
        reloadScripts();
    }

    public static void addScript(String name, Script script)
    {
        allowedScripts.put(name, script);
        saveScripts();
    }

    private static void saveScripts()
    {
        setVar("UserScripts", allowedScripts);
    }

    private static void reloadScripts()
    {
        if (hasVar("UserScripts"))
        {
            allowedScripts = (Map) getVar("UserScripts");
        }
    }

    @Override
    protected String getHelp()
    {
        return "Valid arguments:\n  <scriptname> (runs that script)\n  list "
                + "(lists all available scripts)\n  help (shows this helpfile)";
    }

    @Override
    protected String getSyntax()
    {
        return "runscript <scriptname>|list";
    }

    @Override
    public boolean runCommand(String args)
    {
        if (args.equals("help"))
        {
            showHelp();
            return true;
        }

        if (args.equals("list"))
        {
            if (allowedScripts.isEmpty())
            {
                showMessage("Scripts: none");
                return true;
            }

            String[] scripts = (String[]) allowedScripts.keySet().toArray(
                    new String[allowedScripts.keySet().size()]);
            StringBuilder allScripts = new StringBuilder();

            for (int x = 0; x < scripts.length; x++)
            {
                if (x > 0)
                {
                    allScripts.append(", ");
                }

                allScripts.append(scripts[x]);
            }

            showMessage("Scripts: " + allScripts.toString());

            return true;
        }

        if (allowedScripts.keySet().contains(args))
        {
            Script toRun = (Script) allowedScripts.get(args);
            toRun.run();
            return true;
        }

        showMessage("RunScript: No such script!");
        return false;
    }
}
