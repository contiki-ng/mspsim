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
 * $Id: StackUI.java,v 1.3 2007/10/21 21:17:35 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * StackUI
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:35 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.util;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JPanel;

import se.sics.mspsim.core.*;

public class StackUI extends JPanel implements CPUMonitor {

  private static final int STACK_FRAME = 1024;
  private int updateCyclePeriod = 2500;

  private MSP430 cpu;
  private int heapStartAddress;
  private int stackStartAddress = 0xa00;
  private DotDiagram diagram;
  private int[] minData = new int[STACK_FRAME];
  private int[] maxData = new int[STACK_FRAME];
  private String[] notes = new String[STACK_FRAME];

  private long lastCycles = 0;
  private int pos = 0;

  public StackUI(MSP430 cpu) {
    this(cpu, 2500);
  }

  public StackUI(MSP430 cpu, int updateCyclePeriod) {
    super(new BorderLayout());
    this.updateCyclePeriod = updateCyclePeriod;
    this.cpu = cpu;
    this.cpu.setRegisterWriteMonitor(MSP430.SP, this);

    if (cpu.getDisAsm() != null) {
      MapTable mapTable = cpu.getDisAsm().getMap();
      if (mapTable != null) {
	this.heapStartAddress = mapTable.heapStartAddress;
	this.stackStartAddress = mapTable.stackStartAddress;
      }
    }

    diagram = new DotDiagram(2);
    diagram.setDotColor(0, Color.green);
    diagram.setDotColor(1, Color.green);
    diagram.addConstant(Color.red,
        this.stackStartAddress - this.heapStartAddress);
    add(diagram, BorderLayout.CENTER);
    setPreferredSize(new Dimension(320, 200));
  }

  public void addNote(String note) {
    notes[pos] = note;
  }

  // -------------------------------------------------------------------
  // CPUMonitor
  // -------------------------------------------------------------------

  public void cpuAction(int type, int adr, int data) {
    int size = ((stackStartAddress - data) + 0xffff) % 0xffff;
    if (this.minData[pos] > size) {
      this.minData[pos] = size;
    }
    if (this.maxData[pos] < size) {
      this.maxData[pos] = size;
    }
    if (cpu.cpuCycles - lastCycles > updateCyclePeriod) {
      lastCycles = cpu.cpuCycles;
      //System.out.println("STACK UPDATE: " + type + "," + adr + "," + data);
      pos = (pos + 1) % this.minData.length;
      this.minData[pos] = 0;
      this.maxData[pos] = 0;
      this.notes[pos] = null;
      diagram.setData(0, this.minData, pos, this.minData.length);
      diagram.setDataWithNotes(1, this.maxData, notes, pos, this.maxData.length);
    }
  }

}
