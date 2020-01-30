package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;

import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;

public class ShowBounds implements BaseCommand
{
    private static final boolean SHOW_COLLISION_BOUNDS = true;
    private static final boolean SHOW_COLLISION_RADIUS = true;
    private static final boolean SHOW_SHIELD_RADIUS = true;
    private static final boolean SHOW_TARGET_RADIUS = true;
    private static final boolean SHOW_FIGHTER_BAYS = true;
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
            Console.showMessage("Bounds rendering enabled. Legend:\n" +
                    " - White lines: ship collision bounds\n" +
                    " - Gray circle: collision radius (collisions are only checked in this circle)\n" +
                    " - Blue circle: shield radius\n" +
                    " - Red ellipse: target radius (AI uses this for weapon distance checks)\n" +
                    " - Purple diamonds: fighter launch bays");
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
                engine.removePlugin(this);
                return;
            }

            nextCheck.advance(amount);
            if (firstRun || nextCheck.intervalElapsed())
            {
                firstRun = false;

                for (Iterator<ShipAPI> iter = renderers.keySet().iterator(); iter.hasNext(); )
                {
                    if (!engine.isEntityInPlay(iter.next()))
                    {
                        iter.remove();
                    }
                }
            }
        }

        @Override
        public void renderInWorldCoords(ViewportAPI view)
        {
            if (!(SHOW_COLLISION_RADIUS || SHOW_SHIELD_RADIUS || SHOW_TARGET_RADIUS || SHOW_FIGHTER_BAYS))
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
                }

                // When a ship explodes into chunks, the main piece is still the same ship object
                // This checks for that situation and refreshes the renderer
                PointRenderer renderer = renderers.get(ship);
                if (ship.isPiece() && !renderer.wasPiece)
                {
                    renderer = new PointRenderer(ship);
                    renderers.put(ship, renderer);
                }

                renderer.draw(ship);
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
        private final List<PointData> pointData = new ArrayList<>(3);
        private final boolean wasPiece;

        private PointRenderer(ShipAPI ship)
        {
            wasPiece = ship.isPiece();
            final Vector2f origLoc = new Vector2f(ship.getLocation());
            final float origFacing = ship.getFacing();
            ship.setFacing(0f);
            ship.getLocation().set(0f, 0f);

            if (SHOW_COLLISION_RADIUS)
            {
                pointData.add(new PointData(MathUtils.getPointsAlongCircumference(
                        ship.getLocation(), ship.getCollisionRadius(), NUM_POINTS, 0f),
                        new Color(.5f, .5f, .5f, .25f), GL_POLYGON, false, true, true));
            }

            if (SHOW_SHIELD_RADIUS && ship.getShield() != null)
            {
                final ShieldAPI shield = ship.getShield();
                pointData.add(new PointData(MathUtils.getPointsAlongCircumference(
                        shield.getLocation(), shield.getRadius(), NUM_POINTS, 0f),
                        new Color(0f, .5f, .5f, .25f), GL_POLYGON, !shield.getLocation().equals(ship.getLocation()),
                        true, false));
            }

            if (SHOW_TARGET_RADIUS)
            {
                final List<Vector2f> pointsAroundRadius = MathUtils.getPointsAlongCircumference(
                        ship.getLocation(), ship.getCollisionRadius() * 2f, NUM_POINTS, 0f);
                for (int x = 0; x < NUM_POINTS; x++)
                {
                    final Vector2f tmp = pointsAroundRadius.get(x);
                    tmp.set(MathUtils.getPointOnCircumference(ship.getLocation(),
                            Misc.getTargetingRadius(tmp, ship, false),
                            (360f / NUM_POINTS) * x));
                }

                pointData.add(new PointData(pointsAroundRadius,
                        new Color(1f, 0.25f, 0.25f, .25f), GL_POLYGON, true, false, false));
            }

            if (SHOW_COLLISION_BOUNDS)
            {
                final BoundsAPI bounds = ship.getExactBounds();
                if (bounds != null)
                {
                    bounds.update(ship.getLocation(), ship.getFacing());
                    final List<Vector2f> points = new ArrayList<>(bounds.getSegments().size());
                    for (BoundsAPI.SegmentAPI segment : bounds.getSegments())
                    {
                        points.add(new Vector2f(segment.getP1()));
                    }

                    pointData.add(new PointData(points, new Color(1f, 1f, 1f, 1f), GL_LINE_LOOP, true, true, true));
                    bounds.update(origLoc, origFacing);
                }
            }

            if (SHOW_FIGHTER_BAYS && ship.getNumFighterBays() > 0)
            {
                for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
                {
                    if (slot.getWeaponType() == WeaponAPI.WeaponType.LAUNCH_BAY)
                    {
                        pointData.add(new PointData(MathUtils.getPointsAlongCircumference(slot.computePosition(ship),
                                15f, 4, 0f), new Color(1f, 0.1f, 1f, 0.5f), GL_POLYGON, true, true, false));
                    }
                }
            }

            ship.getLocation().set(origLoc);
            ship.setFacing(origFacing);
        }

        private void draw(ShipAPI ship)
        {
            glLineWidth(2f);
            final Vector2f center = ship.getLocation();
            final float facing = (float) Math.toRadians(ship.getFacing());
            final float cos = (float) FastTrig.cos(facing),
                    sin = (float) FastTrig.sin(facing);
            for (PointData data : pointData)
            {
                if ((ship.isHulk() && !data.drawIfHulk) || (ship.isPiece() && !data.drawIfPiece)) continue;

                if (data.shouldRotate)
                {
                    glColor(data.color);
                    glBegin(data.drawMode);
                    for (Vector2f point : data.points)
                    {
                        glVertex2f((point.x * cos) - (point.y * sin) + center.x,
                                (point.x * sin) + (point.y * cos) + center.y);
                    }
                    glEnd();
                }
                else
                {
                    glColor(data.color);
                    glBegin(data.drawMode);
                    for (Vector2f point : data.points)
                    {
                        glVertex2f(point.x + center.x, point.y + center.y);
                    }
                    glEnd();
                }
            }
        }
    }

    private static class PointData
    {
        private final List<Vector2f> points;
        private final Color color;
        private final int drawMode;
        private final boolean shouldRotate, drawIfHulk, drawIfPiece;

        private PointData(List<Vector2f> points, Color color, int drawMode, boolean shouldRotate,
                          boolean drawIfHulk, boolean drawIfPiece)
        {
            this.points = points;
            this.color = color;
            this.drawMode = drawMode;
            this.shouldRotate = shouldRotate;
            this.drawIfHulk = drawIfHulk;
            this.drawIfPiece = drawIfPiece;
        }
    }
}
