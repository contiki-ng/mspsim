package se.sics.mspsim.cli;

import java.io.BufferedReader;
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

  // Add it to the hashtable (overwriting anything there)
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
    while(!exit) {
      try {
        out.print(">");
        out.flush();
        String line = readLine(inReader);//.readLine();
        if (line != null && line.length() > 0) {
          String[][] parts = CommandParser.parseLine(line);
          if(parts.length > 0 && checkCommands(parts) == 0) {
            CommandContext[] commands = new CommandContext[parts.length];
	    boolean error = false;
	    int pid = -1;
            for (int i = 0; i < parts.length; i++) {
              String[] args = parts[i];
              Command cmd = getCommand(args[0]);
	      if (i == 0 && cmd instanceof AsyncCommand) {
		pid = ++pidCounter;
	      }
              commands[i] = new CommandContext(this, mapTable, line, args, pid, cmd);
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
		  err.println("command '" + commands[i].getCommandName()
			      + "' failed with error code " + code);
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
	    } else if (pid >= 0) {
              currentAsyncCommands.add(commands);
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace(err);
        err.println("Command line tool exiting...");
        exit = true;
      }
    }
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
      }
    }
    return null;
  }
    
  private int checkCommands(String[][] cmds) {
    for (int i = 0; i < cmds.length; i++) {
      Command command = commands.get(cmds[i][0]);
      if (command == null) {
        err.println("CLI: Command not found: " + cmds[i][0]);
        return -1;
      }
      if (i > 0 && !(command instanceof LineListener)) {
        err.println("CLI: Error " + cmds[i][0] + " does not take input");
        return -1;
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
	if (requiredCount > cmds[i].length - 1) {
	  // Too few arguments
	  err.println("Too few arguments for " + cmds[i][0]);
	  err.println("Usage: " + cmds[i][0] + ' ' + argHelp);
	  return -1;
	}
      }
    }
    return 0;
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
    registerCommand("help", new BasicCommand("shows help for the specified command or command list", "[command]") {
      public int executeCommand(CommandContext context) {
        if (context.getArgumentCount() == 0) {
          context.out.println("Available commands:");
          for(Map.Entry<String,Command> entry: commands.entrySet()) {
            String name = entry.getKey();
            Command command = entry.getValue();
            String prefix = ' ' + name + ' ' + command.getArgumentHelp(name);
            String helpText = command.getCommandHelp(name);
            int n;
            if (helpText != null && (n = helpText.indexOf('\n')) > 0) {
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
          return 0;
        }

        String cmd = context.getArgument(0);
        Command command = commands.get(cmd);
        if (command != null) {
          context.out.println(cmd + ' ' + command.getArgumentHelp(cmd));
          context.out.println("  " + command.getCommandHelp(cmd));
          return 0;
        }
        context.err.println("Error: unknown command '" + cmd + '\'');
        return 1;
      }
    });
    registerCommand("workaround", new BasicCommand("", "") {
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
        removePid(pid);
        return 0;
      }
    });
  }

  public void exit(CommandContext commandContext, int exitCode, int pid) {
    if (pid >= 0) {
      removePid(pid);
    }
  }
  
  private void removePid(int pid) {
    System.out.println("Removing pid: " + pid);
    for (int i = 0; i < currentAsyncCommands.size(); i++) {
      CommandContext[] contexts = currentAsyncCommands.get(i);
      CommandContext cmd = contexts[0];
      if (pid == cmd.getPID()) {
        for (int j = 0; j < contexts.length; j++) {
          Command command = contexts[i].getCommand();
          // Stop any commands that have not yet been stopped...
          if (command instanceof AsyncCommand && !contexts[i].hasExited()) {
            AsyncCommand ac = (AsyncCommand) command;
            ac.stopCommand(contexts[i]);
          }
        }
        currentAsyncCommands.remove(contexts);
        break;
      }
    }
  }
}
