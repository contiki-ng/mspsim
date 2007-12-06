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
 * $Id: $
 *
 * -----------------------------------------------------------------
 *
 * HighlightSourceViewer
 *
 * Authors : Adam Dunkels, Joakim Eriksson, Niclas Finne
 * Created : 6 dec 2007
 * Updated : $Date: 6 dec 2007 $
 *           $Revision: 1.0 $
 */

package se.sics.mspsim.extutil.highlight;

import java.awt.Container;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import se.sics.mspsim.util.SourceViewer;
import se.sics.mspsim.util.WindowUtils;

/**
 *
 */
public class HighlightSourceViewer implements SourceViewer {

  private JFrame window;
  private SyntaxHighlighter highlighter;
  
  public HighlightSourceViewer() {
    //
  }

  private void setup() {
    if (window == null) {
      window = new JFrame("Source Viewer");
      window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

      Scanner scanner = new CScanner();
      highlighter = new SyntaxHighlighter(24, 80, scanner);
      highlighter.setBorder(new LineNumberedBorder(LineNumberedBorder.LEFT_SIDE, LineNumberedBorder.RIGHT_JUSTIFY));
      JScrollPane scroller = new JScrollPane(highlighter);
      scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      Container pane = window.getContentPane();
      pane.add(scroller);
      WindowUtils.restoreWindowBounds("SourceViewer", window);
      WindowUtils.addSaveOnShutdown("SourceViewer", window);
    }
  }

  public boolean isVisible() {
    return window != null && window.isVisible();
  }

  public void setVisible(boolean isVisible) {
    setup();
    window.setVisible(isVisible);
  }

  public void viewFile(final String file) {
    setup();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          FileReader reader = new FileReader(file);
          try {
            highlighter.read(reader, null);
            // Workaround for bug 4782232 in Java 1.4
            highlighter.setCaretPosition(1);
            highlighter.setCaretPosition(0);
          } finally {
            reader.close();
          }
        } catch (IOException err) {
          err.printStackTrace();
          JOptionPane.showMessageDialog(window, "Failed to read the file '" + file + '\'', "Could not read file", JOptionPane.ERROR_MESSAGE);
        }
      }
    });
  }

  public void viewLine(final int line) {
    if (highlighter != null) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          if (line >= 0 && line < highlighter.getLineCount()) {
            highlighter.setCaretPosition(highlighter.getLineStartOffset(line));
          }
        }
      });
    }
  }

  public static void main(String[] args) {
    HighlightSourceViewer sv = new HighlightSourceViewer();
    sv.setVisible(true);
    sv.viewFile(args[0]);
  }
}
