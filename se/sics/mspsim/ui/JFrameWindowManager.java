package se.sics.mspsim.ui;

import java.awt.Component;

import javax.swing.JFrame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class JFrameWindowManager implements WindowManager {

    public ManagedWindow createWindow(final String name) {
        ManagedWindow w = new ManagedWindow() {
            private JFrame window = new JFrame(name);
            private boolean restored = false;

            public void setSize(int width, int height) {
                window.setSize(width, height);
            }
            


            public void setBounds(int x, int y, int width, int height) {
                window.setBounds(x, y, width, height);
            }

            public void pack() {
                window.pack();
            }

            public void add(Component component) {
                window.add(component);
                if (!restored) {
                    restored = true;
                    WindowUtils.restoreWindowBounds(name, window);
                }
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
