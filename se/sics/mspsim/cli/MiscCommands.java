/**
 * Copyright (c) 2007, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of MSPSim.
 *
 * $Id$
 *
 * -----------------------------------------------------------------
 *
 * MiscCommands
 *
 * Author  : Joakim Eriksson
 * Created : 9 mar 2008
 * Updated : $Date$
 *           $Revision$
 */
package se.sics.mspsim.cli;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.TimeEvent;
import se.sics.mspsim.util.ComponentRegistry;

/**
 * @author joakim
 *
 */
public class MiscCommands implements CommandBundle {

  private Hashtable <String, FileTarget> fileTargets = new Hashtable<String, FileTarget>();

  public void setupCommands(final ComponentRegistry registry, CommandHandler handler) {
    handler.registerCommand("grep", new BasicLineCommand("print lines matching the specified pattern", "[-i] [-v] <regexp>") {
      private PrintStream out;
      private Pattern pattern;
      private boolean isInverted = false;

      public int executeCommand(CommandContext context) {
        int index = 0;
        int flags = 0;
        while (index + 1 < context.getArgumentCount()) {
          if ("-i".equals(context.getArgument(index))) {
            flags |= Pattern.CASE_INSENSITIVE;
          } else if ("-v".equals(context.getArgument(index))) {
            isInverted = true;
          } else {
            context.err.println("unknown option: " + context.getArgument(index));
            return 1;
          }
          index++;
        }
        out = context.out;
        pattern = Pattern.compile(context.getArgument(index), flags);
        return 0;
      }
      public void lineRead(String line) {
        boolean isMatch = pattern.matcher(line).find();
        if(isMatch ^ isInverted) {
          out.println(line);
        }
      }
      public void stopCommand(CommandContext context) {
        context.exit(0);
      }
    });

    // TODO: this should also be "registered" as a "sink".
    // probably this should be handled using ">" instead!
    handler.registerCommand(">", new BasicLineCommand(null, "<filename>") {
      FileTarget ft;
      public int executeCommand(CommandContext context) {
        String fileName = context.getArgument(0);
        ft = fileTargets.get(fileName);
        if (ft == null) {
          try {
//            System.out.println("Creating new file target: " + fileName);
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
      }
      public void stopCommand(CommandContext context) {
        // Should this do anything?
        // Probably depending on the ft's config
      }
    });

    handler.registerCommand("fclose", new BasicCommand("close the specified file", "<filename>") {
      public int executeCommand(CommandContext context) {
        String name = context.getArgument(0);
        FileTarget ft = fileTargets.get(name);
        if (ft != null) {
          context.out.println("Closing file " + name);
          fileTargets.remove(name);
          ft.close();
          return 0;
        } else {
          context.err.println("Could not find the open file " + name);
          return 1;
        }
      }
    });

    handler.registerCommand("files", new BasicCommand("list open files", "") {
      public int executeCommand(CommandContext context) {
        for (Iterator<FileTarget> iterator = fileTargets.values().iterator(); iterator.hasNext();) {
          FileTarget type = iterator.next();
          context.out.println(type.getName());
        }
        return 0;
      }
    });

    handler.registerCommand("speed", new BasicCommand("set the speed factor for the CPU", "[factor]") {
      public int executeCommand(CommandContext context) {
        MSP430 cpu = (MSP430) registry.getComponent(MSP430.class);
        if (cpu == null) {
          context.err.println("could not access the CPU.");
          return 1;
        } else if (context.getArgumentCount() == 0) {
          long rate = cpu.getSleepRate();
          double d = rate / 25000.0;
          context.out.println("Speed factor is set to " + (((int)(d * 100 + 0.5)) / 100.0));
        } else {
          double d = context.getArgumentAsDouble(0);
          if (d > 0.0) {
            long rate = (long) (25000 * d);
            cpu.setSleepRate(rate);
          } else {
            context.err.println("Speed factor must be larger than zero.");
            return 1;
          }
        }
        return 0;
      }
    });

    handler.registerCommand("quit", new BasicCommand("exit MSPSim", "") {
      public int executeCommand(CommandContext context) {
        System.exit(0);
        return 0;
      }
    });

    handler.registerCommand("echo", new BasicCommand("echo arguments", "") {
      public int executeCommand(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, n = context.getArgumentCount(); i < n; i++) {
          if (i > 0) sb.append(' ');
          sb.append(context.getArgument(i));
        }
        context.out.println(sb.toString());
        return 0;
      }
    });

    handler.registerCommand("source", new BasicCommand("run script", "<filename>") {
      public int executeCommand(CommandContext context) {
        FileInputStream infs = null;
        try {
          infs = new FileInputStream(context.getArgument(0));
        } catch (FileNotFoundException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(infs));
        String line = null;
        try {
          while ((line = input.readLine()) != null) {
            context.executeCommand(line);
          }
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        return 0;
      }
    });

    
    handler.registerCommand("repeat", new BasicAsyncCommand("repeat the specified command line", "[-t delay] [-c count] <command line>") {

      private MSP430 cpu;
      private int period = 1;
      private int count = 0;
      private int maxCount = -1;
      private String commandLine;
      private boolean isRunning = true;

      public int executeCommand(final CommandContext context) {
        int index = 0;
        do {
          String a = context.getArgument(index);
          if (a.startsWith("-")) {
            if (a.equals("-t")) {
              period = context.getArgumentAsInt(index + 1);
              index += 2;
            } else if (a.equals("-c")) {
              maxCount = context.getArgumentAsInt(index + 1);
              index += 2;
            } else {
              context.err.println("illegal option: " + a);
              return 1;
            }
          } else {
            break;
          }
        } while (true);
        if (index + 1 < context.getArgumentCount()) {
          context.err.println("too many arguments");
          return 1;
        }
        commandLine = context.getArgument(index);

        cpu = (MSP430) registry.getComponent(MSP430.class);
        if (cpu == null) {
          context.err.println("could not access the CPU.");
          return 1;
        }

        cpu.scheduleTimeEventMillis(new TimeEvent(0) {

          @Override
          public void execute(long t) {
            if (isRunning) {
              count++;
              context.executeCommand(commandLine);
              if ((maxCount <= 0) || (count < maxCount)) {
                cpu.scheduleTimeEventMillis(this, period * 1000d);
              } else {
                stopCommand(context);
              }
            }
          }

        }, period * 1000d);
        return 0;
      }

      public void stopCommand(CommandContext context) {
        isRunning = false;
        context.err.println("[repeat exit: " + commandLine + ']');
        context.exit(0);
      }
    });

    handler.registerCommand("exec", new ExecCommand());
  }

}
