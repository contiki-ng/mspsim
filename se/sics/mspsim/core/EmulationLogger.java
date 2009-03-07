package se.sics.mspsim.core;

public interface EmulationLogger {

  /* warning mode for CPU errors such as unaligned word access */
  public enum WarningMode {SILENT, PRINT, EXCEPTION};

  public void log(Object source, String message);
  public void warning(Object source, String message) throws EmulationException;
  public void setWarningMode(WarningMode mode);
  
}
