package se.sics.mspsim.cli;

import java.util.Hashtable;
import java.util.Iterator;

import se.sics.mspsim.util.ComponentRegistry;

public class FileCommands implements CommandBundle {

    private Hashtable <String, FileTarget> fileTargets = new Hashtable<String, FileTarget>();

    public void setupCommands(final ComponentRegistry registry, CommandHandler handler) {
        // TODO: this should also be "registered" as a "sink".
        // probably this should be handled using ">" instead!
        handler.registerCommand(">", new FileTargetCommand(fileTargets,
            null, "<filename>", false));
        handler.registerCommand("tee", new FileTargetCommand(fileTargets,
            "redirect to file and std-out", "<filename>", true));

        handler.registerCommand("fclose", new BasicCommand("close the specified file", "<filename>") {
          public int executeCommand(CommandContext context) {
            String name = context.getArgument(0);
            FileTarget ft = fileTargets.get(name);
            if (ft != null) {
              context.out.println("Closing file " + name);
              fileTargets.remove(name);
              ft.close();
              return 0;
            } else {
              context.err.println("Could not find the open file " + name);
              return 1;
            }
          }
        });

        handler.registerCommand("files", new BasicCommand("list open files", "") {
          public int executeCommand(CommandContext context) {
            for (Iterator<FileTarget> iterator = fileTargets.values().iterator(); iterator.hasNext();) {
              FileTarget type = iterator.next();
              context.out.println(type.getName());
            }
            return 0;
          }
        });
    }
        
}
