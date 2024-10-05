package org.lazywizard.console.cheatmanager

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import org.lazywizard.console.Console

private typealias CData = MutableMap<String, CombatCheatData>

class CombatCheatManager : BaseEveryFrameCombatPlugin() {
    private val icon = Global.getSettings().getSpriteName("ui", "console_status")
    private val plugins: CData by lazy(LazyThreadSafetyMode.NONE) { getPlugins() }

    companion object PluginManager {
        private const val DATA_ID = "lw_console_cheats"

        @Suppress("UNCHECKED_CAST")
        private fun getPlugins(): CData {
            return Global.getCombatEngine().customData.getOrPut(DATA_ID) { HashMap<String, CheatPlugin>() } as CData
        }

        @JvmStatic
        fun enableCheat(id: String, statusDesc: String, plugin: CheatPlugin, appliesTo: CheatTarget?) {
            val plugins = getPlugins()
            plugins[id]?.onEnd()

            val data = CombatCheatData(id, statusDesc, plugin, appliesTo)
            plugins[id] = data
            data.onStart()
        }

        @JvmStatic
        fun disableCheat(id: String) {
            getPlugins().remove(id)?.unapply()
        }

        @JvmStatic
        fun isEnabled(id: String) = getPlugins().containsKey(id)

        @JvmStatic
        fun getDefaultTarget(): CheatTarget = Console.getSettings().defaultCombatCheatTarget

        @JvmStatic
        fun isTarget(ship: ShipAPI, target: CheatTarget?): Boolean = when (target) {
            CheatTarget.PLAYER -> ship === Global.getCombatEngine().playerShip
            CheatTarget.FLEET -> ship.owner == 0
            CheatTarget.ENEMY -> ship.owner == 1
            CheatTarget.ALL -> true
            null -> false
        }

        @JvmStatic
        @JvmOverloads
        fun parseTargets(str: String, vararg allowedTargets: CheatTarget = CheatTarget.values()): CheatTarget? = try {
            val result = CheatTarget.valueOf(str.uppercase())
            if (result in allowedTargets) result else null
        } catch (ex: Exception) {
            null
        }
    }

    private fun buildStatusDesc(): String = plugins.values.sortedBy { it.statusDesc }.joinToString { it.statusDesc }

    override fun advance(amount: Float, events: List<InputEventAPI>) {
        if (plugins.isEmpty()) return

        Global.getCombatEngine().maintainStatusForPlayerShip(DATA_ID, icon, "Active cheats", buildStatusDesc(), true)
        for (data in plugins.values) data.advance(amount, events)
    }
}

private data class CombatCheatData(val id: String, val statusDesc: String, val plugin: CheatPlugin, val appliesTo: CheatTarget?) {
    fun appliesTo(ship: ShipAPI): Boolean = CombatCheatManager.isTarget(ship, appliesTo)

    fun advance(amount: Float, events: List<InputEventAPI>) {
        if (!plugin.runWhilePaused() && Global.getCombatEngine().isPaused) return

        plugin.advance(amount, events)
        if (appliesTo != null) for (ship in Global.getCombatEngine().ships) if (appliesTo(ship)) plugin.advance(ship, amount)
    }

    fun unapply() {
        if (appliesTo != null) for (ship in Global.getCombatEngine().ships) if (appliesTo(ship)) plugin.unapply(ship)
    }

    fun onStart() {
        plugin.onStart(Global.getCombatEngine())
    }

    fun onEnd() {
        unapply()
        plugin.onEnd(Global.getCombatEngine())
    }
}

enum class CheatTarget {
    PLAYER,
    FLEET,
    ENEMY,
    ALL
}

abstract class CheatPlugin {
    /** Called once per frame while the command is active, before [advance] is called on applicable ships. */
    open fun advance(amount: Float, events: List<@JvmSuppressWildcards InputEventAPI>) {}

    /** Called once per applicable ship per frame while the command is active. */
    open fun advance(ship: ShipAPI, amount: Float) {}

    /** Called when cheat is toggled off. Do not rely on this being called! */
    open fun unapply(ship: ShipAPI) {}

    /** Called when cheat is toggled on, before [advance] is called on any ships. */
    open fun onStart(engine: CombatEngineAPI) {}

    /** Only called when cheat is toggled off, after [unapply] is called on all ships. Do not rely on this being called! */
    open fun onEnd(engine: CombatEngineAPI) {}

    /** Whether the [advance] methods should be called while the game is paused. */
    abstract fun runWhilePaused(): Boolean
}
