package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class AddLogistics implements BaseCommand
{
    private static final String BONUS_ID = CommonStrings.MOD_ID;

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCampaign())
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if ("clear".equalsIgnoreCase(args))
        {
            Global.getSector().getPlayerPerson().getStats().getLogistics()
                    .unmodifyFlat(BONUS_ID);
            Console.showMessage("Logistics bonus removed.");
            return CommandResult.SUCCESS;
        }

        int amount;
        try
        {
            amount = Integer.parseInt(args);
        }
        catch (NumberFormatException ex)
        {
            Console.showMessage("Error: logistics bonus must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        final MutableStat logistics = Global.getSector().getPlayerPerson().getStats().getLogistics();
        final StatMod bonus = logistics.getFlatStatMod(BONUS_ID);
        if (bonus != null)
        {
            logistics.modifyFlat(BONUS_ID, amount + bonus.value, "Console");
        }
        else
        {
            logistics.modifyFlat(BONUS_ID, amount, "Console");
        }

        Console.showMessage("Logistics " + (amount >= 0 ? "inc" : "dec") + "reased by "
                + amount + ", now at " + (int) logistics.getModifiedValue() + ".");
        Global.getSector().getPlayerFleet().getLogistics().updateRepairUtilizationForUI();
        return CommandResult.SUCCESS;
    }
}
