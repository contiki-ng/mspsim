package se.sics.mspsim.util;

public interface ActiveComponent {
  public void setComponentRegistry(ComponentRegistry registry);
  public void start();
}
