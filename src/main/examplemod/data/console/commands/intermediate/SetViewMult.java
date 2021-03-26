package data.console.commands.intermediate;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.List;

public class SetViewMult implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (!context.isInCombat())
        {
            Console.showMessage(CommonStrings.ERROR_COMBAT_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        final float zoom;
        if (args.isEmpty())
        {
            zoom = Global.getCombatEngine().getViewport().getViewMult();
            Console.showMessage("Current view mult: " + zoom + ".");
            return CommandResult.SUCCESS;
        }

        try
        {
            zoom = Float.parseFloat(args);
        }
        catch (NumberFormatException ex)
        {
            return CommandResult.BAD_SYNTAX;
        }

        final ViewportAPI view = Global.getCombatEngine().getViewport();
        final float mod = 1f / view.getViewMult() * zoom,
        newWidth = view.getVisibleWidth() * mod,
        newHeight = view.getVisibleHeight() * mod;
        view.set(view.getLLX(), view.getLLY(), newWidth, newHeight);
        Global.getCombatEngine().addPlugin(new BaseEveryFrameCombatPlugin()
        {
            @Override
            public void advance(float amount, List<InputEventAPI> events)
            {
                if (view.getViewMult() == zoom)
                {
                    Console.showMessage("New zoom reached.");
                    //Global.getCombatEngine().removePlugin(this);
                    return;
                }

                view.set(view.getLLX(), view.getLLY(), newWidth, newHeight);
            }
        });

        //view.setViewMult(zoom);
        Console.showMessage("Set view mult to " + zoom + " (" + view.getViewMult() + " actual).");
        return CommandResult.SUCCESS;
    }
}
