package org.lazywizard.console.overlay.v2.settings

import lunalib.lunaSettings.LunaSettings
import org.lazywizard.console.ConsoleSettings
import org.lazywizard.console.ConsoleSettings.Keystroke
import org.lazywizard.console.ConsoleSettings.KeystrokePref
import org.lwjgl.input.Keyboard
import java.awt.Color

object ConsoleV2Settings {

    var MOD_ID = "lw_console"

   /* var consoleKeybind = Keyboard.KEY_BACK
    var needsHoldCTRL = true
    var needsHoldShift = false
    var needsHoldAlt = false*/

    @JvmStatic
    var consoleKeybind = ConsoleSettings.Keystroke(Keyboard.KEY_BACK, true, false, false)

    var enableBackgroundBlur = true
    var backgroundDarkening = 0.875f
    var textInputColor =  Color(243, 245,  250)
    var textOutputColor =  Color(188, 190, 196)
    var matchColor = Color(170,222,255)

    var enableAutocomplete = true
    var useContextSensitiveSuggestions = false
    var enableTabCycling = false
    var maxAutocompletionsCount = 20

    var showCommandInfo = false
    var showCompileErrors = true

    var showRAMandVRAMusage = true

    //Only called from the ConsoleLunaSettingsListener class
    @JvmStatic
    fun update() {

        /*consoleKeybind = LunaSettings.getInt(MOD_ID, "console_openKeybind")!!
        needsHoldCTRL = LunaSettings.getBoolean(MOD_ID, "console_holdCTRL")!!
        needsHoldShift = LunaSettings.getBoolean(MOD_ID, "console_holdSHIFT")!!
        needsHoldAlt = LunaSettings.getBoolean(MOD_ID, "console_holdALT")!!*/

        consoleKeybind = Keystroke(LunaSettings.getInt(MOD_ID, "console_openKeybind")!!,
            LunaSettings.getBoolean(MOD_ID, "console_holdCTRL")!!,
            LunaSettings.getBoolean(MOD_ID, "console_holdALT")!!,
            LunaSettings.getBoolean(MOD_ID, "console_holdSHIFT")!!
        )

        enableBackgroundBlur = LunaSettings.getBoolean(MOD_ID, "console_enableBackgroundBlur")!!
        backgroundDarkening = LunaSettings.getFloat(MOD_ID, "console_backgroundDarkening2")!!
        textInputColor = LunaSettings.getColor(MOD_ID, "console_inputTextColor")!!
        textOutputColor = LunaSettings.getColor(MOD_ID, "console_outputTextColor")!!
        matchColor = LunaSettings.getColor(MOD_ID, "console_matchColor")!!

        enableAutocomplete = LunaSettings.getBoolean(MOD_ID, "console_enableAutocomplete")!!
        useContextSensitiveSuggestions = LunaSettings.getBoolean(MOD_ID, "console_contextSensitiveSuggestions")!!
        enableTabCycling = LunaSettings.getBoolean(MOD_ID, "console_enableTabCycling")!!
        maxAutocompletionsCount = LunaSettings.getInt(MOD_ID, "console_autocompletionsCount")!!

        showCommandInfo = LunaSettings.getBoolean(MOD_ID, "console_showCommandInfo")!!
        showCompileErrors = LunaSettings.getBoolean(MOD_ID, "console_showCompileErrors")!!


        showRAMandVRAMusage = LunaSettings.getBoolean(MOD_ID, "console_showRAMAndVram")!!
        var test = ""
    }

}