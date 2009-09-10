package se.sics.mspsim.ui;

import java.awt.Component;

import javax.swing.JFrame;

public class JFrameWindowManager implements WindowManager {

    public ManagedWindow createWindow(final String name) {
        ManagedWindow w = new ManagedWindow() {
            private JFrame window = new JFrame(name);
            private boolean restored = false;
            
            public void add(Component component) {
                if (!restored) {
                    WindowUtils.restoreWindowBounds(name, window);
                    WindowUtils.addSaveOnShutdown(name, window);
                }
                window.add(component);
                window.pack();
            }
            public void setVisible(boolean b) {
                window.setVisible(b);
            }
            public void setTitle(String name) {
                window.setTitle(name);
            }
        };
        return w;
    }

}
