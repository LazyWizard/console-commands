package org.lazywizard.console;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.ui.Fonts;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Level;
import org.lazywizard.console.commands.ReloadConsole;
import org.lazywizard.console.overlay.v2.panels.ConsoleOverlayPanel;
import org.lazywizard.console.overlay.v2.settings.ConsoleLunaSettingsListener;
import org.lazywizard.console.overlay.v2.settings.ConsoleV2Settings;

import java.io.IOException;

public class ConsoleModPlugin extends BaseModPlugin
{
    // TODO: Remove in a future update (only here for compatibility with users of the dev versions)
    private static void migrateSettings() throws IOException
    {
        try
        {
            final SettingsAPI settings = Global.getSettings();
            final String oldPath = "lw_console_settings.json";
            final String oldSettings = settings.readTextFileFromCommon(oldPath);

            // Check if migration is necessary
            if (oldSettings.trim().isEmpty())
            {
                return;
            }

            settings.writeTextFileToCommon(CommonStrings.PATH_COMMON_DATA, oldSettings);
            settings.deleteTextFileFromCommon(oldPath);
            Console.showMessage("Console settings successfully migrated to new version.");
        }
        catch (Exception ex)
        {
            Console.showException("Failed to migrate console settings! Run the 'Settings' command to restore them.", ex);
        }
    }

    // Config file is either empty (never used Settings), or an empty JSONObject (used "settings reset")
    private static boolean needsSetup()
    {
        try
        {
            return Global.getSettings().readTextFileFromCommon(CommonStrings.PATH_COMMON_DATA).trim().length() < 5;
        }
        catch (IOException ex)
        {
            return true;
        }
    }

    @Override
    public void onApplicationLoad() throws Exception
    {

        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            LunaSettings.addSettingsListener(new ConsoleLunaSettingsListener());
            ConsoleV2Settings.update();
        }

        ConsoleOverlayPanel.loadStaticVariables();

        try
        {
            Global.getSettings().getScriptClassLoader().loadClass("org.lazywizard.lazylib.LazyLib");
        }
        catch (ClassNotFoundException ex)
        {
            Global.getLogger(Console.class).error("LazyLib not found!", ex);
            throw new RuntimeException("LazyLib must be installed for the console to function!\n" +
                    "LazyLib can be downloaded here:\nhttps://fractalsoftworks.com/forum/index.php?topic=5444.0");
        }

        migrateSettings();

        // Load console settings - implementing it in ReloadConsole ensures the command will work identically
        ReloadConsole.reloadConsole();

        Console.showMessage("Console loaded, summon with " + Console.getSettings().getConsoleSummonKey() + ".", Level.DEBUG);

        if (needsSetup())
        {
            Console.showMessage("Use the Settings command to configure the console.", Level.DEBUG);
        }
    }

    @Override
    public void onGameLoad(boolean newGame)
    {
        Global.getSector().getListenerManager().addListener(new ConsoleCampaignListener(), true);
    }
}
