/**
 * Copyright (c) 2007, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
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
 * This file is part of MSPSim.
 *
 * $Id$
 *
 * -----------------------------------------------------------------
 *
 * SerialMon
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.ui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import se.sics.mspsim.core.*;

public class SerialMon implements KeyListener, USARTListener {

  private static final String PREFIX = " > ";
  private static final int MAX_LINES = 200;
  private String name;
  private JFrame window;
  private USART usart;
  private JTextArea textArea;
  private JLabel statusLabel;
  private String text = "*** Serial mon for MSPsim ***\n";

  private int lines = 1;
  private boolean isUpdatePending = false;
  private StringBuilder keyBuffer = new StringBuilder();

  public SerialMon(USART usart, String name) {
    this.name = name;
    this.usart = usart;
    window = new JFrame(name);
//     window.setBounds(100, 100, 400,340);
    window.add(new JScrollPane(textArea = new JTextArea(20, 40),
			       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
	       BorderLayout.CENTER);
    textArea.setText(text);
    textArea.setEditable(false);
    statusLabel = new JLabel(PREFIX);
    keyBuffer.append(PREFIX);
    statusLabel.setOpaque(true);
    statusLabel.setBackground(Color.lightGray);
    window.add(statusLabel, BorderLayout.SOUTH);
    String key = "usart." + name;
    WindowUtils.restoreWindowBounds(key, window);
    WindowUtils.addSaveOnShutdown(key, window);
    window.setVisible(true);

    textArea.addKeyListener(this);
  }

  public void saveWindowBounds() {
    WindowUtils.saveWindowBounds("usart." + name, window);
  }

  public void dataReceived(USART source, int data) {
    if (data == '\n') {
      if (lines >= MAX_LINES) {
	int index = text.indexOf('\n');
	text = text.substring(index + 1);
      } else {
	lines++;
      }
    }
    text += (char)data;

    // Collapse several immediate updates
    if (!isUpdatePending) {
      isUpdatePending = true;
      SwingUtilities.invokeLater(new Runnable() {
	  public void run() {
	    isUpdatePending = false;

	    final String newText = text;
	    textArea.setText(newText);
	    textArea.setCaretPosition(newText.length());
	    textArea.repaint();
	  }
	});
    }
  }


  // -------------------------------------------------------------------
  // KeyListener
  // -------------------------------------------------------------------

  public void keyPressed(KeyEvent key) {
  }

  public void keyReleased(KeyEvent key) {
  }

  public void keyTyped(KeyEvent key) {
    char c = key.getKeyChar();
    // Send it to the usart!
    usart.byteReceived(c & 0xff);

    // Visualize the input
    if (c == '\n') {
      statusLabel.setText(PREFIX);
      keyBuffer = new StringBuilder();
      keyBuffer.append(PREFIX);
    } else {
      keyBuffer.append(c);
      statusLabel.setText(keyBuffer.toString());
    }
  }

}
