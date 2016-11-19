package se.sics.mspsim.ui;

import java.awt.Component;

public interface ManagedWindow {

	public void scale(double val,double aspect);
	public void setSize(int width, int height);
	public void setBounds(int x, int y, int width, int height);
	public void pack();
	public void setAlwaysOnTop(boolean val);

	public void add(Component component);
	public void removeAll();

	public void setExitOnClose();

	public boolean isVisible();

	public void setVisible(boolean b);

	public String getTitle();

	public void setTitle(String string);

}
