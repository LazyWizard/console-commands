package org.lazywizard.console;

class ConsoleException extends Exception
{
    ConsoleException(String message) { super(message); }
    ConsoleException(String message, Throwable cause) { super(message, cause); }
    ConsoleException(Throwable cause) { super(cause); }
}
