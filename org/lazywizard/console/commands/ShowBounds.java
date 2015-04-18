package org.lazywizard.console.commands;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.opengl.DrawUtils;
import org.lwjgl.util.vector.Vector2f;
import static org.lwjgl.opengl.GL11.*;

public class ShowBounds implements BaseCommand
{
    private static final boolean SHOW_COLLISION_RADIUS = true;
    private static final boolean SHOW_SHIELD_RADIUS = true;
    private static final boolean SHOW_TARGET_RADIUS = true;
    private static WeakReference<ShowBoundsPlugin> plugin;

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        ShowBoundsPlugin tmp;
        if (plugin == null || plugin.get() == null
                || plugin.get().engine != Global.getCombatEngine())
        {
            tmp = new ShowBoundsPlugin();
            plugin = new WeakReference<>(tmp);
            Global.getCombatEngine().addPlugin(tmp);
            Console.showMessage("Bounds rendering enabled.");
        }
        else
        {
            tmp = plugin.get();
            plugin.clear();
            tmp.active = false;
            Console.showMessage("Bounds rendering disabled.");
        }

        return CommandResult.SUCCESS;
    }

    private static class ShowBoundsPlugin extends BaseEveryFrameCombatPlugin
    {
        private final IntervalUtil nextCheck = new IntervalUtil(1f, 1f);
        private boolean active = true, firstRun = true;
        private CombatEngineAPI engine;

        @Override
        public void advance(float amount, List<InputEventAPI> events)
        {
            if (!active)
            {
                for (ShipAPI ship : engine.getShips())
                {
                    ship.setRenderBounds(false);
                }

                engine.removePlugin(this);
                return;
            }

            nextCheck.advance(amount);
            if (firstRun || nextCheck.intervalElapsed())
            {
                firstRun = false;

                for (ShipAPI ship : engine.getShips())
                {
                    ship.setRenderBounds(true);
                }
            }
        }

        @Override
        public void renderInWorldCoords(ViewportAPI view)
        {
            if (!(SHOW_COLLISION_RADIUS || SHOW_SHIELD_RADIUS || SHOW_TARGET_RADIUS))
            {
                return;
            }

            // Set OpenGL flags
            glPushAttrib(GL_ALL_ATTRIB_BITS);
            glMatrixMode(GL_PROJECTION);
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            // Draw the ship's collision, shield and target radii
            for (ShipAPI ship : Global.getCombatEngine().getShips())
            {
                if (SHOW_COLLISION_RADIUS)
                {
                    glColor4f(.5f, .5f, .5f, .25f);
                    DrawUtils.drawCircle(ship.getLocation().x, ship.getLocation().y,
                            ship.getCollisionRadius(), 144, true);
                }

                if (SHOW_SHIELD_RADIUS)
                {
                    ShieldAPI shield = ship.getShield();
                    if (shield != null)
                    {
                        glColor4f(0f, .5f, .5f, .25f);
                        DrawUtils.drawCircle(shield.getLocation().x, shield.getLocation().y,
                                shield.getRadius(), 144, true);
                    }
                }

                if (SHOW_TARGET_RADIUS)
                {
                    /*final float radiusU = Misc.getTargetingRadius(
                     MathUtils.getPointOnCircumference(ship.getLocation(),
                     ship.getCollisionRadius() * 2f,
                     ship.getFacing() + 0f), ship, false),
                     radiusL = Misc.getTargetingRadius(
                     MathUtils.getPointOnCircumference(ship.getLocation(),
                     ship.getCollisionRadius() * 2f,
                     ship.getFacing() + 90f), ship, false),
                     radiusD = Misc.getTargetingRadius(
                     MathUtils.getPointOnCircumference(ship.getLocation(),
                     ship.getCollisionRadius() * 2f,
                     ship.getFacing() + 180f), ship, false),
                     radiusR = Misc.getTargetingRadius(
                     MathUtils.getPointOnCircumference(ship.getLocation(),
                     ship.getCollisionRadius() * 2f,
                     ship.getFacing() + 270f), ship, false);
                     final float offsetX = radiusR - radiusL,
                     offsetY = radiusU - radiusD;
                     final Vector2f center = new Vector2f(ship.getLocation());
                     center.x += offsetX;
                     center.y += offsetY;
                     VectorUtils.rotateAroundPivot(center, ship.getLocation(),
                     ship.getFacing(), center);

                     glColor4f(1f, 1f, 1f, .25f);
                     DrawUtils.drawEllipse(center.x, center.y, (radiusR + radiusL) / 2f,
                     (radiusU + radiusD) / 2f, ship.getFacing() - 90f, 144, true);*/

                    // TODO: Figure out ellipse bounds and draw using drawEllipse()
                    List<Vector2f> pointsAroundRadius = new ArrayList<>(144);
                    for (float x = ship.getFacing(); x < ship.getFacing() + 360f; x += 360f / 144f)
                    {
                        final float targetRadius = Misc.getTargetingRadius(
                                MathUtils.getPointOnCircumference(ship.getLocation(),
                                        ship.getCollisionRadius() * 2f, x), ship, false);

                        pointsAroundRadius.add(MathUtils.getPointOnCircumference(
                                ship.getLocation(), targetRadius, x));
                    }

                    glColor4f(1f, 1f, 1f, 0.15f);
                    glPointSize(3f);
                    glBegin(GL_POLYGON);
                    for (Vector2f point : pointsAroundRadius)
                    {
                        glVertex2f(point.x, point.y);
                    }
                    glEnd();

                    // Draw difference between collision radius and target radius
                    /*List<Vector2f> pointsAroundRadius = MathUtils.getPointsAlongCircumference(
                     ship.getLocation(), ship.getCollisionRadius() * 2f, 8, 0f);
                     for (Vector2f point : pointsAroundRadius)
                     {
                     final float distanceNormal = MathUtils.getDistance(ship, point),
                     distanceMisc = MathUtils.getDistance(ship.getLocation(), point)
                     - Misc.getTargetingRadius(point, ship, false);

                     final float angleTowards = VectorUtils.getAngle(point, ship.getLocation());
                     Vector2f endPointNormal = MathUtils.getPointOnCircumference(
                     point, distanceNormal, angleTowards),
                     endPointMisc = MathUtils.getPointOnCircumference(
                     point, distanceMisc, angleTowards);

                     glBegin(GL_LINES);
                     glColor4f(0f, 1f, 0f, 1f);
                     glVertex2f(point.x, point.y);
                     glVertex2f(endPointNormal.x, endPointNormal.y);
                     glColor4f(1f, 0f, 0f, 1f);
                     glVertex2f(point.x, point.y);
                     glVertex2f(endPointMisc.x, endPointMisc.y);
                     glEnd();
                     }*/
                }
            }

            // TODO: Optimize this
            // TODO: Add to changelog
            /*final ShipAPI player = engine.getPlayerShip();
             if (player != null && player.getShipTarget() != null)
             {
             final boolean considerShields = false;
             final ShipAPI target = player.getShipTarget();

             final float angleTowards = VectorUtils.getAngle(
             player.getLocation(), target.getLocation());
             final Vector2f startPointNormal = MathUtils.getPointOnCircumference(
             player.getLocation(), player.getCollisionRadius(), angleTowards),
             startPointMisc = MathUtils.getPointOnCircumference(
             player.getLocation(), Misc.getTargetingRadius(
             target.getLocation(), player, considerShields), angleTowards);

             final float distanceNormal = MathUtils.getDistance(target, startPointNormal),
             distanceMisc = MathUtils.getDistance(target.getLocation(), startPointMisc)
             - Misc.getTargetingRadius(player.getLocation(), target, considerShields);

             Vector2f endPointNormal = MathUtils.getPointOnCircumference(
             startPointNormal, distanceNormal, angleTowards),
             endPointMisc = MathUtils.getPointOnCircumference(
             startPointMisc, distanceMisc, angleTowards);

             if (distanceNormal > 0f || distanceMisc > 0f)
             {
             glBegin(GL_LINES);
             if (distanceNormal > 0f)
             {
             glColor4f(1f, 0f, 0f, 1f);
             glVertex2f(startPointNormal.x, startPointNormal.y);
             glVertex2f(endPointNormal.x, endPointNormal.y);
             }
             if (distanceMisc > 0f)
             {
             glColor4f(0f, 1f, 0f, 1f);
             glVertex2f(startPointMisc.x, startPointMisc.y);
             glVertex2f(endPointMisc.x, endPointMisc.y);
             }
             glEnd();
             }
             }*/
            // Finalize drawing
            glDisable(GL_BLEND);
            glPopAttrib();
        }

        @Override
        public void init(CombatEngineAPI engine)
        {
            this.engine = engine;
        }
    }
}
