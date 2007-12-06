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
 * $Id: ControlUI.java,v 1.4 2007/10/21 22:19:07 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * ControlUI
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 22:19:07 $
 *           $Revision: 1.4 $
 */

package se.sics.mspsim.util;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import se.sics.mspsim.core.*;

public class ControlUI extends JPanel implements ActionListener {

  private static final String TITLE = "MSPSim monitor";

  private JFrame window;
  private MSP430 cpu;
  private DebugUI dui;
  private JFrame stackWindow;
  private StackUI stackUI;

  private ELF elfData;
  private SourceViewer sourceViewer;

  private Action stepAction;

  public ControlUI(MSP430 cpu) {
    this(cpu, null);
  }

  public ControlUI(MSP430 cpu, ELF elf) {
    super(new GridLayout(0, 1));
    this.cpu = cpu;

    this.stackUI = new StackUI(cpu);
    stackWindow = new JFrame("Stack");
    stackWindow.add(this.stackUI);
    WindowUtils.restoreWindowBounds("StackUI", stackWindow);
    WindowUtils.addSaveOnShutdown("StackUI", stackWindow);
    stackWindow.setVisible(true);

    window = new JFrame(TITLE);
//     window.setSize(320,240);
    window.setLayout(new BorderLayout());
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    window.add(this, BorderLayout.WEST);
    window.add(dui = new DebugUI(cpu), BorderLayout.CENTER);

    createButton("Debug On");
    createButton("Stop");

    stepAction = new AbstractAction("Single Step") {
	public void actionPerformed(ActionEvent e) {
	  System.out.println("step");
	  ControlUI.this.cpu.step();
	  dui.repaint();
	}
      };
    stepAction.putValue(Action.MNEMONIC_KEY,
			new Integer(KeyEvent.VK_S));
    stepAction.setEnabled(false);

    JButton stepButton = new JButton(stepAction);
    add(stepButton);

    if (elf != null) {
      createButton("Show Source");
    }
    createButton("Profile Dump");

    // Setup standard actions
    stepButton.getInputMap(WHEN_IN_FOCUSED_WINDOW)
      .put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK),
	   "cpuStep");
    stepButton.getActionMap().put("cpuStep", stepAction);

    WindowUtils.restoreWindowBounds("ControlUI", window);
    WindowUtils.addSaveOnShutdown("ControlUI", window);
    window.setVisible(true);
    elfData = elf;
  }

  public void setSourceViewer(SourceViewer viewer) {
    sourceViewer = viewer;
  }


  private JButton createButton(String text) {
    JButton jb = new JButton(text);
    jb.addActionListener(this);
    add(jb);
    return jb;
  }

  private void updateCPUPercent() {
    window.setTitle(TITLE + "  CPU On: " + cpu.getCPUPercent() + "%");
  }

  public void actionPerformed(ActionEvent ae) {
    String cmd = ae.getActionCommand();
    updateCPUPercent();
    if ("Debug On".equals(cmd)) {
      cpu.setDebug(true);
      ((JButton) ae.getSource()).setText("Debug Off");

    } else if ("Debug Off".equals(cmd)) {
      cpu.setDebug(false);
      ((JButton) ae.getSource()).setText("Debug On");

    } else if ("Run".equals(cmd)) {
      new Thread(new Runnable() {
	  public void run() {
	    cpu.cpuloop();
	  }}).start();
      ((JButton) ae.getSource()).setText("Stop");
      stepAction.setEnabled(false);

    } else if ("Stop".equals(cmd)) {
      cpu.stop();
      ((JButton) ae.getSource()).setText("Run");
      stepAction.setEnabled(true);

    } else if ("Profile Dump".equals(cmd)) {
      if (cpu.getProfiler() != null) {
	cpu.getProfiler().printProfile();
      } else {
	System.out.println("*** No profiler available");
      }
      //     } else if ("Single Step".equals(cmd)) {
      //       cpu.step();
//       dui.repaint();
    } else if ("Show Source".equals(cmd)) {
      int pc = cpu.readRegister(cpu.PC);
      if (elfData != null) {
	DebugInfo dbg = elfData.getDebugInfo(pc);
	if (dbg != null) {
	  if (sourceViewer != null) {
	    sourceViewer.viewFile(dbg.getPath(), dbg.getFile());
	    sourceViewer.viewLine(dbg.getLine());
	  } else {
	    System.out.println("File: " + dbg.getFile());
	    System.out.println("LineNr: " + dbg.getLine());
	  }
	}
      }
    }
    dui.updateRegs();
  }

}
