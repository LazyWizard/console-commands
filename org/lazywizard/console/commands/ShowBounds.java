package org.lazywizard.console.commands;

import java.lang.ref.WeakReference;
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
    private static final int NUM_POINTS = 144;
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

        private static void drawCollisionRadius(ShipAPI ship)
        {
            glColor4f(.5f, .5f, .5f, .25f);
            DrawUtils.drawCircle(ship.getLocation().x, ship.getLocation().y,
                    ship.getCollisionRadius(), NUM_POINTS, true);
        }

        private static void drawShieldRadius(ShieldAPI shield)
        {
            glColor4f(0f, .5f, .5f, .25f);
            DrawUtils.drawCircle(shield.getLocation().x, shield.getLocation().y,
                    shield.getRadius(), NUM_POINTS, true);
        }

        /*private static final float CHECK_DISTANCE = 5000f;
        private static final Vector2f FRONT = new Vector2f(CHECK_DISTANCE, 0f);
        private static final Vector2f LEFT = new Vector2f(0f, CHECK_DISTANCE);
        private static final Vector2f REAR = new Vector2f(-CHECK_DISTANCE, 0f);
        private static final Vector2f RIGHT = new Vector2f(0f, -CHECK_DISTANCE);*/

        private static void drawTargetRadius(ShipAPI ship)
        {
            // TODO: Figure out ellipse bounds and draw using drawEllipse()
            // TODO: Optimize this!

            final Vector2f origLoc = new Vector2f(ship.getLocation());
            final float origFacing = ship.getFacing();
            ship.setFacing(0f);
            ship.getLocation().set(0f, 0f);
            /*final float radiusFront = Misc.getTargetingRadius(FRONT, ship, false),
             radiusLeft = Misc.getTargetingRadius(LEFT, ship, false),
             radiusRear = Misc.getTargetingRadius(REAR, ship, false),
             radiusRight = Misc.getTargetingRadius(RIGHT, ship, false);
             final Vector2f front = MathUtils.getPointOnCircumference(origLoc, radiusFront, origFacing),
             left = MathUtils.getPointOnCircumference(origLoc, radiusLeft, origFacing + 90f),
             rear = MathUtils.getPointOnCircumference(origLoc, radiusRear, origFacing + 180f),
             right = MathUtils.getPointOnCircumference(origLoc, radiusRight, origFacing + 270f);*/

            ship.getLocation().set(origLoc);
            ship.setFacing(origFacing);

            /*final Vector2f center = MathUtils.getMidpoint(front, rear);
             glColor4f(1f, 1f, 0f, .25f);
             DrawUtils.drawEllipse(center.x, center.y, (radiusFront + radiusRear) / 2f,
             (radiusLeft + radiusRight) / 2f, ship.getFacing(), NUM_POINTS, true);*/
            final List<Vector2f> pointsAroundRadius = MathUtils.getPointsAlongCircumference(
                    ship.getLocation(), ship.getCollisionRadius() * 2f,
                    NUM_POINTS, ship.getFacing());
            for (int x = 0; x < NUM_POINTS; x++)
            {
                final Vector2f tmp = pointsAroundRadius.get(x);
                tmp.set(MathUtils.getPointOnCircumference(ship.getLocation(),
                        Misc.getTargetingRadius(tmp, ship, false),
                        ship.getFacing() + (360f / NUM_POINTS) * x));
            }

            glColor4f(1f, 0.25f, 0.25f, .25f);
            glBegin(GL_POLYGON);
            for (Vector2f point : pointsAroundRadius)
            {
                glVertex2f(point.x, point.y);
            }
            glEnd();

            /*glColor4f(1f, 1f, 1f, 1f);
             glPointSize(5f);
             glBegin(GL_POINTS);
             glVertex2f(front.x, front.y);
             glVertex2f(left.x, left.y);
             glVertex2f(rear.x, rear.y);
             glVertex2f(right.x, right.y);
             glEnd();*/
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
                if (!view.isNearViewport(ship.getLocation(), ship.getCollisionRadius() + 150f))
                {
                    continue;
                }

                if (SHOW_COLLISION_RADIUS)
                {
                    drawCollisionRadius(ship);
                }

                if (SHOW_SHIELD_RADIUS)
                {
                    ShieldAPI shield = ship.getShield();
                    if (shield != null)
                    {
                        drawShieldRadius(shield);
                    }
                }

                if (SHOW_TARGET_RADIUS)
                {
                    drawTargetRadius(ship);
                }
            }

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
