package se.sics.mspsim.util;

public interface ActiveComponent {
  public void init(String name, ComponentRegistry registry);
  public void start();
}
