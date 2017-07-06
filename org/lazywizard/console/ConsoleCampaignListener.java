package org.lazywizard.console;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.lazylib.StringUtils;

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

    @Override
    public void advance(float amount)
    {
        if (timeUntilNotify > 0f)
        {
            timeUntilNotify -= amount;
        }

        final CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (!ui.isShowingDialog() && !ui.isShowingMenu() && Console.getSettings().getConsoleSummonKey().isPressed())
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
