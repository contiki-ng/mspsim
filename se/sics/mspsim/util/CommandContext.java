package se.sics.mspsim.util;

import java.io.InputStream;
import java.io.PrintStream;


public class CommandContext {

  private String[] args;
  private MapTable mapTable;
  public final PrintStream out;
  public final PrintStream err;
  public final InputStream in;
  
  public CommandContext(MapTable table, String[] args,
      InputStream in, PrintStream out, PrintStream err) {
    this.args = args;
    this.out = out;
    this.err = err;
    this.in = in;
    mapTable = table;
  }
  
  public int getArgumentCount() {
    return args.length - 1;
  }
  
  public String getArgument(int index) {
    return args[index + 1];
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
  
}
