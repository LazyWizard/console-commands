package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

/**
 * The basic command object that all console commands must extend.
 */
public abstract class BaseCommand
{
    /**
     * Get the current {@link SectorAPI}.
     *
     * @return the {@link SectorAPI} object used by this campaign
     */
    protected final SectorAPI getSector()
    {
        return Global.getSector();
    }

    /**
     * Get the current {@link LocationAPI}.
     *
     * @return the {@link LocationAPI} the player fleet is occupying
     */
    protected final LocationAPI getLocation()
    {
        return Console.getManager().getLocation();
    }

    /**
     * Get the current {@link StarSystemAPI}.
     *
     * @return the {@link StarSystemAPI} the player fleet is occupying
     */
    protected final StarSystemAPI getStarSystem()
    {
        return (StarSystemAPI) getLocation();
    }

    /**
     * Get the name of this command
     *
     * @return the name of this command's implementation's class
     */
    public String getName()
    {
        return this.getClass().getSimpleName();
    }

    /**
     * A convenient alias for {@link ConsoleManager#getVar(java.lang.String)}.
     *
     * @see ConsoleManager#getVar(java.lang.String)
     */
    protected static Object getVar(String varName)
    {
        return Console.getManager().getVar(varName);
    }

    /**
     * A convenient alias for {@link ConsoleManager#setVar(java.lang.String, java.lang.Object)}.
     *
     * @see ConsoleManager#setVar(java.lang.String, java.lang.Object)
     */
    protected static void setVar(String varName, Object varData)
    {
        Console.getManager().setVar(varName, varData);
    }

    /**
     * A convenient alias for {@link ConsoleManager#hasVar(java.lang.String)}.
     *
     * @see ConsoleManager#hasVar(java.lang.String)
     */
    protected static boolean hasVar(String varName)
    {
        return Console.getManager().hasVar(varName);
    }

    /**
     * A convenient alias for {@link Console#showMessage(java.lang.String, java.lang.String, boolean)}.
     *
     * @see Console#showMessage(java.lang.String, java.lang.String, boolean)
     */
    protected static void showMessage(String preamble,
            String message, boolean indent)
    {
        Console.showMessage(preamble, message, indent);
    }

    /**
     * A convenient alias for {@link Console#showMessage(java.lang.String)}.
     *
     * @see Console#showMessage(java.lang.String)
     */
    protected static void showMessage(String message)
    {
        Console.showMessage(message);
    }

    /**
     * Displays the helpfile returned by {@link BaseCommand#getHelp()}.
     */
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

    /**
     * Displays the syntax returned by {@link BaseCommand#getSyntax()}.
     */
    public final void showSyntax()
    {
        if (getSyntax().isEmpty())
        {
            showMessage(getName() + " help has not been written yet!");
            return;
        }

        showMessage(getName() + " syntax: " + getSyntax());
    }

    /**
     * Get whether this command is restricted to battles only
     *
     * @return true if this command can only be run on the battle map
     */
    protected boolean isCombatOnly()
    {
        return false;
    }

    /**
     *
     * @return the text to be displayed by {@link BaseCommand#showHelp()}.
     */
    protected abstract String getHelp();

    /**
     *
     * @return the text to be displayed by {@link BaseCommand#showSyntax()}.
     */
    protected abstract String getSyntax();

    /**
     * Run this command with the supplied arguments.
     *
     * @param args the {@link String} arguments passed by the {@link Console}.
     * @return true if the command succeeded, false if it failed
     */
    protected abstract boolean runCommand(String args);
}
