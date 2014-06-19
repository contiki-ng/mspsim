/*
 * Copyright (c) 2007, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * $Id$
 *
 * -----------------------------------------------------------------
 *
 * HighlightSourceViewer
 *
 * Authors : Adam Dunkels, Joakim Eriksson, Niclas Finne
 * Created : 6 dec 2007
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.extutil.highlight;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.MemoryMonitor;
import se.sics.mspsim.core.Memory.AccessMode;
import se.sics.mspsim.core.Memory.AccessType;
import se.sics.mspsim.ui.SourceViewer;
import se.sics.mspsim.ui.WindowUtils;
import se.sics.mspsim.util.ELF;

/**
 *
 */
public class HighlightSourceViewer implements SourceViewer {

  private ELF elfData;
  private MSP430 cpu;
  private MemoryMonitor monitor;

  private JFrame window;
  private SyntaxHighlighter highlighter;
  private String currentFile;
  private ArrayList<File> path = null;
  private JFileChooser fileChooser;

  public HighlightSourceViewer(MSP430 cpu,ELF elfData) {
    this.elfData=elfData;
    this.cpu=cpu;    
  }

  private void setup() {
    if (window == null) {
      window = new JFrame("Source Viewer");
      window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

      LineNumberedBorder border = new LineNumberedBorder(LineNumberedBorder.LEFT_SIDE, LineNumberedBorder.RIGHT_JUSTIFY);
      border.setSeparatorColor(Color.lightGray);
      

      Scanner scanner = new CScanner();
      highlighter = new SyntaxHighlighter(24, 200, scanner);
      highlighter.setEditable(false);
      highlighter.setBorder(border);
      JScrollPane scroller = new JScrollPane(highlighter);
      scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      Container pane = window.getContentPane();
      pane.add(scroller);
      WindowUtils.restoreWindowBounds("SourceViewer", window);
      WindowUtils.addSaveOnShutdown("SourceViewer", window);

      String searchPath = System.getProperty("CONTIKI_PATH");
      if (searchPath != null) {
        addEnvPath(searchPath);
      }
      searchPath = System.getenv("CONTIKI_PATH");
      if (searchPath != null) {
        addEnvPath(searchPath);
      }
      
      setMonitor();
      
      MouseAdapter mouseListener = new MouseAdapter() {
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                int sourceLine=highlighter.getLine(highlighter.viewToModel(e.getPoint()))+1;
                int asmLine=elfData.getPC(currentFile, sourceLine);
                if(asmLine!=-1)editBreakpoint(asmLine);
                setBreakpointList();
            }
        }
    };
    highlighter.addMouseListener(mouseListener);       
      
    }
  }
  
  private void setMonitor(){
    monitor = new MemoryMonitor.Adapter() {
      private long lastCycles = -1;
      @Override
      public void notifyReadBefore(int address, AccessMode mode, AccessType type) {
          if (type == AccessType.EXECUTE && cpu.cycles != lastCycles) {
              cpu.triggBreakpoint();
              lastCycles = cpu.cycles;
          }
      }
  };    
  }
  
  public void setBreakpointList(){
    List<Integer> BreakList = new ArrayList<Integer>();
    for(int i=0;i<highlighter.getLineCount();i++){
      int asmLine=elfData.getPC(currentFile, i+1);
      if(asmLine>0){
        if(cpu.hasWatchPoint(asmLine)){
          BreakList.add(i);
        }
      }
    }
    Integer[] result = new Integer[BreakList.size()];
    result=BreakList.toArray(result);
    highlighter.setBreakpointList(result);
  }
  
  
  private void editBreakpoint(int pos){
    if (cpu.hasWatchPoint(pos)) {
      cpu.removeWatchPoint(pos, monitor); 
    }else{
      cpu.addWatchPoint(pos, monitor);
    }
  }
  
  
  
  private void addEnvPath(String searchPath) {
    String[] p = searchPath.split(File.pathSeparator);
    if (p != null) {
      for (int i = 0, n = p.length; i < n; i++) {
        addSearchPath(new File(p[i]));
      }
    }
  }

  public boolean isVisible() {
    return window != null && window.isVisible();
  }

  public void setVisible(boolean isVisible) {
    setup();
    window.setVisible(isVisible);
  }
  
  public void viewFile(final String path, final String filename,final boolean useChooser) {
    if (filename.equals(currentFile)) {
      // Already showing this file
      return;
    }
    

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          File file = findSourceFile(path, filename,useChooser);
          if (file != null) {
            currentFile = filename;
            setup();
            FileReader reader = new FileReader(file);
            try {
              highlighter.read(reader, null);
              // Workaround for bug 4782232 in Java 1.4
              highlighter.setCaretPosition(1);
              highlighter.setCaretPosition(0);
	      window.setTitle("Source Viewer (" + file.getAbsolutePath()
			      + ')');
              if (!window.isVisible()) {
                window.setVisible(true);
              }
            } finally {
              reader.close();
            }
          }
        } catch (IOException err) {
          err.printStackTrace();
          JOptionPane.showMessageDialog(window, "Failed to read the file '" + filename + '\'', "Could not read file", JOptionPane.ERROR_MESSAGE);
        }
      }
    });
  }

  public void viewLine(final int line) {
    if (highlighter != null) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (highlighter.getDocument().getLength()>0){
            highlighter.viewLine(line - 1);
            if (!window.isVisible()) {
              window.setVisible(true);
            }
          }
        }
      });
    }
  }

  public void addSearchPath(File directory) {
    if (path == null) {
      path = new ArrayList<File>();
    }
    path.add(directory);
  }

  public void removeSearchPath(File directory) {
    if (path != null) {
      path.remove(directory);
    }
  }

  private File findSourceFile(String fPath, String filename, boolean useChooser) {
    File fp = new File(fPath, filename);
    if (fp.exists()) {
      return fp;
    }
    fp = new File(filename);
    if (fp.exists()) {
      return fp;
    }

    if (path != null) {
      for(File p : path) {
        File nfp = new File(p, filename);
        if (nfp.exists()) {
          return nfp;
        }
      }
    } else {
      path = new ArrayList<File>();
    }
    // Find new path to search from
    if (fileChooser == null) {
      fileChooser = new JFileChooser("./");
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      fileChooser.setDialogTitle("Select compilation directory");
    }
    if (useChooser){ 
      if (!window.isVisible()) {
        window.setVisible(true);
      }
    if (fileChooser.showOpenDialog(window) == JFileChooser.APPROVE_OPTION) {
      File d = fileChooser.getSelectedFile();
      if (d != null) {
	path.add(d);
	return findSourceFile(fPath, filename, useChooser);
      }
    }
    }
    return null;
  }

  public static void main(String[] args) {
//    HighlightSourceViewer sv = new HighlightSourceViewer();
//    sv.setVisible(true);
//    sv.viewFile(".", args[0]);
  }
}
