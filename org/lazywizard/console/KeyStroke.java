package org.lazywizard.console;

class KeyStroke
{
    private final int key;
    private final boolean requireShift;
    private final boolean requireControl;

    public KeyStroke(int key, boolean requireShift, boolean requireControl)
    {
        this.key = key;
        this.requireShift = requireShift;
        this.requireControl = requireControl;
    }

    public int getKey()
    {
        return key;
    }

    public boolean requiresShift()
    {
        return requireShift;
    }

    public boolean requiresControl()
    {
        return requireControl;
    }
}
