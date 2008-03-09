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
 * CommandBundle
 *
 * Author  : Joakim Eriksson, Niclas Finne
 * Created : Mon Feb 11 2008
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */
package se.sics.mspsim.cli;
import se.sics.mspsim.core.CPUMonitor;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.DebugInfo;
import se.sics.mspsim.util.ELF;
import se.sics.mspsim.util.MapEntry;
import se.sics.mspsim.util.Utils;

public class DebugCommands implements CommandBundle {

  public void setupCommands(ComponentRegistry registry, CommandHandler ch) {
    final MSP430 cpu = (MSP430) registry.getComponent(MSP430.class);
    final ELF elf = (ELF) registry.getComponent(ELF.class);
    final GenericNode node = (GenericNode) registry.getComponent("node");
    if (cpu != null) {
      ch.registerCommand("break", new BasicAsyncCommand("adds a breakpoint to a given address or symbol",
          "<address or symbol>") { 
        int address = 0; 
        public int executeCommand(final CommandContext context) {
          int baddr = context.getArgumentAsAddress(0);
          cpu.setBreakPoint(address = baddr,
              new CPUMonitor() {
                public void cpuAction(int type, int adr, int data) {
                  context.out.println("*** Break at " + adr);
                }
          });
          context.out.println("Breakpoint set at: " + baddr);
          return 0;
        }
        public void stopCommand(CommandContext context) {
          cpu.clearBreakPoint(address);
        }
      });

      ch.registerCommand("watch",
          new BasicAsyncCommand("adds a write watch to a given address or symbol", "<address or symbol>") {
        int mode = 0;
        int address = 0;
        public int executeCommand(final CommandContext context) {
          int baddr = context.getArgumentAsAddress(0);
          if (baddr == -1) {
            context.out.println("Error: unkown symbol:" + context.getArgument(0));            
            return -1;
          }
          if (context.getArgumentCount() > 1) {
            String modeStr = context.getArgument(1);
            if ("char".equals(modeStr)) {
              mode = 1;
            }
          }
          cpu.setBreakPoint(address = baddr,
              new CPUMonitor() {
            public void cpuAction(int type, int adr, int data) {
              if (mode == 0) {
                int pc = cpu.readRegister(0);
                String adrStr = getSymOrAddr(context, adr);
                String pcStr = getSymOrAddrELF(elf, pc);
                context.out.println("*** Write from " + pcStr +
                    ": " + adrStr + " = " + data);
              } else {
                context.out.print((char) data);
              }
            }
          });
          context.out.println("Watch set at: " + baddr);
          return 0;
        }
        
        public void stopCommand(CommandContext context) {
          cpu.clearBreakPoint(address);
        }
      });

      ch.registerCommand("clear", new Command() {
        public int executeCommand(final CommandContext context) {
          int baddr = context.getArgumentAsAddress(0);
          cpu.setBreakPoint(baddr, null);
          return 0;
        }

        public String getArgumentHelp(CommandContext context) {
          return "<address or symbol>";
        }

        public String getCommandHelp(CommandContext context) {
          return "clears a breakpoint or watch from a given address or symbol";
        }
      });

      
      
      ch.registerCommand("symbol", new BasicCommand("lists matching symbold", "<regexp>") {
        public int executeCommand(final CommandContext context) {
          String regExp = context.getArgument(0);
          MapEntry[] entries = context.getMapTable().getEntries(regExp);
          for (int i = 0; i < entries.length; i++) {
            MapEntry mapEntry = entries[i];
            context.out.println(" " + mapEntry.getName() + " at " +
                Utils.hex16(mapEntry.getAddress()));
          }
          return 0;
        }
      });
      
      if (node != null) {
        ch.registerCommand("stop", new BasicCommand("stops mspsim", "") {
          public int executeCommand(CommandContext context) {
            node.stop();
            context.out.println("CPU stopped at: " + Utils.hex16(cpu.readRegister(0)));
            return 0;
          }
        });
        ch.registerCommand("start", new BasicCommand("starts mspsim", "") {
          public int executeCommand(CommandContext context) {
            node.start();
            return 0;
          }
        });
        ch.registerCommand("step", new BasicCommand("singlesteps mspsim", "<number of lines>") {
          public int executeCommand(CommandContext context) {
            int nr = context.getArgumentCount() > 0 ? context.getArgumentAsInt(0) : 1;
            while(nr-- > 0)
              node.step();
            context.out.println("CPU stepped to: " + Utils.hex16(cpu.readRegister(0)));
            return 0;
          }
        });
        ch.registerCommand("print", new BasicCommand("prints value of an address or symbol", "<address or symbol>") {
          public int executeCommand(CommandContext context) {
            int adr = context.getArgumentAsAddress(0);
            if (adr != -1) { 
              context.out.println("" + context.getArgument(0) + " = " + Utils.hex16(cpu.read(adr, adr >= 0x100)));
            } else {
              context.out.println("unkown symbol");  
            }
            return 0;
          }
        });
        ch.registerCommand("printreg", new BasicCommand("prints value of an register", "<register>") {
          public int executeCommand(CommandContext context) {
            int adr = context.getArgumentAsInt(0);
              context.out.println("" + context.getArgument(0) + " = " + Utils.hex16(cpu.readRegister(adr)));
            return 0;
          }
        });
        ch.registerCommand("time", new BasicCommand("prints the elapse time and cycles", "") {
          public int executeCommand(CommandContext context) {
            long time = ((long)(cpu.getTimeMillis()));
              context.out.println("Emulated time elapsed:" + time + "(ms)  cycles: " + cpu.cycles);
            return 0;
          }
        });

      }
    }
  }

  private static String getSymOrAddr(CommandContext context, int adr) {
    MapEntry me = context.getMapTable().getEntry(adr);
    if (me != null) {
      return me.getName();
    } else {
      return Utils.hex16(adr);
    }
  }

  private static String getSymOrAddrELF(ELF elf, int adr) {
    DebugInfo me = elf.getDebugInfo(adr);
    if (me != null) {
      return me.toString();
    } else {
      return Utils.hex16(adr);
    }
  }

}
