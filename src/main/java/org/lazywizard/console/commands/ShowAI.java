package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.List;

import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;

public class ShowAI implements BaseCommand
{
    private static final boolean SHOW_TARGET = true;
    private static final boolean SHOW_MANEUVER_TARGET = true;
    private static final boolean SHOW_MOUSE_TARGET = true;
    private static WeakReference<ShowAIPlugin> plugin;

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        ShowAIPlugin tmp;
        if (plugin == null || plugin.get() == null
                || plugin.get().engine != Global.getCombatEngine())
        {
            tmp = new ShowAIPlugin();
            plugin = new WeakReference<>(tmp);
            Global.getCombatEngine().addPlugin(tmp);
            Console.showMessage("AI target rendering enabled. Legend:\n - Red: targeted ship\n - Cyan: maneuver target\n - Gray: mouse target");
        }
        else
        {
            tmp = plugin.get();
            plugin.clear();
            tmp.active = false;
            Console.showMessage("AI target rendering disabled.");
        }

        return CommandResult.SUCCESS;
    }

    private static class ShowAIPlugin extends BaseEveryFrameCombatPlugin
    {
        private static final float BLINK_SPEED = 5f;
        private boolean active = true;
        private CombatEngineAPI engine;
        private float alphaTimer = -1f; // Advances from -1 to 1; absolute value is used for smooth fading

        @Override
        public void advance(float amount, List<InputEventAPI> events)
        {
            if (!active)
            {
                engine.removePlugin(this);
            }

            alphaTimer += amount * BLINK_SPEED;
            if (alphaTimer >= 1f) alphaTimer = -1f;
        }

        @Override
        public void renderInWorldCoords(ViewportAPI view)
        {
            if (!(SHOW_TARGET || SHOW_MANEUVER_TARGET || SHOW_MOUSE_TARGET))
            {
                return;
            }

            // Used to make the lines flash a bit for better readability (still not great w/ many lasers)
            final float alpha = 0.5f + (0.2f * Math.abs(alphaTimer)), halfAlpha = alpha * 0.5f;

            // Set OpenGL flags
            glPushAttrib(GL_ALL_ATTRIB_BITS);
            glMatrixMode(GL_PROJECTION);
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            // Draw the ship's target and maneuver targets
            glBegin(GL_LINES);
            for (ShipAPI ship : Global.getCombatEngine().getShips())
            {
                final Vector2f shipLoc = ship.getLocation();

                if (SHOW_TARGET && ship.getShipTarget() != null)
                {
                    final Vector2f targetLoc = ship.getShipTarget().getLocation();
                    glColor(Color.RED, alpha, true);
                    glVertex2f(shipLoc.x, shipLoc.y);
                    glColor(Color.RED, halfAlpha, true);
                    glVertex2f(targetLoc.x, targetLoc.y);
                }

                if (SHOW_MANEUVER_TARGET && ship.getShipTarget() == null)
                {
                    final Object tmpTarget = ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
                    if (tmpTarget instanceof ShipAPI)
                    {
                        final Vector2f targetLoc = ((ShipAPI) tmpTarget).getLocation();
                        glColor(Color.CYAN, alpha, true);
                        glVertex2f(shipLoc.x, shipLoc.y);
                        glColor(Color.CYAN, halfAlpha, true);
                        glVertex2f(targetLoc.x, targetLoc.y);
                    }
                }

                if (SHOW_MOUSE_TARGET && ship.getMouseTarget() != null)
                {
                    final Vector2f targetLoc = ship.getMouseTarget();
                    glColor(Color.DARK_GRAY, alpha, true);
                    glVertex2f(shipLoc.x, shipLoc.y);
                    glColor(Color.DARK_GRAY, halfAlpha, true);
                    glVertex2f(targetLoc.x, targetLoc.y);
                }
            }
            glEnd();

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
