package org.lazywizard.console.overlay.v2.settings

import lunalib.lunaSettings.LunaSettingsListener

class ConsoleLunaSettingsListener : LunaSettingsListener {
    override fun settingsChanged(modID: String) {
        if (modID == "lw_console") {
            ConsoleV2Settings.update()
        }
    }
}