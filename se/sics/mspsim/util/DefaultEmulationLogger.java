package se.sics.mspsim.util;

import java.io.PrintStream;

import se.sics.mspsim.core.EmulationException;
import se.sics.mspsim.core.EmulationLogger;
import se.sics.mspsim.core.LogListener;
import se.sics.mspsim.core.Loggable;
import se.sics.mspsim.core.MSP430Core;

public class DefaultEmulationLogger implements EmulationLogger {

  private WarningMode warningMode = WarningMode.PRINT;
  private PrintStream out;
  private final MSP430Core cpu;
  
  public DefaultEmulationLogger(MSP430Core cpu, PrintStream out) {
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

  @Override
  public void log(Loggable source, String message) {
      // TODO Auto-generated method stub

  }

  @Override
  public void logw(Loggable source, WarningType type, String message)
          throws EmulationException {
      // TODO Auto-generated method stub

  }

  @Override
  public void setDefaultWarningMode(WarningMode mode) {
      // TODO Auto-generated method stub

  }

  @Override
  public void setWarningMode(WarningType type, WarningMode mode) {
      // TODO Auto-generated method stub

  }

@Override
public void addLogListener(LogListener listener) {
    // TODO Auto-generated method stub
    
}

@Override
public void removeLogListener(LogListener listener) {
    // TODO Auto-generated method stub
    
}
}
