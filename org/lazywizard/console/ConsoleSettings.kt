package org.lazywizard.console

import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.prefs.Preferences
import kotlin.reflect.KProperty

object ConsoleSettings {
    private val prefs = Preferences.userNodeForPackage(Console::class.java)
    var commandSeparator by StringPref("commandSeparator", ";")
    var typoCorrectionThreshold by FloatPref("typoCorrectionThreshold", 0.9f)
    var shouldShowEnteredCommands by BoolPref("showEnteredCommands", true)
    var shouldShowCursorIndex by BoolPref("showCursorIndex", false)
    var shouldShowExceptionDetails by BoolPref("showExceptionDetails", false)
    var outputColor by ColorPref("outputColor", Color(255, 255, 0))
    val consoleSummonKey by KeyStrokePref("consoleKeyStroke",
            KeyStroke(Keyboard.getKeyIndex("BACK"), false, true, false))

    private class StringPref(val key: String, val default: String) {
        private var field = prefs.get(key, default)

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): String = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: String) {
            field = value;
            prefs.put(key, value)
        }
    }

    private class BoolPref(val key: String, val default: Boolean) {
        private var field = prefs.getBoolean(key, default)

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Boolean = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Boolean) {
            field = value;
            prefs.putBoolean(key, value)
        }
    }

    private class IntPref(val key: String, val default: Int) {
        private var field = prefs.getInt(key, default)

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Int = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Int) {
            field = value;
            prefs.putInt(key, value)
        }
    }

    private class FloatPref(val key: String, val default: Float) {
        private var field = prefs.getFloat(key, default)

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Float = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Float) {
            field = value;
            prefs.putFloat(key, value)
        }
    }

    private class ColorPref(val key: String, val default: Color) {
        private var field = Color(prefs.getInt("${key}R", default.red),
                prefs.getInt("${key}G", default.green),
                prefs.getInt("${key}B", default.blue),
                prefs.getInt("${key}A", default.alpha))

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): Color = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: Color) {
            field = value
            prefs.putInt("${key}R", value.red)
            prefs.putInt("${key}G", value.green)
            prefs.putInt("${key}B", value.blue)
            prefs.putInt("${key}A", value.alpha)
        }
    }

    private class KeyStrokePref(val key: String, val default: KeyStroke) {
        private var field = KeyStroke(prefs.getInt("${key}Key", default.key),
                prefs.getBoolean("${key}RequiresShift", default.requiresShift),
                prefs.getBoolean("${key}RequiresControl", default.requiresControl),
                prefs.getBoolean("${key}RequiresAlt", default.requiresAlt))

        operator fun getValue(consoleSettings: ConsoleSettings, property: KProperty<*>): KeyStroke = field
        operator fun setValue(consoleSettings: ConsoleSettings, property: KProperty<*>, value: KeyStroke) {
            field = value
            prefs.putInt("${key}Key", value.key)
            prefs.putBoolean("${key}RequiresShift", value.requiresShift)
            prefs.putBoolean("${key}RequiresControl", value.requiresControl)
            prefs.putBoolean("${key}RequiresAlt", value.requiresAlt)
        }
    }

    class KeyStroke(val key: Int, val requiresShift: Boolean, val requiresControl: Boolean, val requiresAlt: Boolean) {
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

