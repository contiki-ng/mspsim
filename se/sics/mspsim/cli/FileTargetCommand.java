package se.sics.mspsim.cli;

import java.io.IOException;
import java.util.Hashtable;

public class FileTargetCommand extends BasicLineCommand {
  FileTarget ft;
  Hashtable<String, FileTarget> fileTargets;
  private boolean print;
  private CommandContext context;
  
  public FileTargetCommand(Hashtable<String,FileTarget> fileTargets,
        String name, String desc, boolean print) {
    super(name, desc);
    this.fileTargets = fileTargets;
    this.print = print;
  }
  public int executeCommand(CommandContext context) {
    this.context = context;
    String fileName = context.getArgument(0);
    ft = fileTargets.get(fileName);
    if (ft == null) {
      try {
        ft = new FileTarget(fileName);
        fileTargets.put(fileName, ft);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return 0;
  }
  
  public void lineRead(String line) {
    ft.lineRead(line);
    if (print) context.out.println(line);
  }
  
  public void stopCommand(CommandContext context) {
    // Should this do anything?
    // Probably depending on the ft's config
  }
}
