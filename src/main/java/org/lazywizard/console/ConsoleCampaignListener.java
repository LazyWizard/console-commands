package org.lazywizard.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.lazylib.StringUtils;

import java.util.List;

public class ConsoleCampaignListener implements CampaignInputListener, ConsoleListener
{
    @Override
    public int getListenerInputPriority()
    {
        return 9999;
    }

    @Override
    public void processCampaignInputPreCore(List<InputEventAPI> events)
    {
        final CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if ((ui.isShowingDialog() && getContext() != CommandContext.CAMPAIGN_MARKET) || ui.isShowingMenu())
        {
            return;
        }

        if (Console.getSettings().getConsoleSummonKey().isPressed(events))
        {
            ConsoleOverlay.show(getContext());
        }

        Console.advance(this);
    }

    @Override
    public void processCampaignInputPreFleetControl(List<InputEventAPI> events)
    {
    }

    @Override
    public void processCampaignInputPostCore(List<InputEventAPI> events)
    {
    }

    @Override
    public boolean showOutput(String output)
    {
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
        final InteractionDialogAPI dialog = Global.getSector().getCampaignUI().getCurrentInteractionDialog();
        if (dialog != null && dialog.getInteractionTarget().getMarket() != null)
        {
            return CommandContext.CAMPAIGN_MARKET;
        }

        return CommandContext.CAMPAIGN_MAP;
    }
}
