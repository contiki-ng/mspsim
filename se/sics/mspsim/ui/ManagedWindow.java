package se.sics.mspsim.ui;

import java.awt.Component;

public interface ManagedWindow {

  public void add(Component component);

  public void setVisible(boolean b);

  public void setTitle(String string);

}
