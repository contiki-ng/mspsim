package se.sics.mspsim.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Map;

public class CommandHandler implements ActiveComponent, Runnable {

  private Hashtable<String, Command> commands = new Hashtable<String, Command>();
  private boolean exit;
  
  private BufferedReader inReader;
  private InputStream in;
  private PrintStream out;
  private PrintStream err;
  private MapTable mapTable;
  private ComponentRegistry registry;
  
  public CommandHandler() {
    exit = false;
    inReader = new BufferedReader(new InputStreamReader(in = System.in));
    out = System.out;
    err = System.err;
    
    registerCommand("help", new Command() {
      public int executeCommand(CommandContext context) {
        if (context.getArgumentCount() == 0) {
	  context.out.println("Available commands:");
	  for(Map.Entry entry: commands.entrySet()) {
	    String name = (String) entry.getKey();
	    Command command = (Command) entry.getValue();
	    CommandContext cc = new CommandContext(mapTable, new String[] {
	      name
	    }, context.in, context.out, context.err);
	    String prefix = ' ' + name + ' ' + command.getArgumentHelp(cc);
	    String helpText = command.getCommandHelp(cc);
	    int n;
	    if (helpText != null && (n = helpText.indexOf('\n')) > 0) {
	      helpText = helpText.substring(0, n);
	    }
	    context.out.print(prefix);
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
	  CommandContext cc = new CommandContext(mapTable, new String[] {
	    cmd
	  }, context.in, context.out, context.err);
	  context.out.println(cmd + ' ' + command.getArgumentHelp(cc));
	  context.out.println("  " + command.getCommandHelp(cc));
	  return 0;
	}
	context.err.println("Error: unknown command '" + cmd + '\'');
	return 1;
      }
      public String getArgumentHelp(CommandContext context) {
        return "<command>";
      }
      public String getCommandHelp(CommandContext context) {
        return "shows help for the specified command";
      }    
    });
  }
  
  // Add it to the hashtable (overwriting anything there)
  public void registerCommand(String cmd, Command command) {
    commands.put(cmd, command);
  }

  
  private String readLine(BufferedReader inReader2) throws IOException {
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
  }
  
  public void run() {
    while(!exit) {
       try {
        out.print(">");
        out.flush();
        String line = readLine(inReader);//.readLine();
        if (line != null && line.length() > 0) {
          String[] parts = line.split(" ");
          Command cmd = commands.get(parts[0]);
          if (cmd == null) {
            out.println("Error: Unknown command " + parts[0]);
          } else {
            CommandContext cc = new CommandContext(mapTable, parts, in, out, err);
            try {
              cmd.executeCommand(cc);
            } catch (Exception e) {
              err.println("Error: Command failed: " + e.getMessage());
              e.printStackTrace(err);
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
        err.println("Command line tool exiting...");
        exit = true;
      }
    }
  }

  public void setComponentRegistry(ComponentRegistry registry) {
    this.registry = registry;
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

  
  
  public static void main(String[] args) {
    CommandHandler cmd = new CommandHandler();
    cmd.registerCommand("test", new Command() {
        public int executeCommand(CommandContext c) {
          System.out.println("Test exected " + c.getCommand() +
              c.getArgumentAsAddress(0));
          return 0;
        }
    
        public String getArgumentHelp(CommandContext context) {
          return "argument 1 - address to something";
        }
        public String getCommandHelp(CommandContext context) {
          return "test - tests the command system";
        }});
    cmd.run();
  }
}
