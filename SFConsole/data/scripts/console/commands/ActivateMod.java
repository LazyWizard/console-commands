package data.scripts.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import data.scripts.console.BaseCommand;

public class ActivateMod extends BaseCommand
{
    @Override
    protected String getHelp()
    {
        return "Activates a mod's generator. The argument is the fully-qualified"
                + " name of the generator's class (for example:"
                + " data.scripts.world.corvus.AddMod).";
    }

    @Override
    protected String getSyntax()
    {
        return "activatemod <fully qualified name of generator>";
    }

    @Override
    protected boolean runCommand(String args)
    {
        args = args.trim();

        if (args.endsWith(".java"))
        {
            args = args.replace("/", ".").replace("\\", ".");
            args = args.substring(0, args.lastIndexOf(".java"));
        }

        if (args.isEmpty() || args.contains(" "))
        {
            showSyntax();
            return false;
        }

        Class genClass;

        try
        {
            genClass = Global.getSettings().getScriptClassLoader().loadClass(args);
        }
        catch (ClassNotFoundException ex)
        {
            showMessage("No generator found with that name! Is the mod activated"
                    + " in the launcher?");
            return false;
        }

        if (!SectorGeneratorPlugin.class.isAssignableFrom(genClass))
        {
            showMessage("Class '" + args + "' does not implement"
                    + " SectorGeneratorPlugin!");
            return false;
        }

        /*int confirm = JOptionPane.showConfirmDialog(null,
         "Are you sure you wish to activate this generator? This action"
         + " will break your game if the mod is not fully compatible"
         + " with all currently activated mods!", "Warning!",
         JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

         if (confirm != JOptionPane.YES_OPTION)
         {
         return false;
         }*/

        SectorGeneratorPlugin generator;

        try
        {
            generator = (SectorGeneratorPlugin) genClass.newInstance();
        }
        catch (InstantiationException ex)
        {
            showError("Error while running generator: ", ex);
            return false;
        }
        catch (IllegalAccessException ex)
        {
            showError("Error while running generator: ", ex);
            return false;
        }
        catch (ClassCastException ex)
        {
            showError("Error while running generator: ", ex);
            return false;
        }

        generator.generate(getSector());
        showMessage("Generator ran successfully, mod should be active now.");
        return true;
    }
}
