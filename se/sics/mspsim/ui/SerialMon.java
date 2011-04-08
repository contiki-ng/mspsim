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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.StateChangeListener;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.core.USARTSource;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.ServiceComponent;

public class SerialMon implements USARTListener, StateChangeListener, ServiceComponent {

  private static final int MAX_LINES = 200;

  private String name;
  private ServiceComponent.Status status = Status.STOPPED;

  private final USARTSource usart;
  private final String title;  

  private JFrame window;
  private JTextArea textArea;
  private JTextField commandField;
  private String[] history = new String[50];
  private int historyPos = 0;
  private int historyCount = 0;
  private String text = "*** Serial mon for MSPsim ***\n";

  private final static int QUEUE_SIZE = 5;
  private String[] queue = new String[QUEUE_SIZE];
  private int wPos = 0, rPos = 0, rIndex = 0;
  private boolean isSending = false;

  private int lines = 1;
  private boolean isUpdatePending = false;

  public SerialMon(USARTSource usart, String title) {
    this.usart = usart;
    this.title = title;
  }

  public Status getStatus() {
    return status;
  }

  public String getName() {
    return name;
  }

  public final void init(String name, ComponentRegistry registry) {
    this.name = name;
  }

  private void initGUI() {
    window = new JFrame(title);
//     window.setBounds(100, 100, 400,340);
    window.add(new JScrollPane(textArea = new JTextArea(20, 40),
			       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
	       BorderLayout.CENTER);
    textArea.setText(text);
    textArea.setEditable(false);

    JPopupMenu popupMenu = new JPopupMenu();
    JMenuItem clearItem = new JMenuItem("Clear");
    clearItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        textArea.setText("");
        lines = 0;
      }

    });
    popupMenu.add(clearItem);
    textArea.setComponentPopupMenu(popupMenu);

    commandField = new JTextField();
    commandField.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        String command = commandField.getText().trim();
        if (command.length() > 0) {
          if (sendCommand(command)) {
            int previous = historyCount - 1;
            if (previous < 0) previous += history.length;
            if (!command.equals(history[previous])) {
                history[historyCount] = command;
                historyCount = (historyCount + 1) % history.length;
            }
            historyPos = historyCount;
            commandField.setText("");
          } else {
            commandField.getToolkit().beep();
          }
        }
      }

    });
    commandField.addKeyListener(new KeyAdapter() {

      @Override
      public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
        case KeyEvent.VK_UP: {
          int nextPos = (historyPos + history.length - 1) % history.length;
          if (nextPos == historyCount || history[nextPos] == null) {
            commandField.getToolkit().beep();
          } else {
            String cmd = commandField.getText().trim();
            if (cmd.length() > 0) {
              history[historyPos] = cmd;
            }
            historyPos = nextPos;
            commandField.setText(history[historyPos]);
          }
          break;
        }
        case KeyEvent.VK_DOWN: {
          int nextPos = (historyPos + 1) % history.length;
          if (nextPos == historyCount) {
            historyPos = nextPos;
            commandField.setText("");
          } else if (historyPos == historyCount || history[nextPos] == null) {
            commandField.getToolkit().beep();
          } else {
            String cmd = commandField.getText().trim();
            if (cmd.length() > 0) {
              history[historyPos] = cmd;
            }
            historyPos = nextPos;
            commandField.setText(history[historyPos]);
          }
          break;
        }
        }
      }

    });
    window.add(commandField, BorderLayout.SOUTH);

    String key = "usart." + name;
    WindowUtils.restoreWindowBounds(key, window);
    WindowUtils.addSaveOnShutdown(key, window);
  }

  public void start() {
    if (window == null) {
      initGUI();
      usart.setUSARTListener(this);
      ((IOUnit) usart).addStateChangeListener(this);
    }
    window.setVisible(true);
    status = Status.STARTED;
  }

  public final void stop() {
    status = Status.STOPPED;
    if (window != null) {
      window.setVisible(false);
    }
  }

  public void dataReceived(USARTSource source, int data) {
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

  protected boolean sendCommand(String command) {
    int next = (wPos + 1) % QUEUE_SIZE;
    if (next == rPos) {
      /* Queue full */
      commandField.setEnabled(false);
      return false;
    }
    queue[wPos] = command + '\n';
    wPos = next;
    if (!isSending) {
      isSending = true;
      sendNext();
    }
    return true;
  }

  public void stateChanged(Object source, int old, int state) {
    if (state == USARTListener.RXFLAG_CLEARED && isSending) {
      sendNext();
    }
  }

  private void sendNext() {
    boolean updateCommand = false;
    while (isSending && usart.isReceiveFlagCleared()) {
      char c = queue[rPos].charAt(rIndex++);
      usart.byteReceived((byte)c);
      dataReceived(usart, c);
      if (rIndex >= queue[rPos].length()) {
        rIndex = 0;
        rPos = (rPos + 1) % QUEUE_SIZE;
        if (rPos == wPos) {
          isSending = false;
        }
        updateCommand = true;
      }
    }
    if (updateCommand && !commandField.isEnabled()) {
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          commandField.setEnabled(true);
        }
      });
    }
  }
}
