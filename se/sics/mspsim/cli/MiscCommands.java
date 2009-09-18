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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;

import se.sics.mspsim.chip.RFListener;
import se.sics.mspsim.chip.RFSource;
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.MSP430Constants;
import se.sics.mspsim.core.TimeEvent;
import se.sics.mspsim.util.ActiveComponent;
import se.sics.mspsim.util.ArgumentManager;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.PluginRepository;
import se.sics.mspsim.util.ServiceComponent;
import se.sics.mspsim.util.Utils;

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
    });

    handler.registerCommand("timestamp", new BasicLineCommand("print lines with timestamp prefixed", "") {
      private PrintStream out;
      private MSP430 cpu;
      long startTime;

      public int executeCommand(CommandContext context) {
        cpu = (MSP430) registry.getComponent(MSP430.class);
        if (cpu == null) {
          context.err.println("could not access the CPU.");
          return 1;
        }
        out = context.out;
        startTime = System.currentTimeMillis() - (long)cpu.getTimeMillis();
        return 0;
      }
      public void lineRead(String line) {
        out.println(Long.toString(startTime + (long)cpu.getTimeMillis()) + ' ' + line);
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
    
    

    handler.registerCommand("source", new BasicCommand("run script", "[-v] <filename>") {
      public int executeCommand(CommandContext context) {
          boolean verbose = false;
          if (context.getArgumentCount() > 1) {
              verbose = "-v".equals(context.getArgument(0));
          }
        File fp = new File(context.getArgument(context.getArgumentCount() - 1));
        if (!fp.canRead()) {
          context.err.println("could not find the script file '" + context.getArgument(0) + "'.");
          return 1;
        }
        try {
          FileInputStream infs = new FileInputStream(fp);
          BufferedReader input = new BufferedReader(new InputStreamReader(infs));
          try {
            String line;
            while ((line = input.readLine()) != null) {
              if (verbose) context.out.println(line);
              context.executeCommand(line);
            }
          } finally {
            input.close();
          }
        } catch (IOException e) {
          e.printStackTrace(context.err);
          return 1;
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

    handler.registerCommand("trig", new BasicLineCommand("trigg command when getting input", "<command>") {
      String command = null;
      CommandContext context;
      public int executeCommand(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, n = context.getArgumentCount(); i < n; i++) {
          if (i > 0) sb.append(' ');
          sb.append(context.getArgument(i));
        }
        command = sb.toString();
        this.context = context;
        return 0;
      }
      public void lineRead(String line) {
        context.executeCommand(command);
      }
    });

    handler.registerCommand("install", new BasicCommand("install and start a plugin", "ClassName [Name]") {
      @Override
      public int executeCommand(CommandContext context) {
        String className = context.getArgument(0);
        String name = className;
        if (context.getArgumentCount() > 1) {
          name = context.getArgument(1);
        }
        if (registry.getComponent(name) != null) {
          context.err.println("Another component with name " + name + " is already installed");
          return 1;
        }
        Class pluginClass = null;
        PluginRepository plugins = (PluginRepository) registry.getComponent("pluginRepository");
        try {
          try {
            pluginClass = plugins != null ? plugins.loadClass(className) :
              Class.forName(className);
          } catch (ClassNotFoundException e) {
            String newClassName = "se.sics.mspsim.plugin." + className;
            pluginClass = plugins != null ? plugins.loadClass(newClassName) :
              Class.forName(newClassName);
          }
          Object component = pluginClass.newInstance();
          registry.registerComponent(name, component);
          return 0;
        } catch (Exception e1) {
          e1.printStackTrace(context.err);
        }
        // TODO Auto-generated method stub
        return 1;
      }
    });
    
    handler.registerCommand("service", new BasicCommand("handle service plugins", "[class name|service name] [start|stop]") {
      @Override
      public int executeCommand(CommandContext context) {
        if (context.getArgumentCount() == 0) {
          ServiceComponent[] sc = (ServiceComponent[]) registry.getAllComponents(ServiceComponent.class);
          for (int i = 0; i < sc.length; i++) {
            context.out.printf(" %-20s %s\n",sc[i].getName(),sc[i].getStatus());
          }
        } else if (context.getArgumentCount() == 1){
          String name = context.getArgument(0);
          ServiceComponent sc = getServiceForName(registry, name);
          if (sc != null) {
            context.out.printf(" %-20s %s\n",sc.getName(),sc.getStatus());
          } else {
            context.out.println("can not find service" + name);
          }
        } else {
          String name = context.getArgument(0);
          String operation = context.getArgument(1);
          if ("start".equals(operation)) {
            ServiceComponent sc = getServiceForName(registry, name);
            if (sc != null) {
              sc.start();
              context.out.println("service " + sc.getName() + " started");
            } else {
              context.out.println("can not find service" + name);
            }
          } else if ("stop".equals(operation)) {
            ServiceComponent sc = getServiceForName(registry, name);
            if (sc != null) {
              sc.stop();
              context.out.println("service " + sc.getName() + " stopped");
            } else {
              context.out.println("can not find service" + name);
            }
          }
        }
        return 0;
      }
    });

    handler.registerCommand("rflistener", new BasicLineCommand("an rflisteer", "[input|output] <rf-chip>") {
      CommandContext context;
      RFListener listener;
      final MSP430 cpu = (MSP430) registry.getComponent(MSP430.class);
      public int executeCommand(CommandContext ctx) {
        this.context = ctx;
        String inout = context.getArgument(0);
        Chip chip = cpu.getChip(context.getArgument(1));
        if ("output".equals(inout)) {
          if (chip instanceof RFSource) {
             ((RFSource)chip).setRFListener(new RFListener(){
              public void receivedByte(byte data) {
                context.out.println("" + Utils.hex8(data));
              }
             });
          }
        } else if ("input".equals(inout)){
          listener = (RFListener) chip;
        } else {
          context.err.println("Error: illegal type: " + inout);
        }
        return 0;
      }
      public void lineRead(String line) {
        if (listener != null) {
          byte[] data = Utils.hexconv(line);
          context.out.println("Should send bytes to radio: " + line);
          for (int i = 0; i < data.length; i++) {
            //context.out.println("Byte " + i + " = " + ((int) data[i] & 0xff));
            listener.receivedByte(data[i]);
          }
        }
      }
    });
    
    handler.registerCommand("sysinfo", new BasicCommand("show info about the MSPSim system", "[-registry]") {
        public int executeCommand(CommandContext context) {
            ArgumentManager config = (ArgumentManager) registry.getComponent("config");
            context.out.println("--------- System info ----------\n");
            context.out.println("MSPSim version: " + MSP430Constants.VERSION);
            context.out.println("Java version  : " + System.getProperty("java.version") + " " +
                    System.getProperty("java.vendor"));
            context.out.println("Firmware      : " + config.getProperty("firmwareFile"));
            context.out.println("AutoloadScript: " + config.getProperty("autoloadScript"));
            context.out.println();
            if (context.getOption("registry")) {
                context.out.println("--------- Registry info --------\n");
                registry.printRegistry(context.out);
            }
            return 0;
        }
    });

    handler.registerCommand("quit", new BasicCommand("exit MSPSim", "") {
        public int executeCommand(CommandContext context) {
          /* TODO: flush all files, etc.... */
          System.exit(0);
          return 0;
        }
      });

    handler.registerCommand("exit", new BasicCommand("exit MSPSim", "") {
        public int executeCommand(CommandContext context) {
            System.exit(0);
            return 0;
        }
    });

  }

  private static ServiceComponent getServiceForName(ComponentRegistry registry, String name) {
    Object o = registry.getComponent(name);
    if (o instanceof ServiceComponent) {
      return (ServiceComponent) o;
    }
    return null;
  }
}
