package data.scripts.console;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.input.Keyboard;

class ConsoleOptions
{
    // Console constants
    public static final boolean REQUIRE_DEV_MODE = false;
    public static final boolean REQUIRE_RUN_WINDOWED = true;
    public static final String COMMAND_PACKAGE = "data.scripts.console.commands";
    public static final Color CONSOLE_COLOR = Color.YELLOW;
    public static final int LINE_LENGTH = 80;
    public static final long INPUT_FRAMERATE = (long) (1000 / 20);
    public static final int DEFAULT_CONSOLE_KEY = Keyboard.KEY_GRAVE;
    public static final int REBIND_KEY = Keyboard.KEY_F1; // Shift+key to rebind
    public static final List<Integer> RESTRICTED_KEYS = new ArrayList<Integer>();

    static
    {
        // These keys can't be bound to summon the console
        RESTRICTED_KEYS.add(REBIND_KEY);
        RESTRICTED_KEYS.add(Keyboard.KEY_ESCAPE);
        RESTRICTED_KEYS.add(Keyboard.KEY_LMETA);
        RESTRICTED_KEYS.add(Keyboard.KEY_RMETA);
        RESTRICTED_KEYS.add(Keyboard.KEY_LSHIFT);
        RESTRICTED_KEYS.add(Keyboard.KEY_RSHIFT);
    }
}
