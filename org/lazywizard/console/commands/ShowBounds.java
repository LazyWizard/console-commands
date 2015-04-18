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
                            ship.getCollisionRadius(), NUM_POINTS, true);
                }

                if (SHOW_SHIELD_RADIUS)
                {
                    ShieldAPI shield = ship.getShield();
                    if (shield != null)
                    {
                        glColor4f(0f, .5f, .5f, .25f);
                        DrawUtils.drawCircle(shield.getLocation().x, shield.getLocation().y,
                                shield.getRadius(), NUM_POINTS, true);
                    }
                }

                if (SHOW_TARGET_RADIUS)
                {
                    // TODO: Figure out ellipse bounds and draw using drawEllipse()
                    // TODO: Optimize this!
                    List<Vector2f> pointsAroundRadius = new ArrayList<>(NUM_POINTS);
                    for (float x = ship.getFacing(); x < ship.getFacing() + 360f; x += 360f / NUM_POINTS)
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
