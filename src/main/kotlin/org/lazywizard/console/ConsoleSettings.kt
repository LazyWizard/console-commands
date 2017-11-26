package org.lazywizard.console

import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.prefs.Preferences
import kotlin.reflect.KProperty

/*
    To add a new setting:
     - Add a var here, ensuring it uses one of the backing preference delegates
     - Update the Settings command's pop-up dialog to support it
 */
object ConsoleSettings {
    private val prefs = Preferences.userNodeForPackage(Console::class.java)
    var commandSeparator by StringPref("commandSeparator", default = ";")
    var maxScrollback by IntPref("maxScrollback", default = 10_000)
    var typoCorrectionThreshold by FloatPref("typoCorrectionThreshold", default = 0.9f)
    var shouldTransferStorageToHome by BoolPref("transferStorageToHome", default = false)
    var shouldShowEnteredCommands by BoolPref("showEnteredCommands", default = true)
    var shouldShowMemoryUsage by BoolPref("showMemoryUsage", default = true)
    var shouldShowCursorIndex by BoolPref("showCursorIndex", default = false)
    var shouldShowExceptionDetails by BoolPref("showExceptionDetails", default = false)
    var outputColor by ColorPref("outputColor", default = Color(255, 255, 0))
    var consoleSummonKey by KeystrokePref("consoleKeystroke",
            default = Keystroke(Keyboard.getKeyIndex("BACK"), true, false, false))

    fun resetToDefaults() = prefs.clear()

    //<editor-fold defaultstate="collapsed" desc="Preference-backed delegates">
    private class StringPref(val key: String, default: String) {
        private var field = prefs.get(key, default)

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): String = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: String) {
            field = value
            prefs.put(key, value)
        }
    }

    private class BoolPref(val key: String, default: Boolean) {
        private var field = prefs.getBoolean(key, default)

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Boolean = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Boolean) {
            field = value
            prefs.putBoolean(key, value)
        }
    }

    private class IntPref(val key: String, default: Int) {
        private var field = prefs.getInt(key, default)

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Int = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Int) {
            field = value
            prefs.putInt(key, value)
        }
    }

    private class FloatPref(val key: String, default: Float) {
        private var field = prefs.getFloat(key, default)

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Float = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Float) {
            field = value
            prefs.putFloat(key, value)
        }
    }

    private class ColorPref(val key: String, default: Color) {
        private var field = parseColor(prefs.get(key, asString(default)))

        private fun asString(color: Color): String = "${color.red}|${color.green}|${color.blue}"
        private fun parseColor(color: String): Color {
            val components = color.split('|').map { Integer.parseInt(it) }
            return Color(components[0].coerceIn(0, 255), components[1].coerceIn(0, 255), components[2].coerceIn(0, 255))
        }


        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Color = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Color) {
            field = value
            prefs.put(key, asString(value))
        }
    }

    private class KeystrokePref(val key: String, default: Keystroke) {
        private var field = parseKeystroke(prefs.get(key, asString(default)))

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
            prefs.put(key, asString(value))
        }
    }
    //</editor-fold>

    class Keystroke(val keyCode: Int, val ctrl: Boolean, val alt: Boolean, val shift: Boolean) {
        fun isPressed(): Boolean {
            if (!Keyboard.isKeyDown(keyCode)) return false

            if (ctrl && !(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                    || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) return false
            if (alt && !(Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                    || Keyboard.isKeyDown(Keyboard.KEY_RMENU))) return false
            if (shift && !(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                    || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) return false

            return true
        }

        override fun toString(): String {
            var str = if (keyCode == Keyboard.KEY_BACK) "BACKSPACE" else Keyboard.getKeyName(keyCode).toUpperCase()
            if (shift) str = "SHIFT+" + str
            if (alt) str = "ALT+" + str
            if (ctrl) str = "CONTROL+" + str
            return str
        }
    }
}

