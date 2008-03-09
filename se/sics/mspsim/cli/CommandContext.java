package se.sics.mspsim.cli;

import java.io.InputStream;
import java.io.PrintStream;

import se.sics.mspsim.util.MapTable;


public class CommandContext {

  private String[] args;
  private MapTable mapTable;
  private int pid;
  
  public final PrintStream out;
  public final PrintStream err;
  public final InputStream in;
  
  public CommandContext(MapTable table, String[] args,
      int pid, InputStream in, PrintStream out, PrintStream err) {
    this.args = args;
    this.out = out;
    this.err = err;
    this.in = in;
    this.pid = pid;
    mapTable = table;
  }
  
  public int getPID() {
    return pid;
  }
  
  /**
   * exit needs to be called as soon as the command is completed (or stopped).
   * @param exitCode - the exit code of the command
   */
  public void exit(int exitCode) {
    // TODO: Clean up can be done now!
  }
  
  public int getArgumentCount() {
    return args.length - 1;
  }
  
  public String getArgument(int index) {
    return args[index + 1];
  }
  
  public MapTable getMapTable() {
    return mapTable;
  }
  
  public String getCommand() {
    return args[0];
  }
  
  public int getArgumentAsAddress(int index) {
    String adr = getArgument(index);
    if (adr == null || adr.length() == 0) return 0;
    adr = adr.trim();
    if (adr.charAt(0) == '$') {
      try {
        return Integer.parseInt(adr.substring(1), 16);
      } catch (Exception e) {
        err.println("Illegal hex number format: " + adr);
      }
    } else if (Character.isDigit(adr.charAt(0))) {
      try {
        return Integer.parseInt(adr); 
      } catch (Exception e) {
        err.println("Illegal number format: " + adr);
      }
    } else {
      // Assume that it is a symbol
      if (mapTable != null) {
        return mapTable.getFunctionAddress(adr);
      }
    }
    return 0;
  }

  public int getArgumentAsInt(int i) {
    try {
      return Integer.parseInt(getArgument(i));
    } catch (Exception e) {
      err.println("Illegal number format: " + getArgument(i));
    }
    return 0;
  }
  
}
