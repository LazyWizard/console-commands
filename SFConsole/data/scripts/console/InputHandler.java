package data.scripts.console;

import org.lwjgl.input.Keyboard;

public class InputHandler extends Thread
{
    private static final long FRAMERATE = (long) (1000 / 20);
    private static int consoleKey = Keyboard.KEY_GRAVE;
    private static volatile transient InputHandler instance = new InputHandler();
    private transient boolean isPressed = false;
    private boolean shouldExit = false;

    private InputHandler()
    {
    }

    public synchronized static InputHandler getInputHandler()
    {
        if (instance == null || instance.shouldExit)
        {
            instance = new InputHandler();
        }

        return instance;
    }

    public static void stopInputHandler()
    {
        instance.shouldExit = true;
    }

    public static void setConsoleKey(int key)
    {
        consoleKey = key;
    }

    @Override
    public void run()
    {
        Console.showMessage("Keyhandler started.");

        while (!shouldExit)
        {
            try
            {
                if (!isPressed)
                {
                    if (Keyboard.isKeyDown(consoleKey))
                    {
                        isPressed = true;

                        // Due to a bug with LWJGL input and window focus, the console
                        // will only activate once the console key is released
                    }
                }
                else
                {
                    if (!Keyboard.isKeyDown(consoleKey))
                    {
                        isPressed = false;

                        if (!ConsoleManager.allowConsole())
                        {
                            ConsoleManager.showRestrictions();
                            return;
                        }

                        Console.getInput();
                    }
                }

                Thread.sleep(FRAMERATE);
            }
            catch (InterruptedException ex)
            {
                Console.showMessage("Keyhandler interrupted!");
                return;
            }
        }

        Console.showMessage("Keyhandler stopped.");
    }
}
