package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class Flameout implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final ShipAPI player = Global.getCombatEngine().getPlayerShip();
        ShipAPI target = player.getShipTarget();
        if (target == null)
        {
            target = player;
        }

        for (ShipEngineAPI engine : target.getEngineController().getShipEngines())
        {
            engine.disable();
        }

        Console.showMessage("Forced flameout of all engines on "
                + (target == player ? "player." : "target."));
        return CommandResult.SUCCESS;
    }
}
