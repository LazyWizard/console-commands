package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

public abstract class BaseCommand
{
    protected final SectorAPI getSector()
    {
        return Global.getSector();
    }

    protected final LocationAPI getLocation()
    {
        return Console.getManager().getLocation();
    }

    protected final StarSystemAPI getStarSystem()
    {
        return (StarSystemAPI) getLocation();
    }

    public String getName()
    {
        return this.getClass().getSimpleName();
    }

    protected static Object getVar(String varName)
    {
        return Console.getManager().getVar(varName);
    }

    protected static void setVar(String varName, Object varData)
    {
        Console.getManager().setVar(varName, varData);
    }

    protected static boolean hasVar(String varName)
    {
        return Console.getManager().hasVar(varName);
    }

    protected static void showMessage(String preamble,
            String message, boolean indent)
    {
        Console.showMessage(preamble, message, indent);
    }

    protected static void showMessage(String message, boolean indent)
    {
        Console.showMessage(message, indent);
    }

    protected static void showMessage(String message)
    {
        Console.showMessage(message);
    }

    public final void showHelp()
    {
        if (getHelp().isEmpty())
        {
            showSyntax();
            return;
        }

        showMessage(getName() + " help:",
                "Combat only: " + (isCombatOnly() ? "yes" : "no") + "\n"
                + (getSyntax().isEmpty() ? "" : "Usage: " + getSyntax() + "\n")
                + getHelp(), true);
    }

    public final void showSyntax()
    {
        if (getSyntax().isEmpty())
        {
            showMessage(getName() + " help has not been written yet!");
            return;
        }

        showMessage(getName() + " syntax: " + getSyntax());
    }

    protected boolean isCampaignOnly()
    {
        return true;
    }

    protected boolean isCombatOnly()
    {
        return false;
    }

    protected abstract String getHelp();

    protected abstract String getSyntax();

    protected abstract boolean runCommand(String args);// throws Exception;
}
