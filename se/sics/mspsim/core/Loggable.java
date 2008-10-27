package se.sics.mspsim.core;

import java.io.PrintStream;

public interface Loggable {
  public void setLogStream(PrintStream out);
  public void clearLogStream();
}
