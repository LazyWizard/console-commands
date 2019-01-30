package org.lazywizard.console

import com.fs.starfarer.api.input.InputEventAPI
import org.lazywizard.console.cheatmanager.CheatTarget
import org.lazywizard.lazylib.JSONUtils
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.reflect.KProperty

/*
    To add a new setting:
     - Add a var here, ensuring it uses one of the common data-backed delegates (IntPref, StringPref, etc)
     - Update the Settings command's pop-up dialog to support changing it
 */
object ConsoleSettings {
    private val settings = JSONUtils.loadCommonJSON(CommonStrings.PATH_COMMON_DATA)
    var fontScaling by FloatPref("fontScaling", default = 1.0f)
    var commandSeparator by StringPref("commandSeparator", default = ";")
    var maxScrollback by IntPref("maxScrollback", default = 10_000)
    var typoCorrectionThreshold by FloatPref("typoCorrectionThreshold", default = 0.9f)
    var showBackground by BoolPref("showBackground", default = true)
    var transferStorageToHome by BoolPref("transferStorageToHome", default = true)
    var devModeTogglesDebugFlags by BoolPref("devModeTogglesDebugFlags", default = true)
    var defaultCombatCheatTarget by EnumPref("defaultCombatCheatTarget", enumClass = CheatTarget::class.java, default = CheatTarget.PLAYER)
    var showEnteredCommands by BoolPref("showEnteredCommands", default = true)
    var showMemoryUsage by BoolPref("showMemoryUsage", default = true)
    var showCursorIndex by BoolPref("showCursorIndex", default = false)
    var showExceptionDetails by BoolPref("showExceptionDetails", default = false)
    var outputColor by ColorPref("outputColor", default = Color(0, 255, 255))
    var consoleSummonKey by KeystrokePref("consoleKeystroke",
            default = Keystroke(Keyboard.getKeyIndex("BACK"), true, false, false))

    fun resetToDefaults() {
        JSONUtils.clear(settings)
        settings.save()
    }

    //<editor-fold defaultstate="collapsed" desc="Delegate implementations">
    private class StringPref(val key: String, default: String) {
        private var field = settings.optString(key, default)

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): String = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: String) {
            field = value
            settings.put(key, value)
            settings.save()
        }
    }

    private class BoolPref(val key: String, default: Boolean) {
        private var field = settings.optBoolean(key, default)

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Boolean = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Boolean) {
            field = value
            settings.put(key, value)
            settings.save()
        }
    }

    private class IntPref(val key: String, default: Int) {
        private var field = settings.optInt(key, default)

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Int = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Int) {
            field = value
            settings.put(key, value)
            settings.save()
        }
    }

    private class FloatPref(val key: String, default: Float) {
        private var field = settings.optDouble(key, default.toDouble()).toFloat()

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Float = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Float) {
            field = value
            settings.put(key, value)
            settings.save()
        }
    }

    private class ColorPref(val key: String, default: Color) {
        private var field = parseColor(settings.optString(key, asString(default)))

        private fun asString(color: Color): String = "${color.red}|${color.green}|${color.blue}"
        private fun parseColor(color: String): Color {
            val components = color.split('|').map { Integer.parseInt(it) }
            return Color(components[0].coerceIn(0, 255), components[1].coerceIn(0, 255), components[2].coerceIn(0, 255))
        }


        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Color = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Color) {
            field = value
            settings.put(key, asString(value))
            settings.save()
        }
    }

    private class EnumPref<T : Enum<T>>(val key: String, enumClass: Class<T>, default: T) {
        private var field = try {
            java.lang.Enum.valueOf(enumClass, settings.optString(key, default.name))
        } catch (ex: Exception) {
            default
        }

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): T = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: T) {
            field = value
            settings.put(key, value.name)
            settings.save()
        }
    }

    private class KeystrokePref(val key: String, default: Keystroke) {
        private var field = parseKeystroke(settings.optString(key, asString(default)))

        private fun asString(keystroke: Keystroke) = "${keystroke.keyCode}|${keystroke.ctrl}|${keystroke.alt}|${keystroke.shift}"
        private fun parseKeystroke(keystroke: String): Keystroke {
            val components = keystroke.split('|')
            return Keystroke(Integer.parseInt(components[0]),
                    java.lang.Boolean.parseBoolean(components[1]),
                    java.lang.Boolean.parseBoolean(components[2]),
                    java.lang.Boolean.parseBoolean(components[3]))
        }

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Keystroke = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Keystroke) {
            field = value
            settings.put(key, asString(value))
            settings.save()
        }
    }
    //</editor-fold>

    class Keystroke(val keyCode: Int, val ctrl: Boolean, val alt: Boolean, val shift: Boolean) {

        fun isPressed(events: List<InputEventAPI>): Boolean {
            for (event in events) {
                if (event.isConsumed || !event.isKeyDownEvent || event.eventValue != keyCode) continue

                if (ctrl && !event.isCtrlDown) return false
                if (alt && !event.isAltDown) return false
                if (shift && !event.isShiftDown) return false

                event.consume()
                return true
            }

            return false
        }

        override fun toString(): String {
            var str = if (keyCode == Keyboard.KEY_BACK) "BACKSPACE" else Keyboard.getKeyName(keyCode).toUpperCase()
            if (shift) str = "SHIFT+$str"
            if (alt) str = "ALT+$str"
            if (ctrl) str = "CONTROL+$str"
            return str
        }
    }
}

