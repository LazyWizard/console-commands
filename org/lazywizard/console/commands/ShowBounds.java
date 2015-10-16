package org.lazywizard.console.commands;

import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
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
        private final Map<ShipAPI, PointRenderer> renderers = new LinkedHashMap<>();
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

                // Recalculate everything occasionally, supports morphing ships
                renderers.clear();
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
                if (!view.isNearViewport(ship.getLocation(), ship.getCollisionRadius() + 150f))
                {
                    continue;
                }

                if (!renderers.containsKey(ship))
                {
                    renderers.put(ship, new PointRenderer(ship));
                    System.out.println(ship.getVariant().getFullDesignationWithHullName());
                }

                renderers.get(ship).draw(ship.getLocation(), ship.getFacing());
            }

            // Finalize drawing
            glDisable(GL_BLEND);
            glPopAttrib();
        }

        @Override
        public void init(CombatEngineAPI engine)
        {
            this.engine = engine;
            renderers.clear();
        }
    }

    private static class PointRenderer
    {
        private final Map<List<Vector2f>, Color> points = new LinkedHashMap<>();

        private static List<Vector2f> getRotatedPoints(List<Vector2f> originalPoints,
                Vector2f center, float facing)
        {
            final List<Vector2f> rotated = VectorUtils.rotate(originalPoints, facing);
            for (Vector2f point : rotated)
            {
                Vector2f.add(point, center, point);
            }

            return rotated;
        }

        private PointRenderer(ShipAPI ship)
        {
            if (SHOW_COLLISION_RADIUS)
            {
                points.put(MathUtils.getPointsAlongCircumference(
                        null, ship.getCollisionRadius(), NUM_POINTS, 0f),
                        new Color(.5f, .5f, .5f, .25f));
            }

            if (SHOW_SHIELD_RADIUS && ship.getShield() != null)
            {
                points.put(MathUtils.getPointsAlongCircumference(
                        null, ship.getShield().getRadius(), NUM_POINTS, 0f),
                        new Color(0f, .5f, .5f, .25f));
            }

            if (SHOW_TARGET_RADIUS)
            {
                final Vector2f origLoc = new Vector2f(ship.getLocation());
                final float origFacing = ship.getFacing();
                ship.setFacing(0f);
                ship.getLocation().set(0f, 0f);

                final List<Vector2f> pointsAroundRadius = MathUtils.getPointsAlongCircumference(
                        null, ship.getCollisionRadius() * 2f, NUM_POINTS, 0f);
                for (int x = 0; x < NUM_POINTS; x++)
                {
                    final Vector2f tmp = pointsAroundRadius.get(x);
                    tmp.set(MathUtils.getPointOnCircumference(null,
                            Misc.getTargetingRadius(tmp, ship, false),
                            (360f / NUM_POINTS) * x));
                }

                points.put(pointsAroundRadius, new Color(1f, 0.25f, 0.25f, .25f));
                ship.getLocation().set(origLoc);
                ship.setFacing(origFacing);
            }
        }

        private void draw(Vector2f center, float facing)
        {
            for (Map.Entry<List<Vector2f>, Color> entry : points.entrySet())
            {
                glColor(entry.getValue());
                glBegin(GL_POLYGON);
                for (Vector2f point : getRotatedPoints(entry.getKey(), center, facing))
                {
                    glVertex2f(point.x, point.y);
                }
                glEnd();
            }
        }
    }
}
