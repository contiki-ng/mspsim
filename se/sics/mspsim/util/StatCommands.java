/**
 * Copyright (c) 2008, Swedish Institute of Computer Science.
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
 * StatCommands
 *
 * Author  : Joakim Eriksson, Niclas Finne
 * Created : 11 March 2008
 * Updated : $Date$
 *           $Rev$
 */
package se.sics.mspsim.util;
import java.io.PrintStream;

import se.sics.mspsim.cli.BasicAsyncCommand;
import se.sics.mspsim.cli.BasicCommand;
import se.sics.mspsim.cli.CommandBundle;
import se.sics.mspsim.cli.CommandContext;
import se.sics.mspsim.cli.CommandHandler;
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.TimeEvent;

public class StatCommands implements CommandBundle {

  private final MSP430Core cpu;
  private final OperatingModeStatistics statistics;

  public StatCommands(MSP430Core cpu, OperatingModeStatistics statistics) {
    this.cpu = cpu;
    this.statistics = statistics;
  }

  @Override
  public void setupCommands(ComponentRegistry registry, CommandHandler handler) {
    handler.registerCommand("chipinfo", new BasicCommand("show information about specified chip",
    "[chips...]") {

      @Override
      public int executeCommand(CommandContext context) {
        if (context.getArgumentCount() > 0) {
          for (int i = 0, n = context.getArgumentCount(); i < n; i++) {
            String chipName = context.getArgument(i);
            Chip chip = statistics.getChip(chipName);
            if (chip == null) {
              context.out.println("  " + chipName + ": NOT FOUND");
            } else {
              context.out.println("  " + chipName + ": " + chip);
            }
          }
        } else {
          Chip[] chips = statistics.getChips();
          if (chips == null) {
            context.out.println("No chips found.");
          } else {
            for (int i = 0, n = chips.length; i < n; i++) {
              context.out.println("  " + chips[i].getName());
            }
          }
        }
        return 0;
      }
        
    });

    handler.registerCommand("duty", new BasicAsyncCommand("add a duty cycle sampler for operating modes to the specified chips",
        "<frequency> <chip> [chips...]") {

      private PrintStream out;
      private MultiDataSource[] sources;
      private double frequency;

      public int executeCommand(CommandContext context) {
        frequency = context.getArgumentAsDouble(0);
        if (frequency <= 0.0) {
          context.err.println("illegal frequency: " + context.getArgument(0));
          return 1;
        }
        sources = new MultiDataSource[context.getArgumentCount() - 1];
        for (int i = 0, n = sources.length; i < n; i++) {
          sources[i] = statistics.getMultiDataSource(context.getArgument(i + 1)); 
          if (sources[i] == null) {
            context.err.println("could not find chip " + context.getArgument(i + 1));
            return 1;            
          }
        }
        this.out = context.out;

        cpu.scheduleTimeEventMillis(new TimeEvent(0) {

          @Override
          public void execute(long t) {
            cpu.scheduleTimeEventMillis(this, 1000.0 / frequency);
            for (int j = 0, n = sources.length; j < n; j++) {
              MultiDataSource ds = sources[j];
              if (j > 0) out.print(' ');
              for (int k = 0, m = ds.getModeMax(); k <= m; k++) {
                if (k > 0) out.print(' ');
                out.print(ds.getValue(k));                 
              }
            }
            out.println();
          }
        }, 1000.0 / frequency);
        return 0;
      }

      public void stopCommand(CommandContext context) {
        context.exit(0);
      }
    });
  }

}
