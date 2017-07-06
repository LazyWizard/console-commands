package org.lazywizard.console

import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.prefs.Preferences
import kotlin.reflect.KProperty

object ConsoleSettings {
    private val prefs = Preferences.userNodeForPackage(Console::class.java)
    var commandSeparator by StringPref("commandSeparator", ";")
    var maxScrollback by IntPref("maxScrollback", 10_000)
    var typoCorrectionThreshold by FloatPref("typoCorrectionThreshold", 0.9f)
    var shouldShowEnteredCommands by BoolPref("showEnteredCommands", true)
    var shouldShowCursorIndex by BoolPref("showCursorIndex", false)
    var shouldShowExceptionDetails by BoolPref("showExceptionDetails", false)
    var outputColor by ColorPref("outputColor", Color(255, 255, 0))
    var consoleSummonKey by KeystrokePref("consoleKeystroke",
            Keystroke(Keyboard.getKeyIndex("BACK"), false, true, false))

    fun resetToDefaults() = prefs.clear()

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
            return Color(components[0], components[1], components[2])
        }


        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Color = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Color) {
            field = value
            prefs.put(key, asString(value))
        }
    }

    private class KeystrokePref(val key: String, default: Keystroke) {
        private var field = parseKeystroke(prefs.get(key, asString(default)))

        private fun asString(keystroke: Keystroke) = "${keystroke.key}|${keystroke.requiresShift}|${keystroke.requiresControl}|${keystroke.requiresAlt}"
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

    class Keystroke(val key: Int, val requiresShift: Boolean, val requiresControl: Boolean, val requiresAlt: Boolean) {
        fun isPressed(): Boolean {
            if (!Keyboard.isKeyDown(key)) return false

            if (requiresShift && !(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)
                    || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) return false
            if (requiresControl && !(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                    || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) return false
            if (requiresAlt && !(Keyboard.isKeyDown(Keyboard.KEY_LMENU)
                    || Keyboard.isKeyDown(Keyboard.KEY_RMENU))) return false

            return true
        }

        override fun toString(): String {
            var str = if (key == Keyboard.KEY_BACK) "BACKSPACE" else Keyboard.getKeyName(key).toUpperCase()
            if (requiresShift) str = "SHIFT+" + str
            if (requiresAlt) str = "ALT+" + str
            if (requiresControl) str = "CONTROL+" + str
            return str
        }
    }
}

