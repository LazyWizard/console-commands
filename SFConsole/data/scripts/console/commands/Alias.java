package data.scripts.console.commands;

import data.scripts.console.BaseCommand;

public class Alias extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Allows you to set a shortcut for console commands. For example,"
                + " 'alias ac addcredits' would allow you to later use the"
                + " command 'ac 5000' as shorthand.";
    }

    @Override
    protected String getSyntax()
    {
        return "addalias <alias> <command>";
    }

    @Override
    protected boolean runCommand(String args)
    {
        String[] tmp = args.split(" ");

        if (tmp.length < 2)
        {
            showSyntax();
            return false;
        }
        else if (tmp[0].equalsIgnoreCase("alias") || tmp[1].equalsIgnoreCase("alias"))
        {
            showMessage("Nice try!");
            return false;
        }


        String alias = tmp[0];
        StringBuilder arg = new StringBuilder();

        for (int x = 1; x < tmp.length; x++)
        {
            if (x != 1)
            {
                arg.append(" ");
            }

            arg.append(tmp[x]);
        }

        String command = arg.toString();

        if (!getConsole().addAlias(alias, command))
        {
            showMessage("Alias failed! Does this alias already exist as a command?");
            return false;
        }

        showMessage("'" + alias + "' successfully aliased for '" + command + "'.");
        return true;
    }
}
