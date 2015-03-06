package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Suicide implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        // TODO: Implement suicide in campaign
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        ShipAPI player = Global.getCombatEngine().getPlayerShip();
        Kill.killShip(player);
        Console.showMessage("Destroyed currently piloted ship.");
        return CommandResult.SUCCESS;
    }
}
