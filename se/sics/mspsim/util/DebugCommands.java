package se.sics.mspsim.util;

import se.sics.mspsim.core.CPUMonitor;
import se.sics.mspsim.core.MSP430;

public class DebugCommands implements ActiveComponent {

  private ComponentRegistry registry;
  public void setComponentRegistry(ComponentRegistry registry) {
    this.registry = registry;
  }

  public void start() {
    CommandHandler ch = (CommandHandler) registry.getComponent(CommandHandler.class);
    final MSP430 cpu = (MSP430) registry.getComponent(MSP430.class);
    if (ch != null && cpu != null) {
      ch.registerCommand("break", new Command() {
        public int executeCommand(final CommandContext context) {
          int baddr = context.getArgumentAsAddress(0);
          cpu.setBreakPoint(baddr,
              new CPUMonitor() {
                public void cpuAction(int type, int adr, int data) {
                  context.out.println("*** Break at " + adr);
                }
          });
          context.out.println("Breakpoint set at: " + baddr);
          return 0;
        }

        public String getArgumentHelp(CommandContext context) {
          return "break";
        }

        public String getCommandHelp(CommandContext context) {
          return "adds a breakpoint to a given address or symbol";
        }   
      });
      
      ch.registerCommand("watch", new Command() {
        public int executeCommand(final CommandContext context) {
          int baddr = context.getArgumentAsAddress(0);
          cpu.setBreakPoint(baddr,
              new CPUMonitor() {
                public void cpuAction(int type, int adr, int data) {
                  context.out.println("*** Write: " + adr + " = " + data);
                }
          });
          context.out.println("Watch set at: " + baddr);
          return 0;
        }

        public String getArgumentHelp(CommandContext context) {
          return "watch";
        }

        public String getCommandHelp(CommandContext context) {
          return "adds a write watch to a given address or symbol";
        }
      });
      
    }
  }
}
