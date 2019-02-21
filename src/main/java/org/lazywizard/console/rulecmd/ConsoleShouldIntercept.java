package org.lazywizard.console.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

import java.util.List;
import java.util.Map;

public class ConsoleShouldIntercept extends BaseCommandPlugin
{
    private static boolean ENABLED = false;

    public static void setIntercepting(boolean shouldIntercept)
    {
        ENABLED = shouldIntercept;
    }

    public static boolean shouldIntercept()
    {
        return ENABLED;
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap)
    {
        return ENABLED;
    }
}
