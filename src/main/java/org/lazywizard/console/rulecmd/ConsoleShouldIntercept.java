package org.lazywizard.console.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

import java.util.List;
import java.util.Map;

public class ConsoleShouldIntercept extends BaseCommandPlugin
{
    private static boolean intercepting = false;

    public static void setIntercepting(boolean shouldIntercept)
    {
        intercepting = shouldIntercept;
    }

    public static boolean shouldIntercept()
    {
        return intercepting;
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap)
    {
        return intercepting;
    }
}
