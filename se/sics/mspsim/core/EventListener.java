package se.sics.mspsim.core;

public interface EventListener {
  public void event(EventSource source, String event, Object data);
}
