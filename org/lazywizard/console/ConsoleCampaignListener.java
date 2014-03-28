package org.lazywizard.console;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lwjgl.input.Keyboard;

public class ConsoleCampaignListener implements EveryFrameScript
{
    // Here to work around a LWJGL input bug that will probably never be fixed
    private transient boolean isPressed = false;

    @Override
    public boolean isDone()
    {
        return false;
    }

    @Override
    public boolean runWhilePaused()
    {
        return true;
    }

    // TODO: replace with something much simpler once the dialog is added
    private boolean checkInput()
    {
        KeyStroke key = Console.getSettings().getConsoleSummonKey();

        if (!isPressed)
        {
            boolean modPressed = true;

            if (key.requiresShift()
                    && !(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                    || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)))
            {
                modPressed = false;
            }

            if (key.requiresControl()
                    && !(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                    || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)))
            {
                modPressed = false;
            }

            if (key.requiresAlt()
                    && !(Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                    || Keyboard.isKeyDown(Keyboard.KEY_RMENU)))
            {
                modPressed = false;
            }

            if (modPressed && Keyboard.isKeyDown(key.getKey()))
            {
                isPressed = true;
            }
        }
        else
        {
            if (!Keyboard.isKeyDown(key.getKey()))
            {
                isPressed = false;
                return true;
            }
        }

        return false;
    }

    @Override
    public void advance(float amount)
    {
        if (checkInput())
        {
            Global.getSector().getCampaignUI().showInteractionDialog(
                    new ConsoleCampaignPopup(), null);

            // TODO: write an interaction dialog plugin for this!
            /*String rawInput = JOptionPane.showInputDialog(null,
                    "Enter command, or 'help' for a list of valid commands.");
            Console.parseInput(rawInput, CommandContext.CAMPAIGN_MAP);

            // Temporary!
            ConsoleCombatListener.resetKeyboard();*/
        }

        Console.advance(CommandContext.CAMPAIGN_MAP);
    }
}
