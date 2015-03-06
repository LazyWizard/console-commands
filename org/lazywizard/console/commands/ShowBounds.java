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
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.opengl.DrawUtils;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;

public class ShowBounds implements BaseCommand
{
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

            ViewportAPI view = engine.getViewport();

            // Retina display fix
            int width = (int) (Display.getWidth() * Display.getPixelScaleFactor()),
                    height = (int) (Display.getHeight() * Display.getPixelScaleFactor());

            // Set OpenGL flags
            glPushAttrib(GL_ALL_ATTRIB_BITS);
            glMatrixMode(GL_PROJECTION);
            glPushMatrix();
            glLoadIdentity();
            glViewport(0, 0, width, height);
            glOrtho(0, width, 0, height, 1, -1);
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            // Draw the ship's collision and shield radii
            for (ShipAPI ship : Global.getCombatEngine().getShips())
            {
                final float mult = view.getViewMult();
                glColor4f(.5f, .5f, .5f, .25f);
                DrawUtils.drawCircle(view.convertWorldXtoScreenX(ship.getLocation().x),
                        view.convertWorldYtoScreenY(ship.getLocation().y),
                        ship.getCollisionRadius() / mult, 144, true);
                ShieldAPI shield = ship.getShield();
                if (shield != null)
                {
                    glColor4f(0f, .5f, .5f, .25f);
                    DrawUtils.drawCircle(view.convertWorldXtoScreenX(shield.getLocation().x),
                            view.convertWorldYtoScreenY(shield.getLocation().y),
                            shield.getRadius() / mult, 144, true);
                }
            }

            // Finalize drawing
            glDisable(GL_BLEND);
            glPopMatrix();
            glMatrixMode(GL_PROJECTION);
            glPopMatrix();
            glPopAttrib();
        }

        @Override
        public void init(CombatEngineAPI engine)
        {
            this.engine = engine;
        }
    }
}
