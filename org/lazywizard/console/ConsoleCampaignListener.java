package org.lazywizard.console;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.ConsoleSettings.KeyStroke;
import org.lazywizard.lazylib.StringUtils;
import org.lwjgl.input.Keyboard;

public class ConsoleCampaignListener implements EveryFrameScript, ConsoleListener
{
    private transient float timeUntilNotify = 0.5f;

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

    private boolean checkInput()
    {
        KeyStroke key = Console.getSettings().getConsoleSummonKey();
        boolean modPressed = true;

        if (key.requiresShift() && !(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)))
        {
            modPressed = false;
        }

        if (key.requiresControl() && !(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)))
        {
            modPressed = false;
        }

        if (key.requiresAlt() && !(Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                || Keyboard.isKeyDown(Keyboard.KEY_RMENU)))
        {
            modPressed = false;
        }

        return (modPressed && Keyboard.isKeyDown(key.getKey()));
    }

    @Override
    public void advance(float amount)
    {
        if (timeUntilNotify > 0f)
        {
            timeUntilNotify -= amount;
        }

        if (checkInput())
        {
            ConsoleOverlay.show(getContext());
        }

        Console.advance(amount, this);
    }

    @Override
    public boolean showOutput(String output)
    {
        if (timeUntilNotify > 0f)
        {
            return false;
        }

        for (String message : output.split("\n"))
        {
            message = StringUtils.wrapString(message, 80);
            Global.getSector().getCampaignUI().addMessage(message,
                    Console.getSettings().getOutputColor());
        }

        return true;
    }

    @Override
    public CommandContext getContext()
    {
        return CommandContext.CAMPAIGN_MAP;
    }
}
