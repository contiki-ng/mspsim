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
 * Updated : $Date$
 *           $Revision$
 */
package se.sics.mspsim.cli;
import se.sics.mspsim.core.CPUMonitor;
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.EmulationException;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.MSP430Constants;
import se.sics.mspsim.core.Memory;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.DebugInfo;
import se.sics.mspsim.util.ELF;
import se.sics.mspsim.util.GDBStubs;
import se.sics.mspsim.util.MapEntry;
import se.sics.mspsim.util.Utils;

public class DebugCommands implements CommandBundle {
  private long lastCall = 0;
  private long lastWall = 0;
  private ComponentRegistry registry;

  private ELF getELF() {
    return (ELF) registry.getComponent(ELF.class);
  }
  
  public void setupCommands(ComponentRegistry registry, CommandHandler ch) {
    this.registry = registry;
    final MSP430 cpu = (MSP430) registry.getComponent(MSP430.class);
    final GenericNode node = (GenericNode) registry.getComponent("node");
    if (cpu != null) {
      ch.registerCommand("break", new BasicAsyncCommand("add a breakpoint to a given address or symbol",
          "<address or symbol>") { 
        int address = 0; 
        public int executeCommand(final CommandContext context) {
          int baddr = context.getArgumentAsAddress(0);
          if (baddr < 0) {
            context.err.println("unknown symbol: " + context.getArgument(0));
            return 1;
          }
          cpu.setBreakPoint(address = baddr,
              new CPUMonitor() {
                public void cpuAction(int type, int adr, int data) {
                  context.out.println("*** Break at $" + Utils.hex16(adr));
                }
          });
          context.out.println("Breakpoint set at $" + Utils.hex16(baddr));
          return 0;
        }
        public void stopCommand(CommandContext context) {
          cpu.clearBreakPoint(address);
        }
      });

      ch.registerCommand("watch",
          new BasicAsyncCommand("add a write/read watch to a given address or symbol", "<address or symbol> [char | break]") {
        int mode = 0;
        int address = 0;
        public int executeCommand(final CommandContext context) {
          int baddr = context.getArgumentAsAddress(0);
          if (baddr == -1) {
            context.err.println("unknown symbol: " + context.getArgument(0));            
            return -1;
          }
          if (context.getArgumentCount() > 1) {
            String modeStr = context.getArgument(1);
            if ("char".equals(modeStr)) {
              mode = 1;
            } else if ("break".equals(modeStr)) {
              mode = 2;
            }
          }
          cpu.setBreakPoint(address = baddr,
              new CPUMonitor() {
            public void cpuAction(int type, int adr, int data) {
              if (mode == 0 || mode == 2) {
                int pc = cpu.readRegister(0);
                String adrStr = getSymOrAddr(context, adr);
                String pcStr = getSymOrAddrELF(getELF(), pc);
                String op = "op";
                if (type == MEMORY_READ) {
                  op = "Read";
                } else if (type == MEMORY_WRITE){
                  op = "Write";
                }
                context.out.println("*** " + op + " from " + pcStr +
                    ": " + adrStr + " = " + data);
                if (mode == 2) {
                  cpu.stop();
                }
              } else {
                context.out.print((char) data);
              }
            }
          });
          context.out.println("Watch set at $" + Utils.hex16(baddr));
          return 0;
        }

        public void stopCommand(CommandContext context) {
          cpu.clearBreakPoint(address);
          context.exit(0);
        }
      });

      ch.registerCommand("watchreg",
          new BasicAsyncCommand("add a write watch to a given register", "<register> [int]") {
        int mode = 0;
        int register = 0;
        public int executeCommand(final CommandContext context) {
          register = context.getArgumentAsRegister(0);
          if (register < 0) {
            return -1;
          }
          if (context.getArgumentCount() > 1) {
            String modeStr = context.getArgument(1);
            if ("int".equals(modeStr)) {
              mode = 1;
            } else {
              context.err.println("illegal argument: " + modeStr);
              return -1;
            }
          }
          cpu.setRegisterWriteMonitor(register, new CPUMonitor() {
            public void cpuAction(int type, int adr, int data) {
              if (mode == 0) {
                int pc = cpu.readRegister(0);
                String adrStr = getRegisterName(register);
                String pcStr = getSymOrAddrELF(getELF(), pc);
                context.out.println("*** Write from " + pcStr +
                    ": " + adrStr + " = " + data);
              } else {
                context.out.println(data);
              }
            }
          });
          context.out.println("Watch set for register " + getRegisterName(register));
          return 0;
        }
        
        public void stopCommand(CommandContext context) {
          cpu.clearBreakPoint(register);
        }
      });

      ch.registerCommand("clear", new Command() {
        public int executeCommand(final CommandContext context) {
          int baddr = context.getArgumentAsAddress(0);
          cpu.setBreakPoint(baddr, null);
          return 0;
        }

        public String getArgumentHelp(String commandName) {
          return "<address or symbol>";
        }

        public String getCommandHelp(String commandName) {
          return "clear a breakpoint or watch from a given address or symbol";
        }
      });

      ch.registerCommand("symbol", new BasicCommand("list matching symbols", "<regexp>") {
        public int executeCommand(final CommandContext context) {
          String regExp = context.getArgument(0);
          MapEntry[] entries = context.getMapTable().getEntries(regExp);
          for (int i = 0; i < entries.length; i++) {
            MapEntry mapEntry = entries[i];
            int address = mapEntry.getAddress();
            context.out.println(" " + mapEntry.getName() + " at $" +
                Utils.hex16(address) + " (" + Utils.hex8(cpu.memory[address]) +
                  " " + Utils.hex8(cpu.memory[address + 1]) + ")");
          }
          return 0;
        }
      });
      
      ch.registerCommand("line", new BasicCommand("print line number of address/symbol", "<addres or symbol>") {
        public int executeCommand(final CommandContext context) {
          int adr = context.getArgumentAsAddress(0);
          DebugInfo di = getELF().getDebugInfo(adr);
          if (di != null) {
            di.getLine();
            context.out.println(di);
          } else {
            context.err.println("No line number found for: " + context.getArgument(0));
          }
          return 0;
        }
      });      
      
      if (node != null) {
        ch.registerCommand("stop", new BasicCommand("stop the CPU", "") {
          public int executeCommand(CommandContext context) {
            node.stop();
            context.out.println("CPU stopped at: $" + Utils.hex16(cpu.readRegister(0)));
            return 0;
          }
        });
        ch.registerCommand("start", new BasicCommand("start the CPU", "") {
          public int executeCommand(CommandContext context) {
            node.start();
            return 0;
          }
        });
        ch.registerCommand("step", new BasicCommand("singlestep the CPU", "[number of instructions]") {
          public int executeCommand(CommandContext context) {
            int nr = context.getArgumentCount() > 0 ? context.getArgumentAsInt(0) : 1;
            long cyc = cpu.cycles;
            try {
              node.step(nr); 
            } catch (Exception e) {
              e.printStackTrace(context.out);
            }
            context.out.println("CPU stepped to: $" + Utils.hex16(cpu.readRegister(0)) +
                " in " + (cpu.cycles - cyc) + " cycles (" + cpu.cycles + ")");
            return 0;
          }
        });
        ch.registerCommand("stack", new BasicCommand("show stack info", "") {
          public int executeCommand(CommandContext context) {
            int stackEnd = context.getMapTable().heapStartAddress;
            int stackStart = context.getMapTable().stackStartAddress;
            int current = cpu.readRegister(MSP430Constants.SP);
            context.out.println("Current stack: $" + Utils.hex16(current) + " (" + (stackStart - current) + " used of " + (stackStart - stackEnd) + ')');
            return 0;
          }
        });
        ch.registerCommand("print", new BasicCommand("print value of an address or symbol", "<address or symbol>") {
          public int executeCommand(CommandContext context) {
            int adr = context.getArgumentAsAddress(0);
            if (adr != -1) {
              try {
                context.out.println("" + context.getArgument(0) + " = " + Utils.hex16(cpu.read(adr, adr >= 0x100)));
              } catch (Exception e) {
                e.printStackTrace(context.out);
              }
              return 0;
            } else {
              context.err.println("unknown symbol: " + context.getArgument(0));
              return 1;
            }
          }
        });
        ch.registerCommand("printreg", new BasicCommand("print value of an register", "<register>") {
          public int executeCommand(CommandContext context) {
            int register = context.getArgumentAsRegister(0);
            if (register >= 0) {
              context.out.println(context.getArgument(0) + " = $" + Utils.hex16(cpu.readRegister(register)));
              return 0;
            }
            return -1;
          }
        });
        ch.registerCommand("reset", new BasicCommand("reset the CPU", "") {
          public int executeCommand(CommandContext context) {
            cpu.reset();
            return 0;
          }
        });

        ch.registerCommand("time", new BasicCommand("print the elapse time and cycles", "") {
          public int executeCommand(CommandContext context) {
            long time = ((long)(cpu.getTimeMillis()));
            context.out.println("Emulated time elapsed: " + time + "(ms)  since last: " + (time - lastCall) + " ms" + " wallTime: " +
                (System.currentTimeMillis() - lastWall) + " ms");
            lastCall = time;
            lastWall = System.currentTimeMillis();
            return 0;
          }
        });
        
        ch.registerCommand("mem", new BasicCommand("dump memory", "<start address> <num_emtries> [type]") {
          public int executeCommand(final CommandContext context) {
            int start = context.getArgumentAsAddress(0);
            int count = context.getArgumentAsInt(1);
            int size = 1; // unsigned byte
            boolean signed = false;
            if (context.getArgumentCount() > 2) {
              String tS = context.getArgument(2);
              if ("byte".equals(tS)) {
                signed = true;
              } else if ("word".equals(tS)) {
                signed = true;
                size = 2;
              } else if ("uword".equals(tS)) {
                size = 2;
              }
            }
            // Does not yet handle signed data...
            for (int i = 0; i < count; i++) {
              int data = 0;
              data = cpu.memory[start++];
              if (size == 2) {
                data = data  + (cpu.memory[start++] << 8);
              }
              context.out.print(" " + data);
            }
            context.out.println();
            return 0;
          }
        });

        ch.registerCommand("set", new BasicCommand("set memory", "<address> <value> [type]") {
          public int executeCommand(final CommandContext context) {
            int adr = context.getArgumentAsAddress(0);
            int val = context.getArgumentAsInt(1);
            boolean word = val > 0xff;
            try {
              cpu.write(adr, val, word);
            } catch (EmulationException e) {
              e.printStackTrace(context.out);
            }
            return 0;
          }});
        
        /******************************************************
         * handle external memory (flash, etc). 
         ******************************************************/
        ch.registerCommand("xmem", new BasicCommand("dump flash memory", "<start address> <num_emtries> [type]") {
          public int executeCommand(final CommandContext context) {
            Memory xmem = (Memory) DebugCommands.this.registry.getComponent("xmem");
            if (xmem == null) {
              context.err.println("No xmem component registered");
              return 0;
            }
            int start = context.getArgumentAsAddress(0);
            int count = context.getArgumentAsInt(1);
            int size = 1; // unsigned byte
            boolean signed = false;
            if (context.getArgumentCount() > 2) {
              String tS = context.getArgument(2);
              if ("byte".equals(tS)) {
                signed = true;
              } else if ("word".equals(tS)) {
                signed = true;
                size = 2;
              } else if ("uword".equals(tS)) {
                size = 2;
              }
            }
            // Does not yet handle signed data...
            for (int i = 0; i < count; i++) {
              int data = 0;
              data = xmem.readByte(start++);
              if (size == 2) {
                data = data  + (xmem.readByte(start++) << 8);
              }
              context.out.print(" " + data);
            }
            context.out.println();
            return 0;
          }
        });
        
        ch.registerCommand("xset", new BasicCommand("set memory", "<address> <value> [type]") {
          public int executeCommand(final CommandContext context) {
            Memory xmem = (Memory) DebugCommands.this.registry.getComponent("xmem");
            if (xmem == null) {
              context.err.println("No xmem component registered");
              return 0;
            }
            int adr = context.getArgumentAsAddress(0);
            int val = context.getArgumentAsInt(1);
            boolean word = val > 0xff;
            if (word) {
              xmem.writeByte(adr, val >> 8);
              val = val & 0xff;
              adr++;
            }
            xmem.writeByte(adr, val & 0xff);
            return 0;
          }});

        ch.registerCommand("gdbstubs", new BasicCommand("open up a gdb stubs server for GDB remote debugging", "port") {
          private GDBStubs stubs = null;
          public int executeCommand(CommandContext context) {
            if (stubs != null) {
              context.err.println("GDBStubs alread openend");
            } else {
              int port = context.getArgumentAsInt(0);
              stubs = new GDBStubs();
              stubs.setupServer(cpu, port);
            }
            return 0;
          }
        });
        
        ch.registerCommand("loggable", new BasicCommand("list loggable objects", "") {
          @Override
          public int executeCommand(CommandContext context) {
            Chip[] chips = cpu.getChips();
            for (int i = 0; i < chips.length; i++) {
              context.out.println(chips[i].getName());
            }
            return 0;
          }
        });
        
        ch.registerCommand("log", new BasicAsyncCommand("log a loggable object", "<loggable>" ) {
          Chip chip = null;
          @Override
          public int executeCommand(CommandContext context) {
            chip = cpu.getChip(context.getArgument(0));
            if (chip == null) {
              context.err.println("Can not find loggable: " + context.getArgument(0));
            }
            chip.setLogStream(context.out);
            return 0;
          }

          public void stopCommand(CommandContext context) {
            chip.clearLogStream();
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

  private static String getRegisterName(int register) {
    if (register >= 0 && register < MSP430Constants.REGISTER_NAMES.length) {
      return MSP430Constants.REGISTER_NAMES[register];
    } else {
      return "R" + register;
    }
  }
}
