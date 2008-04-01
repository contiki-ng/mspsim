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
 * SkyNode
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.platform.sky;
import java.io.IOException;

import se.sics.mspsim.chip.CC2420;
import se.sics.mspsim.chip.M25P80;
import se.sics.mspsim.chip.FileM25P80;
import se.sics.mspsim.chip.PacketListener;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.USART;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.extutil.jfreechart.DataChart;
import se.sics.mspsim.extutil.jfreechart.DataSourceSampler;
import se.sics.mspsim.platform.GenericNode;
//import se.sics.mspsim.util.ArgumentManager;
import se.sics.mspsim.util.ArgumentManager;
import se.sics.mspsim.util.ELF;
import se.sics.mspsim.util.NetworkConnection;
import se.sics.mspsim.util.OperatingModeStatistics;

/**
 * Emulation of Sky Mote
 */
public class SkyNode extends GenericNode implements PortListener, USARTListener {

  public static final boolean DEBUG = false;

  public static final int MODE_LEDS_OFF = 0;
  public static final int MODE_LEDS_1 = 1;
  public static final int MODE_LEDS_2 = 2;
  public static final int MODE_LEDS_3 = 3;
  public static final int MODE_MAX = MODE_LEDS_3;
  // Port 2.
  public static final int BUTTON_PIN = 7;

  /* P1.0 - Input: FIFOP from CC2420 */
  /* P1.3 - Input: FIFO from CC2420 */
  /* P1.4 - Input: CCA from CC2420 */
  public static final int CC2420_FIFOP = 0;
  public static final int CC2420_FIFO = 3;
  public static final int CC2420_CCA = 4;

  /* P4.5 - Output: VREG_EN to CC2420 */
  /* P4.2 - Output: SPI Chip Select (CS_N) */
  public static final int CC2420_VREG = (1 << 5);
  public static final int CC2420_CHIP_SELECT = 0x04;

  private IOPort port1;
  private IOPort port2;
  private IOPort port4;
  private IOPort port5;

  public CC2420 radio;
  public NetworkConnection network;
  
  
  private M25P80 flash;
  private String flashFile;

  public static final int BLUE_LED = 0x40;
  public static final int GREEN_LED = 0x20;
  public static final int RED_LED = 0x10;

  public boolean redLed;
  public boolean blueLed;
  public boolean greenLed;
  private int mode = MODE_LEDS_OFF;

  public SkyGui gui;
  /**
   * Creates a new <code>SkyNode</code> instance.
   *
   */
  public SkyNode() {
  }

  public void setButton(boolean hi) {
    port2.setPinState(BUTTON_PIN, hi ? IOPort.PIN_HI : IOPort.PIN_LOW);
  }

  public boolean getDebug() {
    return cpu.getDebug();
  }

  public ELF getElfInfo() {
    return elf;
  }

  public M25P80 getFlash() {
    return flash;
  }

  public void setFlash(M25P80 flash) {
    this.flash = flash;
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
      int newMode = (redLed ? 1 : 0) + (greenLed ? 1 : 0) + (blueLed ? 1 : 0);
      if (mode != newMode) {
        mode = newMode;
        modeChanged(mode);
      }

      if (gui != null) {
	gui.repaint();
      }
    } else if (source == port4) {
      // Chip select = active low...
      radio.setChipSelect((data & CC2420_CHIP_SELECT) == 0);
      radio.setVRegOn((data & CC2420_VREG) != 0);
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

  public int getModeMax() {
    return MODE_MAX;
  }

  public String getName() {
    return "Tmote Sky";
  }

  public void setupNodePorts() {
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
      radio = new CC2420(cpu);
      radio.setCCAPort(port1, CC2420_CCA);
      radio.setFIFOPPort(port1, CC2420_FIFOP);
      radio.setFIFOPort(port1, CC2420_FIFO);
      flash = new FileM25P80(cpu, flashFile);
      ((USART) usart0).setUSARTListener(this);
      port4 = (IOPort) cpu.getIOUnit("Port 4");
      if (port4 != null && port4 instanceof IOPort) {
        (port4).setPortListener(this);
      }
    }
  }

  public void setupNode() {
    // create a filename for the flash file
    // This should be possible to take from a config file later!
    String fileName = config.getProperty("flashfile");
    if (fileName == null) {
      fileName = firmwareFile;
      int ix = fileName.lastIndexOf('.');
      if (ix > 0) {
        fileName = fileName.substring(0, ix);
      }
      fileName = fileName + ".flash";
    }
    System.out.println("Using flash file: " + fileName);

    this.flashFile = fileName;

    setupNodePorts();

    stats.addMonitor(this);
    stats.addMonitor(radio);
    stats.addMonitor(cpu);

//    cpu.scheduleCycleEvent(new TimeEvent(0) {
//      public void execute(long t) {
//        System.out.println("SkyNode: 1000000 cycles elapsed: " + t + "  " +
//            SkyNode.this.cpu.getTimeMillis());
//        // schedule at planned time + 1000000
//        SkyNode.this.cpu.scheduleCycleEvent(this, time + 1000000);
//      }
//    }, 1000000);

    
    network = new NetworkConnection();
    network.addPacketListener(new PacketListener() {
      public void transmissionEnded(int[] receivedData) {
        radio.setIncomingPacket(receivedData);
      }
      public void transmissionStarted() {
      }
    });
    // TODO: remove this test...
    radio.setPacketListener(new PacketListener() {
      public void transmissionEnded(int[] receivedData) {
        System.out.println(getName() + " got packet from radio " + SkyNode.this.cpu.getTimeMillis());
        network.dataSent(receivedData);
      }
      public void transmissionStarted() {
        System.out.println(getName() + " got indication on transmission from radio " + SkyNode.this.cpu.getTimeMillis());
      }
    });

    // UART0 TXreg = 0x77?
//    cpu.setBreakPoint(0x77, new CPUMonitor() {
//      public void cpuAction(int type, int adr, int data) {
//        System.out.println("Write to USART0 TX: " + data + " at " +
//            SkyNode.this.elf.getDebugInfo(SkyNode.this.cpu.readRegister(0)));
//      }
//    });

    if (!config.getPropertyAsBoolean("nogui", false)) {
      gui = new SkyGui(this);

      // A HACK for some "graphs"!!!
      DataChart dataChart =  new DataChart("Duty Cycle", "Duty Cycle");
      DataSourceSampler dss = dataChart.setupChipFrame(cpu);
      dataChart.addDataSource(dss, "LEDS", stats.getDataSource("Tmote Sky", 0, OperatingModeStatistics.OP_INVERT));
      dataChart.addDataSource(dss, "Listen", stats.getDataSource("CC2420", CC2420.MODE_RX_ON));
      dataChart.addDataSource(dss, "Transmit", stats.getDataSource("CC2420", CC2420.MODE_TXRX_ON));
      dataChart.addDataSource(dss, "CPU", stats.getDataSource("MSP430 Core", MSP430.MODE_ACTIVE));
    }
  }


  public static void main(String[] args) throws IOException {
    SkyNode node = new SkyNode();
    ArgumentManager config = new ArgumentManager();
    config.handleArguments(args);
    node.setup(config);
    node.start();
  }

}
