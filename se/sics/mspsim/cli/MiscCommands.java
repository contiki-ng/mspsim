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
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.util.ComponentRegistry;

/**
 * @author joakim
 *
 */
public class MiscCommands implements CommandBundle {

  private Hashtable <String, FileTarget> fileTargets = new Hashtable<String, FileTarget>();

  public void setupCommands(final ComponentRegistry registry, CommandHandler handler) {
    handler.registerCommand("grep", new BasicLineCommand("print lines matching the specified pattern", "<regexp>") {
      private PrintStream out;
      private Pattern pattern;
      public int executeCommand(CommandContext context) {
        out = context.out;
        pattern = Pattern.compile(context.getArgument(0));
        return 0;
      }
      public void lineRead(String line) {
        if (pattern.matcher(line).find())
          out.println(line);
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
          FileTarget type = (FileTarget) iterator.next();
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

    handler.registerCommand("exec", new ExecCommand());
  }

}
