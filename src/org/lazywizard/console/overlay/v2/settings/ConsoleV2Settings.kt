package org.lazywizard.console.overlay.v2.settings

import lunalib.lunaSettings.LunaSettings
import java.awt.Color

object ConsoleV2Settings {

    var MOD_ID = "lw_console"

    var enableBackgroundBlur = true
    var backgroundDarkening = 0.9f
    var textInputColor =  Color(243, 245,  250)
    var textOutputColor =  Color(188, 190, 196)
    var matchColor = Color(170,222,255)

    var enableAutocomplete = true
    var useContextSensitiveSuggestions = false
    var enableTabCycling = false
    var maxAutocompletionsCount = 20

    var showRAMandVRAMusage = true

    //Only called from the ConsoleLunaSettingsListener class
    @JvmStatic
    fun update() {
        enableBackgroundBlur = LunaSettings.getBoolean(MOD_ID, "console_enableBackgroundBlur")!!
        backgroundDarkening = LunaSettings.getFloat(MOD_ID, "console_backgroundDarkening")!!
        textInputColor = LunaSettings.getColor(MOD_ID, "console_inputTextColor")!!
        textOutputColor = LunaSettings.getColor(MOD_ID, "console_outputTextColor")!!
        matchColor = LunaSettings.getColor(MOD_ID, "console_matchColor")!!

        enableAutocomplete = LunaSettings.getBoolean(MOD_ID, "console_enableAutocomplete")!!
        useContextSensitiveSuggestions = LunaSettings.getBoolean(MOD_ID, "console_contextSensitiveSuggestions")!!
        enableTabCycling = LunaSettings.getBoolean(MOD_ID, "console_enableTabCycling")!!
        maxAutocompletionsCount = LunaSettings.getInt(MOD_ID, "console_autocompletionsCount")!!

        showRAMandVRAMusage = LunaSettings.getBoolean(MOD_ID, "console_showRAMAndVram")!!
        var test = ""
    }

}