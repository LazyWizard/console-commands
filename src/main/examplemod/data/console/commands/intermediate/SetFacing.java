package data.console.commands.intermediate;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import org.apache.log4j.Logger;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;

public class SetFacing implements BaseCommand
{
    private static final Logger Log = Logger.getLogger(SetFacing.class);

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        float angle;
        try
        {
            angle = Float.parseFloat(args);
        }
        catch (NumberFormatException ex)
        {
            return CommandResult.BAD_SYNTAX;
        }

        final ShipAPI player = Global.getCombatEngine().getPlayerShip(),
                ship = (player.getShipTarget() == null ? player : player.getShipTarget());
        angle = MathUtils.clampAngle(angle);
        ship.setFacing(angle);
        Console.showMessage("Set facing of " + ship.getName() + " ("
                + ship.getHullSpec().getHullId() + ") to " + angle + " degrees.");
        return CommandResult.SUCCESS;
    }
}
