package org.lazywizard.console.commands;

import com.fs.starfarer.api.Global;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ScriptEvaluator;
import org.json.JSONArray;
import org.json.JSONException;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.BaseCommand.CommandContext;
import org.lazywizard.console.BaseCommand.CommandResult;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class RunCode implements BaseCommand
{
    private static ScriptEvaluator eval;

    public static void reloadImports()
    {
        if (eval == null)
        {
            createEval();
        }

        List<String> imports = new ArrayList<>();

        try
        {
            JSONArray csv = Global.getSettings().getMergedSpreadsheetDataForMod(
                    "import", CommonStrings.RUNCODE_CSV_PATH, CommonStrings.MOD_ID);
            for (int x = 0; x < csv.length(); x++)
            {
                imports.add(csv.getJSONObject(x).getString("import"));
            }
        }
        catch (IOException | JSONException ex)
        {
            Console.showException("Failed to load RunCode imports: ", ex);
            return;
        }

        eval.setDefaultImports(imports.toArray(new String[imports.size()]));
    }

    private static void createEval()
    {
        eval = new ScriptEvaluator();
        eval.setReturnType(void.class);
        eval.setParentClassLoader(Global.getSettings().getScriptClassLoader());
        eval.setThrownExceptions(new Class[]
        {
            Exception.class
        });
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (args.isEmpty())
        {
            return CommandResult.BAD_SYNTAX;
        }

        // Yes, this is relatively slow. But for obvious security reasons,
        // I'd rather allow only manually-entered code to run
        if (!Console.class.getCanonicalName().equals(
                new Throwable().getStackTrace()[1].getClassName()))
        {
            Console.showMessage("A mod attempted to execute arbitrary code on your machine!");
            return CommandResult.ERROR;
        }

        if (!args.endsWith(";"))
        {
            args += ";";
        }

        // This is only triggered if imports weren't set somehow
        if (eval == null)
        {
            reloadImports();
        }

        try
        {
            eval.cook(args);
            eval.evaluate(null);
        }
        catch (CompileException ex)
        {
            Console.showException("Compilation failed: ", ex);
            return CommandResult.ERROR;
        }
        catch (InvocationTargetException ex)
        {
            // Unwrap exception to get at actual error details
            Console.showException("Execution failed: ", ex.getTargetException());
            return CommandResult.ERROR;
        }
        catch (Exception ex)
        {
            Console.showException("Execution failed: ", ex);
            return CommandResult.ERROR;
        }

        return CommandResult.SUCCESS;
    }
}
