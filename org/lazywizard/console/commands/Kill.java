package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

public class Kill implements BaseCommand
{
    public static void killShip(ShipAPI target, boolean assignKillToPlayer)
    {
        if (target == null)
        {
            return;
        }

        // Ensure we hit (needed for certain oddly-shaped mod ships)
        Vector2f hitLoc = target.getLocation();
        if (!CollisionUtils.isPointWithinBounds(hitLoc, target))
        {
            if (!target.getAllWeapons().isEmpty())
            {
                //System.out.println("Using alternate hit location for "
                //        + ship.getHullSpec().getHullId());
                hitLoc = target.getAllWeapons().get(0).getLocation();
            }
            else
            {
                if (!target.getEngineController().getShipEngines().isEmpty())
                {
                    hitLoc = target.getEngineController().getShipEngines().get(0).getLocation();
                }
                else
                {
                    Console.showMessage("Error nuking " + target.getHullSpec().getHullId());
                }
            }
        }

        // Ensure a kill
        target.getMutableStats().getHullDamageTakenMult().unmodify();
        target.getMutableStats().getArmorDamageTakenMult().unmodify();
        target.setHitpoints(1f);
        int[] cell = target.getArmorGrid().getCellAtLocation(hitLoc);
        target.getArmorGrid().setArmorValue(cell[0], cell[1], 0f);
        Global.getCombatEngine().applyDamage(target, hitLoc, 500_000,
                DamageType.OTHER, 500_000, true, false, null);
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        ShipAPI target = engine.getPlayerShip().getShipTarget();

        if (target == null)
        {
            Console.showMessage("No target found!");
            return CommandResult.ERROR;
        }

        killShip(target, false);
        Console.showMessage("Destroyed "
                + target.getVariant().getFullDesignationWithHullName() + ".");
        return CommandResult.SUCCESS;
    }
}
