package se.sics.mspsim.util;

import java.io.PrintStream;

import se.sics.mspsim.core.EmulationException;
import se.sics.mspsim.core.EmulationLogger;
import se.sics.mspsim.core.MSP430;

public class DefaultEmulationLogger implements EmulationLogger {

  private WarningMode warningMode = WarningMode.PRINT;
  private PrintStream out;
  private MSP430 cpu;
  
  public DefaultEmulationLogger(MSP430 cpu, PrintStream out) {
    this.cpu = cpu;
    this.out = out;
  }
  
  public void warning(Object source, String message) throws EmulationException {
    if (warningMode == WarningMode.EXCEPTION) {
      throw new EmulationException(message);
    } else {
      if (warningMode == WarningMode.PRINT) {
        out.println(message);
        cpu.generateTrace(out);
      }
    }
  }

  public void setWarningMode(WarningMode mode) {
    warningMode = mode;
  }

  public void log(Object source, String message) {
    out.println(message);
  }
}
