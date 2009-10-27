package se.sics.mspsim.cli;

import java.io.IOException;
import java.util.Hashtable;

public class FileTargetCommand extends BasicLineCommand {
  private final Hashtable<String, FileTarget> fileTargets;
  private final boolean print;
  private final boolean append;

  private FileTarget ft;
  private CommandContext context;
  
  public FileTargetCommand(Hashtable<String,FileTarget> fileTargets,
        String name, String desc, boolean print, boolean append) {
    super(name, desc);
    this.fileTargets = fileTargets;
    this.print = print;
    this.append = append;
  }

  public int executeCommand(CommandContext context) {
    this.context = context;
    String fileName = context.getArgument(0);
    synchronized (fileTargets) {
      ft = fileTargets.get(fileName);
      if (ft == null) {
        try {
          ft = new FileTarget(fileTargets, fileName, append);
        } catch (IOException e) {
          e.printStackTrace(context.err);
          return -1;
        }
      } else if (!append) {
        context.err.println("File already opened: can not overwrite");
        return -1;
      }
      ft.addContext(context);
    }
    return 0;
  }

  public void lineRead(String line) {
    if (print) context.out.println(line);
    ft.lineRead(context, line);
  }

  public void stopCommand(CommandContext context) {
    if (ft != null) {
      ft.removeContext(context);
    }
  }
}
