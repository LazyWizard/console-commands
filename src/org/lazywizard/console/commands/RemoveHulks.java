package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class RemoveHulks implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        int numHulks = 0;
        for (ShipAPI ship : Global.getCombatEngine().getShips())
        {
            if (ship.isHulk())
            {
                Global.getCombatEngine().removeEntity(ship);
                numHulks++;
            }
        }

        Console.showMessage(numHulks + " hulks removed from the battle map.");
        return CommandResult.SUCCESS;
    }
}
