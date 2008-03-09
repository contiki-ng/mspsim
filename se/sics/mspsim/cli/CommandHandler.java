package se.sics.mspsim.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
            }, 0, null, context.out, context.err);
            String prefix = ' ' + name + ' ' + command.getArgumentHelp(cc);
            String helpText = command.getCommandHelp(cc);
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
          CommandContext cc = new CommandContext(mapTable, new String[] {
              cmd
          }, 0, null, context.out, context.err);
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
    registerCommand("workaround", new BasicCommand("", "") {
      public int executeCommand(CommandContext context) {
        workaround = true;
        return 0;
      }     
    });
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
            // TODO add support for pipes
            CommandContext[] commands = new CommandContext[parts.length];
            for (int i = 0; i < parts.length; i++) {
              String[] args = parts[i];
              Command cmd = getCommand(args[0]);
              commands[i] = new CommandContext(mapTable, args, 0, cmd);
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
            for (int i = parts.length - 1; i >= 0; i--) {
              try {
                commands[i].getCommand().executeCommand(commands[i]);
              } catch (Exception e) {
                err.println("Error: Command failed: " + e.getMessage());
                e.printStackTrace(err);
              }
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

  // This will return an instance that can be configured -
  // which is basically not OK... TODO - fix this!!!
  private Command getCommand(String cmd)  {
    try {
      return (Command) commands.get(cmd).clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
    return null;
  }
    
  private int checkCommands(String[][] cmds) {
    for (int i = 0; i < cmds.length; i++) {
      Command command = commands.get(cmds[i][0]);
      if (command == null) {
        System.out.println("CLI: Command not found: " + cmds[i]);
        return -1;
      }
      if (i > 0 && !(command instanceof LineListener)) {
        System.out.println("CLI: Error " + cmds[i][0] + " does not take input");
        return -1;
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
}
