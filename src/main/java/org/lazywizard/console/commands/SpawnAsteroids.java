package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.opengl.DrawUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static org.lazywizard.lazylib.opengl.ColorUtils.glColor;
import static org.lwjgl.opengl.GL11.*;

public class SpawnAsteroids implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.getCombatUI().isShowingCommandUI())
        {
            Console.showMessage("Error: this command can't be used while the map screen is open!");
            return CommandResult.ERROR;
        }

        // TODO: Have size selectable within the combat plugin (number keys? +/-?)
        final int size;
        switch (args.toLowerCase())
        {
            case "": // No argument, random size
                size = -1;
                break;
            case "tiny":
                size = 0;
                break;
            case "small":
                size = 1;
                break;
            case "medium":
                size = 2;
                break;
            case "large":
                size = 3;
                break;
            default:
                Console.showMessage("Valid asteroid sizes: tiny, small, medium, large.");
                return CommandResult.BAD_SYNTAX;
        }

        // TODO: Ensure only one plugin exists at a time
        engine.addPlugin(new SpawnPlugin(size));
        engine.getCombatUI().addMessage(0, Console.getSettings().getOutputColor(),
                "Click and drag to spawn asteroids, press space to finish spawning.");
        Console.showMessage("Click and drag to spawn asteroids, press space to finish spawning.");
        return CommandResult.SUCCESS;
    }

    private static class SpawnPlugin extends BaseEveryFrameCombatPlugin
    {
        private int asteroidSize;
        private final Vector2f spawnLoc = new Vector2f(0f, 0f);
        private final boolean wasPaused;
        private boolean mouseDown = false;

        private SpawnPlugin(int asteroidSize)
        {
            this.asteroidSize = asteroidSize;
            wasPaused = Global.getCombatEngine().isPaused();
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events)
        {
            final CombatEngineAPI engine = Global.getCombatEngine();
            if (engine.getCombatUI().isShowingCommandUI())
            {
                engine.getCombatUI().addMessage(0, Console.getSettings().getOutputColor(),
                        "Finished spawning asteroids.");
                engine.removePlugin(this);
                return;
            }

            final ViewportAPI view = engine.getViewport();
            final Vector2f mouseLoc = new Vector2f(view.convertScreenXToWorldX(Mouse.getX()),
                    view.convertScreenYToWorldY(Mouse.getY()));
            for (InputEventAPI event : events)
            {
                if (event.isConsumed())
                {
                    continue;
                }

                if (event.isKeyDownEvent() && event.getEventValue() == Keyboard.KEY_SPACE)
                {
                    event.consume();
                    engine.getCombatUI().addMessage(0, Console.getSettings().getOutputColor(),
                            "Finished spawning asteroids.");
                    engine.removePlugin(this);
                    engine.setPaused(wasPaused);
                    return;
                }
                else if (event.isMouseDownEvent() && event.getEventValue() == 0)
                {
                    if (!mouseDown)
                    {
                        mouseDown = true;
                        spawnLoc.set(mouseLoc);
                    }

                    event.consume();
                }
                else if (event.isMouseUpEvent() && event.getEventValue() == 0)
                {
                    if (mouseDown)
                    {
                        mouseDown = false;
                        final Vector2f velocity = Vector2f.sub(mouseLoc, spawnLoc, null);
                        final int size = (asteroidSize < 0 ? (int) (Math.random() * 4) : asteroidSize);
                        engine.spawnAsteroid(size, spawnLoc.x, spawnLoc.y, velocity.x, velocity.y);
                    }

                    event.consume();
                }
            }

            // Engine must be paused to work around mouse event consumption bug
            engine.setPaused(true);
        }

        @Override
        public void renderInWorldCoords(ViewportAPI view)
        {
            if (mouseDown)
            {
                final Vector2f mouseLoc = new Vector2f(view.convertScreenXToWorldX(Mouse.getX()),
                        view.convertScreenYToWorldY(Mouse.getY()));
                final Vector2f drawLoc;
                final boolean beyondRange;
                if (MathUtils.isWithinRange(spawnLoc, mouseLoc, 600))
                {
                    drawLoc = mouseLoc;
                    beyondRange = false;
                }
                else
                {
                    drawLoc = MathUtils.getPointOnCircumference(spawnLoc, 600f,
                            VectorUtils.getAngle(spawnLoc, mouseLoc));
                    beyondRange = true;
                }
                glDisable(GL_TEXTURE_2D);
                glLineWidth(5f);
                glColor(Console.getSettings().getOutputColor());
                DrawUtils.drawCircle(spawnLoc.x, spawnLoc.y, 10f, 16, false);
                glBegin(GL_LINES);
                glVertex2f(spawnLoc.x, spawnLoc.y);
                glVertex2f(drawLoc.x, drawLoc.y);
                if (beyondRange)
                {
                    glColor(Console.getSettings().getOutputColor(), 0.3f, false);
                    glVertex2f(drawLoc.x, drawLoc.y);
                    glVertex2f(mouseLoc.x, mouseLoc.y);
                }
                glEnd();
            }
        }
    }
}
