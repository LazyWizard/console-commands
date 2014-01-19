package org.lazywizard.console;

import com.fs.starfarer.api.EveryFrameScript;

public class ConsoleCampaignScript implements EveryFrameScript
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
