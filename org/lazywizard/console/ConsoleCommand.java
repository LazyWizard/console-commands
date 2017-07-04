package org.lazywizard.console;

import java.util.List;

public interface ConsoleCommand extends BaseCommand
{
    List<String> getValidAutocompletions(String prefix);
}
