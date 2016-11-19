package se.sics.mspsim.ui;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JFrame;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class JFrameWindowManager implements WindowManager {

	public ManagedWindow createWindow(final String name) {
		ManagedWindow w = new ManagedWindow() {
			private JFrame window = new JFrame(name);
			private boolean restored = false;
			private int ratio = -1;
			private Dimension oldsize=new Dimension(-1,-1);
			

			public void setSize(int width, int height) {
				window.setSize(width, height);
			}



			public void setBounds(int x, int y, int width, int height) {
				window.setBounds(x, y, width, height);
			}

			public void pack() {
				window.pack();
			}

			public void scale(double val,double aspect) {
				Dimension size=window.getSize();
				Dimension isize=window.getContentPane().getSize();
				Dimension bsize=new Dimension(size.width-isize.width,size.height-isize.height);
				
				double cur_aspect=(double)isize.width/isize.height;
				
				if(cur_aspect>aspect){
					isize.width=(int)(isize.height*aspect+.5);
				} else {
					isize.height=(int)(isize.width/aspect+.5);					
				}
				
				//window.getInsets().
				window.setSize((int)(isize.width*val+.5+bsize.width),(int)(isize.height*val+.5+bsize.height));
			}


			public void add(Component component) {
				window.add(component);
				if (!restored) {
					restored = true;
					WindowUtils.restoreWindowBounds(name, window);
				}

				window.addComponentListener( new ComponentListener() {
					@Override
					public void componentResized( ComponentEvent e ) {
					}
					@Override
					public void componentMoved( ComponentEvent e ) {
					}
					@Override
					public void componentShown( ComponentEvent e ) {}
					@Override
					public void componentHidden(ComponentEvent e) {}
				} );		

			}

			public void removeAll() {
				window.removeAll();
			}

			public void setAlwaysOnTop(boolean val) {
				window.setAlwaysOnTop( val);
			}

			public void setExitOnClose(){
				window.addWindowListener(new WindowAdapter(){
					public void windowClosing(WindowEvent e){
						System.exit(0);//cierra aplicacion
					}
				}); 
			}


			public boolean isVisible() {
				return window.isVisible();
			}

			public void setVisible(boolean b) {
				if (b != window.isVisible()) {
					if (b) {
						WindowUtils.addSaveOnShutdown(name, window);
					} else {
						WindowUtils.saveWindowBounds(name, window);
						WindowUtils.removeSaveOnShutdown(window);
					}
				}
				window.setVisible(b);
			}

			public String getTitle() {
				return window.getTitle();
			}

			public void setTitle(String name) {
				window.setTitle(name);
			}
		};

		return w;
	}

}
