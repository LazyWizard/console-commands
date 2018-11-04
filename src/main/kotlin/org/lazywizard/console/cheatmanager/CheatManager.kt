package org.lazywizard.console.cheatmanager

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI

private typealias CData = MutableMap<String, CombatCheatData>

class CombatCheatManager : BaseEveryFrameCombatPlugin() {
    private val icon = Global.getSettings().getSpriteName("ui", "console_status")

    private companion object PluginManager {
        private const val DATA_ID = "lw_console_cheats"
        private val plugins: CData by lazy(LazyThreadSafetyMode.NONE)
        {
            @Suppress("UNCHECKED_CAST")
            Global.getCombatEngine().customData.getOrPut(DATA_ID) { HashMap<String, CheatPlugin>() } as CData
        }

        @JvmStatic
        fun enableCheat(id: String, statusDesc: String, plugin: CheatPlugin, appliesTo: CheatTarget) {
            plugins[id]?.onEnd()

            val data = CombatCheatData(id, statusDesc, plugin, appliesTo)
            plugins[id] = data
            data.onStart()
        }

        @JvmStatic
        fun disableCheat(id: String) {
            plugins.remove(id)?.unapply()
        }

        @JvmStatic
        fun isEnabled(id: String) = plugins.containsKey(id)

        @JvmStatic
        @JvmOverloads
        fun parseTargets(str: String, vararg allowedTargets: CheatTarget = CheatTarget.values()): CheatTarget? = try {
            val result = CheatTarget.valueOf(str.toUpperCase())
            if (result in allowedTargets) result else null
        } catch (ex: Exception) {
            null
        }
    }

    private fun buildStatusDesc(): String = plugins.values.sortedBy { it.statusDesc }.joinToString { it.statusDesc }

    override fun advance(amount: Float, events: List<InputEventAPI>) {
        if (plugins.isEmpty()) return

        Global.getCombatEngine().maintainStatusForPlayerShip(DATA_ID, icon, "Active cheats", buildStatusDesc(), true)
        for (data in plugins.values) {
            data.advance(amount)
        }
    }
}

private data class CombatCheatData(val id: String, val statusDesc: String, val plugin: CheatPlugin, val appliesTo: CheatTarget) {
    fun appliesTo(ship: ShipAPI): Boolean = when (appliesTo) {
        CheatTarget.PLAYER -> ship === Global.getCombatEngine().playerShip
        CheatTarget.FLEET -> ship.owner == 0
        CheatTarget.ENEMY -> ship.owner == 1
        CheatTarget.ALL -> true
    }

    fun advance(amount: Float) {
        for (ship in Global.getCombatEngine().ships) if (appliesTo(ship)) plugin.advance(ship, amount)
    }

    fun unapply() {
        for (ship in Global.getCombatEngine().ships) if (appliesTo(ship)) plugin.unapply(ship)
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
    /** Called once per applicable ship per frame the command is active. */
    abstract fun advance(ship: ShipAPI, amount: Float)

    /** Called when cheat is toggled off, before [onEnd]. Do not rely on this being called! */
    open fun unapply(ship: ShipAPI) {}

    /** Called when cheat is toggled on, before [advance] is called on any ships. */
    open fun onStart(engine: CombatEngineAPI) {}

    /** Only called when cheat is toggled off - do not rely on this being called! */
    open fun onEnd(engine: CombatEngineAPI) {}
}
