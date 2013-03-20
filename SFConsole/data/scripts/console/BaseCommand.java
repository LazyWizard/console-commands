package data.scripts.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;

/**
 * The basic command object that all console commands must extend.
 */
public abstract class BaseCommand
{
    /**
     * Returns the current {@link SectorAPI}.
     *
     * @return the {@link SectorAPI} object used by this campaign
     */
    protected final SectorAPI getSector()
    {
        return Global.getSector();
    }

    /**
     * Returns the current {@link LocationAPI}.
     *
     * @return the {@link LocationAPI} the player fleet is occupying
     */
    protected final LocationAPI getLocation()
    {
        return Console.getConsole().getLocation();
    }

    /**
     * Returns the current {@link StarSystemAPI}.
     *
     * @return the {@link StarSystemAPI} the player fleet is occupying
     */
    protected final StarSystemAPI getStarSystem()
    {
        return (StarSystemAPI) getLocation();
    }

    /**
     * Returns the name of this command.
     *
     * @return the name of this command's implementation's class
     */
    public String getName()
    {
        return this.getClass().getSimpleName();
    }

    /**
     * A convenient alias for {@link Console#getConsole()}.
     *
     * @see Console#getConsole()
     */
    protected final Console getConsole()
    {
        return Console.getConsole();
    }

    /**
     * A convenient alias for {@link Console#getCombatEngine()}.
     *
     * @see Console#getCombatEngine()
     */
    protected final CombatEngineAPI getCombatEngine()
    {
        return Console.getCombatEngine();
    }

    /**
     * A convenient alias for {@link Console#getVar(java.lang.String, java.lang.Class)}.
     *
     * @see Console#getVar(java.lang.String, java.lang.Class)
     */
    protected static <T> T getVar(String varName, Class<T> type)
    {
        return Console.getConsole().getVar(varName, type);
    }

    /**
     * A convenient alias for {@link Console#setVar(java.lang.String, java.lang.Object)}.
     *
     * @see Console#setVar(java.lang.String, java.lang.Object)
     */
    protected static void setVar(String varName, Object varData)
    {
        Console.getConsole().setVar(varName, varData);
    }

    /**
     * A convenient alias for {@link Console#hasVar(java.lang.String)}.
     *
     * @see Console#hasVar(java.lang.String)
     */
    protected static boolean hasVar(String varName)
    {
        return Console.getConsole().hasVar(varName);
    }

    /**
     * A convenient alias for {@link Console#getVarType(java.lang.String)}.
     *
     * @see Console#getVarType(java.lang.String)
     */
    protected static Class getVarType(String varName)
    {
        return Console.getConsole().getVarType(varName);
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
     * A convenient alias for {@link Console#showError(java.lang.String, java.lang.Exception)}.
     *
     * @see Console#showError(java.lang.String, java.lang.Throwable)
     */
    protected static void showError(String preamble, Throwable ex)
    {
        Console.showError(preamble, ex);
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
                "Useable in campaign: " + (isUseableInCampaign() ? "yes" : "no") + "\n"
                + "Useable in combat: " + (isUseableInCombat() ? "yes" : "no") + "\n"
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
     * Returns whether this command can be used in campaigns.
     *
     * @return true if this command can be run on the campaign map
     */
    protected boolean isUseableInCampaign()
    {
        return true;
    }

    /**
     * Returns whether this command can be used in battles.
     *
     * @return true if this command can be run on the battle map
     */
    protected boolean isUseableInCombat()
    {
        return false;
    }

    /**
     * A block of text displayed via the 'help' command.
     *
     * @return the text to be displayed by {@link BaseCommand#showHelp()}.
     */
    protected abstract String getHelp();

    /**
     * A line of text displayed when a command is entered incorrectly.
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
