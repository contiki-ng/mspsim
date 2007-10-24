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
 * $Id: ESBNode.java,v 1.3 2007/10/21 21:17:34 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * ESBNode
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.platform.esb;
import java.io.IOException;

import se.sics.mspsim.core.*;
import se.sics.mspsim.util.*;

public class ESBNode implements PortListener {

  public static final boolean DEBUG = false;

  public static final int PIR_PIN = 3;
  public static final int VIB_PIN = 4;
  // Port 2.
  public static final int BUTTON_PIN = 7;

  private MSP430 cpu;
  private IOPort port1;
  private IOPort port2;

  public static final int RED_LED = 0x01;
  public static final int GREEN_LED = 0x02;
  public static final int YELLOW_LED = 0x04;
  public static final int BEEPER = 0x08;

  public boolean redLed;
  public boolean greenLed;
  public boolean yellowLed;

  public ESBGui gui;
  /**
   * Creates a new <code>ESBNode</code> instance.
   *
   */
  public ESBNode(MSP430 cpu) {
    this.cpu = cpu;
    IOUnit unit = cpu.getIOUnit("Port 2");
    if (unit instanceof IOPort) {
      port2 = (IOPort) unit;
      System.out.println("Found port 2!!!");
      port2.setPortListener(this);
    }

    unit = cpu.getIOUnit("Port 1");
    if (unit instanceof IOPort) {
      port1 = (IOPort) unit;
    }

  }

  public void setPIR(boolean hi) {
    port1.setPinState(PIR_PIN, hi ? IOPort.PIN_HI : IOPort.PIN_LOW);
  }

  public void setVIB(boolean hi) {
    port1.setPinState(VIB_PIN, hi ? IOPort.PIN_HI : IOPort.PIN_LOW);
  }

  public void setButton(boolean hi) {
    port2.setPinState(BUTTON_PIN, hi ? IOPort.PIN_HI : IOPort.PIN_LOW);
  }

  public boolean getDebug() {
    return cpu.getDebug();
  }
  public void setDebug(boolean debug) {
    cpu.setDebug(debug);
  }


  public MSP430 getCPU() {
    return cpu;
  }


  public void portWrite(IOPort source, int data) {
    //    System.out.println("ESB: Writing to port: " + data);
    if (source == port2) {
//       System.out.println("ESBNode.PORT2: 0x" + Integer.toString(data,16));
      redLed = (data & RED_LED) == 0;
      if (DEBUG && greenLed != ((data & GREEN_LED) == 0)) {
	System.out.println("Green toggled!");
      }
      greenLed = (data & GREEN_LED) == 0;
      if (DEBUG && yellowLed != ((data & YELLOW_LED) == 0)) {
	System.out.println("Yellow toggled!");
      }
      yellowLed = (data & YELLOW_LED) == 0;
      if (gui != null) {
	gui.repaint();
	gui.beeper.beepOn((data & BEEPER) != 0);
      }
    }
  }

  public static void main(String[] args) throws IOException {
    final MSP430 cpu = new MSP430(0);
    // Monitor execution
    cpu.setMonitorExec(true);

    int[] memory = cpu.getMemory();

    if (args[0].endsWith("ihex")) {
      // IHEX Reading
      IHexReader reader = new IHexReader();
      reader.readFile(memory, args[0]);
    } else {
      ELF elf = ELF.readELF(args[0]);
      elf.loadPrograms(memory);
      MapTable map = elf.getMap();
      cpu.getDisAsm().setMap(map);
      cpu.setMap(map);
    }

    cpu.reset();
    ESBNode node = new ESBNode(cpu);
    node.gui = new ESBGui(node);
    ControlUI control = new ControlUI(cpu);

    if (args.length > 1) {
      MapTable map = new MapTable(args[1]);
      cpu.getDisAsm().setMap(map);
      cpu.setMap(map);

      // An illustration on how to add a breakpoint!
//       if (map != null) {
// 	int adr = map.getFunctionAddress("process_run");
// 	if (adr != -1) {
// 	  cpu.setBreakPoint(adr, new CPUMonitor() {
// 	      public void cpuAction(int type, int adr, int data) {
// 		System.out.println("Break at: " + cpu.reg[cpu.PC]);
// 		cpu.stop();
// 	      }
// 	    });
// 	}
//       }

    }
    cpu.cpuloop();
  }
}
