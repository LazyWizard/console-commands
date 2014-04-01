package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.CollisionUtils;
import org.lwjgl.util.vector.Vector2f;

public class Nuke implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context == CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        CombatEngineAPI engine = Global.getCombatEngine();
        for (ShipAPI ship : engine.getShips())
        {
            if (ship.isHulk() || ship.isShuttlePod())
            {
                continue;
            }

            if (ship.getOwner() == FleetSide.ENEMY.ordinal())
            {
                Vector2f hitLoc = ship.getLocation();

                // Ensure we hit (needed for certain oddly-shaped mod ships)
                if (!CollisionUtils.isPointWithinBounds(ship.getLocation(), ship))
                {
                    if (!ship.getAllWeapons().isEmpty())
                    {
                        //System.out.println("Using alternate hit location for "
                        //        + ship.getHullSpec().getHullId());
                        hitLoc = ship.getAllWeapons().get(0).getLocation();
                    }
                    else
                    {
                        Console.showMessage("Error nuking " + ship.getHullSpec().getHullId());
                    }
                }

                // Ensure a kill
                ship.setHitpoints(1f);
                engine.applyDamage(ship, hitLoc, 500_000,
                        DamageType.OTHER, 500_000, true, false, null);
            }
        }

        Console.showMessage("Nuke activated. All enemy ships destroyed.");
        return CommandResult.SUCCESS;
    }
}
