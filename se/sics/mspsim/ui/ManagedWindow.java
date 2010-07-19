package se.sics.mspsim.ui;

import java.awt.Component;

public interface ManagedWindow {

  public void setBounds(int x, int y, int width, int height);

  public void add(Component component);
  public void removeAll();

  public boolean isVisible();

  public void setVisible(boolean b);

  public String getTitle();

  public void setTitle(String string);

}
