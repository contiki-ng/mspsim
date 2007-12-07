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
 * $Id: SkyNode.java,v 1.4 2007/10/22 18:03:42 joakime Exp $
 *
 * -----------------------------------------------------------------
 *
 * SkyNode
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/22 18:03:42 $
 *           $Revision: 1.4 $
 */

package se.sics.mspsim.platform.sky;
import java.io.IOException;

import se.sics.mspsim.chip.CC2420;
import se.sics.mspsim.core.*;
import se.sics.mspsim.extutil.highlight.HighlightSourceViewer;
import se.sics.mspsim.util.*;
import java.io.File;

/**
 * Emulation of Sky Mote
 */
public class SkyNode implements PortListener, USARTListener {

  public static final boolean DEBUG = false;

  // Port 2.
  public static final int BUTTON_PIN = 7;

  public static final int CC2420_CHIP_SELECT = 0x04;
  /* P1.0 - Input: FIFOP from CC2420 */
  /* P1.3 - Input: FIFO from CC2420 */
  /* P1.4 - Input: CCA from CC2420 */
  public static final int CC2420_FIFOP = 0;
  public static final int CC2420_FIFO = 3;
  public static final int CC2420_CCA = 4;

  private MSP430 cpu;
  private IOPort port1;
  private IOPort port2;
  private IOPort port4;
  private IOPort port5;

  private CC2420 radio;
  private ExtFlash flash;

  public static final int BLUE_LED = 0x40;
  public static final int GREEN_LED = 0x20;
  public static final int RED_LED = 0x10;

  public boolean redLed;
  public boolean blueLed;
  public boolean greenLed;

  public SkyGui gui;
  /**
   * Creates a new <code>SkyNode</code> instance.
   *
   */
  public SkyNode(MSP430 cpu) {
    this.cpu = cpu;
    IOUnit unit = cpu.getIOUnit("Port 5");
    if (unit instanceof IOPort) {
      port5 = (IOPort) unit;
      port5.setPortListener(this);
    }

    unit = cpu.getIOUnit("Port 1");
    if (unit instanceof IOPort) {
      port1 = (IOPort) unit;
    }

    unit = cpu.getIOUnit("Port 2");
    if (unit instanceof IOPort) {
      port2 = (IOPort) unit;
    }

    IOUnit usart0 = cpu.getIOUnit("USART 0");
    if (usart0 instanceof USART) {
      radio = new CC2420();
      radio.setCCAPort(port1, CC2420_CCA);
      radio.setFIFOPPort(port1, CC2420_FIFOP);
      radio.setFIFOPort(port1, CC2420_FIFO);
      flash = new ExtFlash((USART)usart0);
      ((USART) usart0).setUSARTListener(this);
      port4 = (IOPort) cpu.getIOUnit("Port 4");
      if (port4 != null && port4 instanceof IOPort) {
      	System.out.println("Found port 4!!!");
      	((IOPort) port4).setPortListener(this);
      }
    }
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
    if (source == port5) {
      redLed = (data & RED_LED) == 0;
      blueLed = (data & BLUE_LED) == 0;
      greenLed = (data & GREEN_LED) == 0;
      if (gui != null) {
	gui.repaint();
      }
    } else if (source == port4) {
      // Chip select = active low...
      radio.setChipSelect((data & CC2420_CHIP_SELECT) == 0);
      //radio.portWrite(source, data);
      flash.portWrite(source, data);
    }
  }

  // USART Listener
  public void dataReceived(USART source, int data) {
    radio.dataReceived(source, data);
    flash.dataReceived(source, data);
  }


  public void radioIncomingPacket(int[] data) {
    radio.setIncomingPacket(data);
  }

  public static void main(String[] args) throws IOException {
    final MSP430 cpu = new MSP430(0);
    // Monitor execution
    cpu.setMonitorExec(true);
    //cpu.setDebug(true);
    ELF elf = null;
    int[] memory = cpu.getMemory();

    if (args[0].endsWith("ihex")) {
      // IHEX Reading
      IHexReader reader = new IHexReader();
      reader.readFile(memory, args[0]);
    } else {
      elf = ELF.readELF(args[0]);
      elf.loadPrograms(memory);
      MapTable map = elf.getMap();
      cpu.getDisAsm().setMap(map);
      cpu.setMap(map);
    }

    cpu.reset();
    SkyNode node = new SkyNode(cpu);
    node.gui = new SkyGui(node);
    ControlUI control = new ControlUI(cpu, elf);
    HighlightSourceViewer sourceViewer = new HighlightSourceViewer();
    sourceViewer.addSearchPath(new File("e:/work/contiki-2.x/examples/sky/"));
    control.setSourceViewer(sourceViewer);

    if (args.length > 1) {
      MapTable map = new MapTable(args[1]);
      cpu.getDisAsm().setMap(map);
    }
    cpu.cpuloop();
  }
}
