package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.mission.FleetSide;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class ForceDeployAll implements BaseCommand
{
    private static final float MIN_OFFSET = 300f;

    // FIXME: Second line never forms
    private static void moveToSpawnLocations(List<ShipAPI> toMove)
    {
        final CombatEngineAPI engine = Global.getCombatEngine();
        final Vector2f spawnLoc = new Vector2f(
                -engine.getMapWidth() * 0.2f, engine.getMapHeight() / 2f);

        final List<ShipAPI> ships = engine.getShips();
        for (ShipAPI ship : toMove)
        {
            final float radius = ship.getCollisionRadius() + MIN_OFFSET;
            for (int i = 0; i < ships.size(); i++)
            {
                final ShipAPI other = ships.get(i);
                if (MathUtils.isWithinRange(other, spawnLoc, radius))
                {
                    spawnLoc.x += radius;
                    if (spawnLoc.x >= engine.getMapWidth() / 2f)
                    {
                        spawnLoc.x = -engine.getMapWidth();
                        spawnLoc.y -= radius;
                    }

                    // We need to recheck for collisions in our new position
                    i = 0;
                }
            }

            //System.out.println("Moving " + ship.getHullSpec().getHullId()
            //        + " to { " + spawnLoc.x + ", " + spawnLoc.y + " }");
            ship.getLocation().set(spawnLoc.x, spawnLoc.y);
            spawnLoc.x += radius;
        }
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final CombatFleetManagerAPI cfm = Global.getCombatEngine().getFleetManager(FleetSide.ENEMY);
        final List<ShipAPI> deployed = new ArrayList<>();
        for (FleetMemberAPI member : cfm.getReservesCopy())
        {
            deployed.add(cfm.spawnFleetMember(member, new Vector2f(0f, 0f), 270f, 3f));
        }

        moveToSpawnLocations(deployed);

        // Not sure this actually lasts long enough to matter, but worth a shot...
        for (ShipAPI ship : Global.getCombatEngine().getShips())
        {
            if (ship.isAlive() && ship.hasLaunchBays() && ship.getOwner() == 1
                    && ship.isPullBackFighters())
            {
                ship.giveCommand(ShipCommand.PULL_BACK_FIGHTERS, null, 0);
            }
        }

        Console.showMessage("Forced enemy to deploy " + deployed.size() + " ships.");
        return CommandResult.SUCCESS;
    }
}
