package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.StatBonus;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import static org.lazywizard.console.CommandUtils.*;

public class AddOrdnancePoints implements BaseCommand
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
            Global.getSector().getPlayerPerson().getStats().getShipOrdnancePointBonus()
                    .unmodifyFlat(BONUS_ID);
            Console.showMessage("Ordnance point bonus removed.");
            return CommandResult.SUCCESS;
        }

        if (!isInteger(args))
        {
            Console.showMessage("Error: OP bonus must be a whole number!");
            return CommandResult.BAD_SYNTAX;
        }

        int amount = Integer.parseInt(args);
        final StatBonus ordnance = Global.getSector().getPlayerPerson().getStats().getShipOrdnancePointBonus();
        final MutableStat.StatMod bonus = ordnance.getFlatBonus(BONUS_ID);
        if (bonus != null)
        {
            amount += bonus.value;
        }

        ordnance.modifyFlat(BONUS_ID, amount, "Console");
        Console.showMessage("All ships in your fleet now have " + format(amount)
                + " extra ordnance points.\nUse 'addordnancepoints clear' to remove.");
        return CommandResult.SUCCESS;
    }
}
