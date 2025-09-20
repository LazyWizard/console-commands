package org.lazywizard.console

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.Misc
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.console.overlay.legacy.addToHistory
import org.lazywizard.console.overlay.legacy.show
import org.lazywizard.console.overlay.v2.panels.ConsoleOverlayPanel
import org.lazywizard.lazylib.StringUtils
import java.util.*

internal class ConsoleCampaignListener : CampaignInputListener, ConsoleListener {
    override fun getListenerInputPriority(): Int = 9999

    override fun processCampaignInputPreCore(events: MutableList<InputEventAPI>) {
        if (Global.getSector().campaignUI.isShowingMenu) return


        if (Console.getSettings().legacyConsoleSummonKey.isPressed(events)) {
            show(context)
            events.clear()
        }
        else if (ConsoleOverlayPanel.instance == null && Console.getSettings().consoleSummonKey.isPressed(events)) {
            ConsoleOverlayPanel(context)
            //show(context)
            events.clear()
        }



        Console.advance(this)
    }

    override fun processCampaignInputPreFleetControl(events: List<InputEventAPI>) {}

    override fun processCampaignInputPostCore(events: List<InputEventAPI>) {}

    override fun showOutput(output: String): Boolean {
        /*for (tmp in output.split('\n').dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val message = StringUtils.wrapString(tmp, 100)
            Global.getSector().campaignUI.addMessage(message, Console.getSettings().outputColor)
        }*/

        addToHistory(output)
        return true
    }

    override fun getContext(): CommandContext {
        return if (Global.getSector().campaignUI?.currentInteractionDialog?.interactionTarget?.market != null) {
            CommandContext.CAMPAIGN_MARKET
        } else CommandContext.CAMPAIGN_MAP
    }
}

internal class ConsoleCombatListener : BaseEveryFrameCombatPlugin(), ConsoleListener {
    private lateinit var context: CommandContext

    override fun processInputPreCoreControls(amount: Float, events: MutableList<InputEventAPI>) {
        if (!::context.isInitialized || Global.getCombatEngine().playerShip == null) return

        if (Console.getSettings().legacyConsoleSummonKey.isPressed(events)) {
            show(context)
            events.clear()
        }
        else if (ConsoleOverlayPanel.instance == null && Console.getSettings().consoleSummonKey.isPressed(events)) {
            ConsoleOverlayPanel(context)
            //show(context)
            events.clear()
        }



        // Advance the console and all combat commands
        Console.advance(this)
    }

    override fun init(engine: CombatEngineAPI) {
        // Determine what context this battle is in
        context = when {
            engine.isSimulation -> CommandContext.COMBAT_SIMULATION
            engine.isInCampaign -> CommandContext.COMBAT_CAMPAIGN
            engine.missionId != null -> CommandContext.COMBAT_MISSION
            else -> CommandContext.MAIN_MENU
        }

        engine.customData["consolePlugin"] = this
    }

    override fun getContext() = context

    override fun showOutput(output: String): Boolean {
        val ui = Global.getCombatEngine()?.combatUI ?: return false

        // Fallback if the console overlay doesn't exist for some reason
        /*val messages = output.split('\n').dropLastWhile { it.isEmpty() }.toTypedArray()
        Arrays.asList(*messages).reverse()
        for (tmp in messages) {
            val message = StringUtils.wrapString(tmp, 80)
            ui.addMessage(0, Console.getSettings().outputColor, message)
        }*/

        addToHistory(output)
        return true
    }
}
