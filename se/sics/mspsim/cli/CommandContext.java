package se.sics.mspsim.cli;
import java.io.PrintStream;

import se.sics.mspsim.core.MSP430Constants;
import se.sics.mspsim.util.MapTable;

public class CommandContext {

  private String[] args;
  private String commandLine;
  private MapTable mapTable;
  private int pid = -1;
  private boolean exited = false;
  private Command command;
  
  public PrintStream out;
  public PrintStream err;
  private CommandHandler commandHandler;
  
  public CommandContext(CommandHandler ch, MapTable table, String commandLine, String[] args,
			int pid, Command command, PrintStream out, PrintStream err) {
    this(ch, table, commandLine, args, pid, command);
    setOutput(out, err);
  }
  
  public CommandContext(CommandHandler ch,MapTable table, String commandLine, String[] args,
			int pid, Command command) {
    this.commandLine = commandLine;
    this.args = args;
    this.pid = pid;
    this.mapTable = table;
    this.command = command;
    this.commandHandler = ch;
  }
  
  void setOutput(PrintStream out, PrintStream err) {
    this.out = out;
    this.err = err;
  }
  
  Command getCommand( ) {
    return command;
  }
  
  String getCommandLine() {
    return commandLine;
  }

  public int getPID() {
    return pid;
  }

  public boolean hasExited() {
    return exited;
  }
  
  /**
   * exit needs to be called as soon as the command is completed (or stopped).
   * @param exitCode - the exit code of the command
   */
  public void exit(int exitCode) {
    // TODO: Clean up can be done now!
    exited = true;
    commandHandler.exit(this, exitCode, pid);
  }

  public MapTable getMapTable() {
    return mapTable;
  }

  public String getCommandName() {
    return args[0];
  }

  public int getArgumentCount() {
    return args.length - 1;
  }
  
  public String getArgument(int index) {
    return args[index + 1];
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
    return -1;
  }

  public int getArgumentAsRegister(int index) {
    String symbol = getArgument(index);
    for (int i = 0, n = MSP430Constants.REGISTER_NAMES.length; i < n; i++) {
      if (MSP430Constants.REGISTER_NAMES[i].equals(symbol)) {
        return i;
      }
    }
    String reg = symbol.startsWith("R") ? symbol.substring(1) : symbol;
    try {
      int register = Integer.parseInt(reg);
      if (register >= 0 && register <= 15) {
        return register;
      } else {
        err.println("illegal register: " + symbol);
      }
    } catch (Exception e) {
      err.println("illegal register: " + symbol);
    }
    return -1;
  }

  public int getArgumentAsInt(int index) {
    try {
      return Integer.parseInt(getArgument(index));
    } catch (Exception e) {
      err.println("Illegal number format: " + getArgument(index));
    }
    return 0;
  }

  public long getArgumentAsLong(int index) {
    try {
      return Long.parseLong(getArgument(index));
    } catch (Exception e) {
      err.println("Illegal number format: " + getArgument(index));
    }
    return 0L;
  }

  public float getArgumentAsFloat(int index) {
    try {
      return Float.parseFloat(getArgument(index));
    } catch (Exception e) {
      err.println("Illegal number format: " + getArgument(index));
    }
    return 0f;
  }

  public double getArgumentAsDouble(int index) {
    String arg = getArgument(index);
    try {
      return Double.parseDouble(arg);
    } catch (Exception e) {
      err.println("Illegal number format: " + getArgument(index));
    }
    return 0.0;
  }

  public int executeCommand(String command) {
    return commandHandler.executeCommand(command);
  }

  public String toString() {
    return (pid >= 0 ? ("" + pid) : "?") + '\t' + (commandLine == null ? getCommandName() : commandLine);
  }

}
