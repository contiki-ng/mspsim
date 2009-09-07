package se.sics.mspsim.profiler;

import se.sics.mspsim.core.Profiler;
import se.sics.mspsim.util.MapEntry;

public interface CallListener {

  public void functionCall(Profiler source,  MapEntry entry);

  public void functionReturn(Profiler source, MapEntry entry);

}
