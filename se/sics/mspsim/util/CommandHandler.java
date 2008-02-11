package se.sics.mspsim.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Hashtable;

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
          out.println(getCommandHelp(context));
        } else {
          String cmd = context.getArgument(0);
          Command command = commands.get(cmd);
          if (command != null) {
            out.println(command.getCommandHelp(context));
          }
        }
        return 0;
      }
      public String getArgumentHelp(CommandContext context) {
        return "help takes one argumet which is the command to print help text for";
      }
      public String getCommandHelp(CommandContext context) {
        return "help <commandname> gives some help for the command";
      }    
    });
  }
  
  // Add it to the hashtable (overwriting anything there)
  public void registerCommand(String cmd, Command command) {
    commands.put(cmd, command);
  }

  
  public void run() {
    while(!exit) {
       try {
        out.print(">");
        String line = inReader.readLine();
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
    new Thread(this).start();
    mapTable = (MapTable) registry.getComponent(MapTable.class);
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
          return "argument 1 - addres to something";
        }
        public String getCommandHelp(CommandContext context) {
          return "test - tests the command system";
        }});
    cmd.run();
  }
}
