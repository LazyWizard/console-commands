package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import data.scripts.console.BaseCommand;

public class AddXP extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Adds the specified amount of experience points to your character.";
    }

    @Override
    protected String getSyntax()
    {
        return "addxp <amount>";
    }

    @Override
    public boolean runCommand(String args)
    {
        MutableCharacterStatsAPI character =
                Global.getSector().getPlayerFleet().getCommanderStats();
        long amount;

        try
        {
            amount = Long.parseLong(args);
        }
        catch (NumberFormatException ex)
        {
            showMessage("Error: experience must be a whole number!");
            return false;
        }

        character.addXP(amount);
        showMessage("Added " + amount + " experience points to character.");
        return true;
    }
}