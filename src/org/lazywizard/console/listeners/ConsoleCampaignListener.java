package org.lazywizard.console.listeners;

import com.fs.starfarer.api.EveryFrameScript;

public class ConsoleCampaignListener implements EveryFrameScript
{
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
        // TODO: Check for input
    }
}
