package se.sics.mspsim.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import se.sics.mspsim.util.ActiveComponent;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.MapTable;

public class CommandHandler implements ActiveComponent, Runnable {

  private static final String SCRIPT_EXT = ".sc";
  private String scriptDirectory = "script";

  private Hashtable<String, Command> commands = new Hashtable<String, Command>();
  private boolean exit;
  private boolean workaround = false;

  private ArrayList<CommandContext[]> currentAsyncCommands = new ArrayList<CommandContext[]>();
  private BufferedReader inReader;
  private PrintStream out;
  private PrintStream err;
  private MapTable mapTable;
  private ComponentRegistry registry;
  private int pidCounter = 0;

  public CommandHandler() {
    exit = false;
    inReader = new BufferedReader(new InputStreamReader(System.in));
    out = System.out;
    err = System.err;

    registerCommands();
  }

  // Add it to the command table (overwriting anything there)
  public void registerCommand(String cmd, Command command) {
    commands.put(cmd, command);
  }


  private String readLine(BufferedReader inReader2) throws IOException {
    if (workaround) {
      StringBuilder str = new StringBuilder();
      while(true) {
        if (inReader2.ready()) {
          int c = inReader2.read();
          if (c == '\n') {
            return str.toString();
          }
          if (c != '\r') {
            str.append((char)c);
          }
        } else {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            throw new InterruptedIOException();
          }
        }
      }
    } else {
      return inReader2.readLine();
    }
  }

  public void run() {
    String lastLine = null;
    while(!exit) {
      try {
        out.print(">");
        out.flush();
        String line = readLine(inReader);//.readLine();
        // Simple execution of last called command line when not running from terminal with history support
        if (((char) 27 + "[A").equals(line)) {
          line = lastLine;          
        }
        if (line != null && line.length() > 0) {
          lastLine = line;
          executeCommand(line, null);
        }
      } catch (IOException e) {
        e.printStackTrace(err);
        err.println("Command line tool exiting...");
        exit = true;
      }
    }
  }

  public int executeCommand(String commandLine, CommandContext context) {
    String[][] parts;
    PrintStream out = context == null ? this.out : context.out;
    PrintStream err = context == null ? this.err : context.err;

    try {
      parts = CommandParser.parseCommandLine(commandLine);
    } catch (Exception e) {
      err.println("Error: failed to parse command:");
      e.printStackTrace(err);
      return -1;
    }
    if (parts == null || parts.length == 0) {
      // Nothing to execute
      return 0;
    }
    Command[] cmds = createCommands(parts);
    if(cmds != null && cmds.length > 0) {
      CommandContext[] commands = new CommandContext[parts.length];
      boolean error = false;
      int pid = -1;
      for (int i = 0; i < parts.length; i++) {
        String[] args = parts[i];
        Command cmd = cmds[i];
        if (i == 0 && cmd instanceof AsyncCommand) {
          pid = ++pidCounter;
        }
        commands[i] = new CommandContext(this, mapTable, commandLine, args, pid, cmd);
        if (i > 0) {
          PrintStream po = new PrintStream(new LineOutputStream((LineListener) commands[i].getCommand()));
          commands[i - 1].setOutput(po, err);
        }
        // Last element also needs output!
        if (i == parts.length - 1) {
          commands[i].setOutput(out, err);
        }
        // TODO: Check if first command is also LineListener and set it up for input!!
      }
      // Execute when all is set-up in opposite order...
      // TODO if error the command chain should be stopped
      for (int i = parts.length - 1; i >= 0; i--) {
        try {
          int code = commands[i].getCommand().executeCommand(commands[i]);
          if (code != 0) {
            err.println("command '" + commands[i].getCommandName() + "' failed with error code " + code);
            error = true;
          }
        } catch (Exception e) {
          err.println("Error: Command failed: " + e.getMessage());
          e.printStackTrace(err);
          error = true;
        }
      }
      if (error) {
        // TODO close any started commands
        return 1;
      } else if (pid >= 0) {
        synchronized (currentAsyncCommands) {
          currentAsyncCommands.add(commands);
        }
      }
      return 0;
    }
    return -1;
  }

  // This will return an instance that can be configured -
  // which is basically not OK... TODO - fix this!!!
  private Command getCommand(String cmd)  {
    Command command = commands.get(cmd);
    if (command != null) {
      try {
        return (Command) command.clone();
      } catch (CloneNotSupportedException e) {
        e.printStackTrace(err);
        return null;
      }
    }
    File scriptFile = new File(scriptDirectory, cmd + SCRIPT_EXT);
    if (scriptFile.isFile() && scriptFile.canRead()) {
      return new ScriptCommand(scriptFile);
    }
    return null;
  }

  private Command[] createCommands(String[][] commandList) {
    Command[] cmds = new Command[commandList.length];
    for (int i = 0; i < commandList.length; i++) {
      Command command = getCommand(commandList[i][0]);
      if (command == null) {
        err.println("CLI: Command not found: \"" + commandList[i][0] + "\". Try \"help\".");
        return null;
      }
      if (i > 0 && !(command instanceof LineListener)) {
        err.println("CLI: Error, command \"" + commandList[i][0] + "\" does not take input.");
        return null;
      }
      // TODO replace with command name
      String argHelp = command.getArgumentHelp(null);
      if (argHelp != null) {
        int requiredCount = 0;
        for (int j = 0, m = argHelp.length(); j < m; j++) {
          if (argHelp.charAt(j) == '<') {
            requiredCount++;
          }
        }
        if (requiredCount > commandList[i].length - 1) {
          // Too few arguments
          err.println("Too few arguments for " + commandList[i][0]);
          err.println("Usage: " + commandList[i][0] + ' ' + argHelp);
          return null;
        }
      }
      cmds[i] = command;
    }
    return cmds;
  }

  public void setComponentRegistry(ComponentRegistry registry) {
    this.registry = registry;
  }

  public void setWorkaround(boolean w) {
    workaround = w;
  }

  public void start() {
    mapTable = (MapTable) registry.getComponent(MapTable.class);

    Object[] commandBundles = registry.getAllComponents(CommandBundle.class);
    if (commandBundles != null) {
      for (int i = 0, n = commandBundles.length; i < n; i++) {
        ((CommandBundle) commandBundles[i]).setupCommands(registry, this);
      }
    }
    new Thread(this, "cmd").start();
  }

  private void registerCommands() {
    registerCommand("help", new BasicCommand("show help for the specified command or command list", "[command]") {
      public int executeCommand(CommandContext context) {
        if (context.getArgumentCount() == 0) {
          context.out.println("Available commands:");
          for(Map.Entry<String,Command> entry: commands.entrySet()) {
            String name = entry.getKey();
            Command command = entry.getValue();
            String helpText = command.getCommandHelp(name);
            if (helpText != null) {
              String argHelp = command.getArgumentHelp(name);
              String prefix = argHelp != null ? (' ' + name + ' ' + argHelp) : (' ' + name);
              int n;
              if ((n = helpText.indexOf('\n')) > 0) {
                // Show only first line as short help if help text consists of several lines
                helpText = helpText.substring(0, n);
              }
              context.out.print(prefix);
              if (prefix.length() < 8) {
                context.out.print('\t');
              }
              if (prefix.length() < 16) {
                context.out.print('\t');
              }
              context.out.println("\t " + helpText);
            }
          }
          return 0;
        }

        String cmd = context.getArgument(0);
        Command command = getCommand(cmd);
        if (command != null) {
          String helpText = command.getCommandHelp(cmd);
          String argHelp = command.getArgumentHelp(cmd);
          context.out.print(cmd);
          if (argHelp != null && argHelp.length() > 0) {
            context.out.print(' ' + argHelp);
          }
          context.out.println();
          if (helpText != null && helpText.length() > 0) {
            context.out.println("  " + helpText);
          }
          return 0;
        }
        context.err.println("Error: unknown command '" + cmd + '\'');
        return 1;
      }
    });
    registerCommand("workaround", new BasicCommand("activate workaround for Java console input bug", "") {
      public int executeCommand(CommandContext context) {
        workaround = true;
        return 0;
      }     
    });

    registerCommand("ps", new BasicCommand("list current executing commands", "") {
      public int executeCommand(CommandContext context) {
        for (int i = 0; i < currentAsyncCommands.size(); i++) {
          CommandContext cmd = currentAsyncCommands.get(i)[0];
          context.out.println("  " + cmd);
        }
        return 0;
      }
    });

    registerCommand("kill", new BasicCommand("kill a currently executing command", "<process>") {
      public int executeCommand(CommandContext context) {
        int pid = context.getArgumentAsInt(0);
        if (removePid(pid)) {
          return 0;
        }
        context.err.println("could not find the command to kill.");
        return 1;
      }
    });
  }

  public void exit(CommandContext commandContext, int exitCode, int pid) {
    if (pid >= 0) {
      removePid(pid);
    }
  }

  private boolean removePid(int pid) {
    CommandContext[] contexts = null;
    synchronized (currentAsyncCommands) {
      for (int i = 0, n = currentAsyncCommands.size(); i < n; i++) {
        CommandContext[] cntx = currentAsyncCommands.get(i);
        if (pid == cntx[0].getPID()) {
          contexts = cntx;
          currentAsyncCommands.remove(cntx);
          break;
        }
      }
    }
    if (contexts != null) {
      for (int i = 0; i < contexts.length; i++) {
        Command command = contexts[i].getCommand();
        // Stop any commands that have not yet been stopped...
        if (command instanceof AsyncCommand && !contexts[i].hasExited()) {
          AsyncCommand ac = (AsyncCommand) command;
          ac.stopCommand(contexts[i]);
        }
      }
      return true;
    }
    return false;
  }
}
