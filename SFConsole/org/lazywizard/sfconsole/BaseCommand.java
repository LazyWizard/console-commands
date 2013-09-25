package org.lazywizard.sfconsole;

/**
 * The basic command object that all console commands must extend.
 */
public abstract class BaseCommand
{

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
     * Displays the helpfile returned by {@link BaseCommand#getHelp()}.
     */
    public final void showHelp()
    {
        if (getHelp().isEmpty())
        {
            showSyntax();
            return;
        }

        Console.showMessage(getName() + " help:",
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
            Console.showMessage(getName() + " help has not been written yet!");
            return;
        }

        Console.showMessage(getName() + " syntax: " + getSyntax());
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
