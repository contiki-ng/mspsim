/*
 * Copyright (c) 2008, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
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
 * $Id$
 *
 * -----------------------------------------------------------------
 *
 * ProfilerCommands
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : 12 maj 2008
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.cli;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.Profiler;
import se.sics.mspsim.util.ComponentRegistry;

/**
 *
 */
public class ProfilerCommands implements CommandBundle {

  public void setupCommands(ComponentRegistry registry, CommandHandler ch) {
    final MSP430 cpu = (MSP430) registry.getComponent(MSP430.class);
    if (cpu != null) {
      ch.registerCommand("profile", new BasicCommand("show profile information",
          "[-clear] [regexp]") {

        @Override
        public int executeCommand(final CommandContext context) {
          Profiler profiler = cpu.getProfiler();
          if (profiler == null) {
            context.err.println("No profiler found.");
            return 1;
          }
          if (context.getArgumentCount() > 1) {
            context.err.println("Too many arguments. Either clear or show profile information.");
            return 1;
          }
          String namematch = null;
          if (context.getArgumentCount() > 0) {
            namematch = context.getArgument(0);
            if ("-clear".equals(namematch)) {
              profiler.clearProfile();
              context.out.println("Cleared profile information.");
              return 0;
            }
          }
          profiler.printProfile(context.out, namematch);
          return 0;
        }

      });
      ch.registerCommand("stacktrace", new BasicCommand("show stack trace", "") {

        @Override
        public int executeCommand(CommandContext context) {
          Profiler profiler = cpu.getProfiler();
          if (profiler == null) {
            context.err.println("No profiler found.");
            return 1;
          }
          profiler.printStackTrace(context.out);
          return 0;
        }

      });

      ch.registerCommand("printcalls", new BasicAsyncCommand("print function calls", "") {
        @Override
        public int executeCommand(CommandContext context) {
          Profiler profiler = cpu.getProfiler();
          if (profiler == null) {
            context.err.println("No profiler found.");
            return 1;
          }
          profiler.setLogger(context.out);
          return 0;
        }
        public void stopCommand(CommandContext context) {
          Profiler profiler = cpu.getProfiler();
          if (profiler != null) {
            profiler.setLogger(null);
          }
        }
      });
    }
  }

}
