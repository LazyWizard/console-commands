package org.lazywizard.console

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.input.InputEventAPI
import org.apache.log4j.Level
import org.lazywizard.console.BaseCommand.CommandContext
import org.lazywizard.console.commands.DebugDialogs
import org.lazywizard.console.commands.ReloadConsole
import org.lazywizard.lazylib.StringUtils
import java.util.*

internal class ConsoleModPlugin : BaseModPlugin() {
    private var debugDialogs = false

    // TODO: Remove in a future update (only here for compatibility with users of the dev versions)
    private fun migrateSettings() {
        try {
            val settings = Global.getSettings()
            val oldPath = "lw_console_settings.json"
            val oldSettings = settings.readTextFileFromCommon(oldPath)
            if (oldSettings.trim().isEmpty()) return

            settings.writeTextFileToCommon(CommonStrings.PATH_COMMON_DATA, oldSettings)
            settings.writeTextFileToCommon(oldPath, "") // TODO: Delete old file if/when it becomes possible
            Console.showMessage("Console settings successfully migrated to new version.")
        } catch (ex: Exception) {
            Console.showException("Failed to migrate console settings! Run the 'Settings' command to restore them.", ex)
        }
    }

    // Config file is either empty (never used Settings), or an empty JSONObject (used "settings reset")
    private fun needsSetup() = Global.getSettings().readTextFileFromCommon(CommonStrings.PATH_COMMON_DATA).trim().length < 5

    @Throws(Exception::class)
    override fun onApplicationLoad() {
        migrateSettings()

        // Load console settings
        ReloadConsole.reloadConsole()

        Console.showMessage("Console loaded, summon with ${Console.getSettings().consoleSummonKey}.", Level.DEBUG);
        if (needsSetup()) Console.showMessage("The console has not been customized yet. Use the Settings command to configure it.", Level.DEBUG);
    }

    override fun onGameLoad(newGame: Boolean) {
        Global.getSector().listenerManager.addListener(ConsoleCampaignListener(), true)
    }

    override fun beforeGameSave() {
        // Ensure DebugDialogs doesn't persist between saves
        val memory = Global.getSector().memory
        debugDialogs = memory.getBoolean(DebugDialogs.MEMORY_KEY)
        if (debugDialogs) memory.unset(DebugDialogs.MEMORY_KEY)
    }

    override fun afterGameSave() {
        if (debugDialogs) Global.getSector().memory.set(DebugDialogs.MEMORY_KEY, true)
    }
}

internal class ConsoleCampaignListener : CampaignInputListener, ConsoleListener {
    override fun getListenerInputPriority(): Int = 9999

    override fun processCampaignInputPreCore(events: List<InputEventAPI>) {
        val ui = Global.getSector().campaignUI
        if ((ui.isShowingDialog && context != CommandContext.CAMPAIGN_MARKET) || ui.isShowingMenu) {
            return
        }

        if (Console.getSettings().consoleSummonKey.isPressed(events)) {
            show(context)
        }

        Console.advance(this)
    }

    override fun processCampaignInputPreFleetControl(events: List<InputEventAPI>) {}

    override fun processCampaignInputPostCore(events: List<InputEventAPI>) {}

    override fun showOutput(output: String): Boolean {
        for (tmp in output.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val message = StringUtils.wrapString(tmp, 100)
            Global.getSector().campaignUI.addMessage(message, Console.getSettings().outputColor)
        }

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

    override fun processInputPreCoreControls(amount: Float, events: List<InputEventAPI>) {
        val engine = Global.getCombatEngine() ?: return

        // Main menu check
        val player = engine.playerShip
        if (player != null) {
            if (Console.getSettings().consoleSummonKey.isPressed(events)) {
                show(context)
            }

            // Advance the console and all combat commands
            Console.advance(this)
        }
    }

    override fun init(engine: CombatEngineAPI) {
        // Determine what context this battle is in
        context = when {
            engine.isSimulation -> CommandContext.COMBAT_SIMULATION
            engine.isInCampaign -> CommandContext.COMBAT_CAMPAIGN
            else -> CommandContext.COMBAT_MISSION
        }
    }

    override fun getContext(): CommandContext {
        return context
    }

    override fun showOutput(output: String): Boolean {
        val ui = Global.getCombatEngine()?.combatUI ?: return false

        // Fallback if the console overlay doesn't exist for some reason
        val messages = output.split('\n').dropLastWhile { it.isEmpty() }.toTypedArray()
        Arrays.asList(*messages).reverse()
        for (tmp in messages) {
            val message = StringUtils.wrapString(tmp, 80)
            ui.addMessage(0, Console.getSettings().outputColor, message)
        }

        addToHistory(output)
        return true
    }
}
