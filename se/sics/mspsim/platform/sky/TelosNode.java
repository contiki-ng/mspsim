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
 * $Id: SkyNode.java 304 2008-09-06 20:04:45Z joxe $
 *
 * -----------------------------------------------------------------
 *
 * SkyNode
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2008-09-06 22:04:45 +0200 (Sat, 06 Sep 2008) $
 *           $Revision: 304 $
 */

package se.sics.mspsim.platform.sky;
import java.io.IOException;

import se.sics.mspsim.chip.AT45DB;
import se.sics.mspsim.chip.CC2420;
import se.sics.mspsim.chip.FileAT45DB;
import se.sics.mspsim.chip.PacketListener;
import se.sics.mspsim.chip.RFListener;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.USART;
import se.sics.mspsim.extutil.jfreechart.DataChart;
import se.sics.mspsim.extutil.jfreechart.DataSourceSampler;
import se.sics.mspsim.util.ArgumentManager;
import se.sics.mspsim.util.NetworkConnection;
import se.sics.mspsim.util.OperatingModeStatistics;

/**
 * Emulation of Telos Mote (old version of Sky Node)
 * 
 * TODO: Cleanup the MoteIVNode, SkyNode and TelosNode
 */
public class TelosNode extends MoteIVNode {
  public static final boolean DEBUG = false;

  // P4.4 - Output: SPI Flash Chip Select
  public static final int FLASH_RESET = (1<<3);
  public static final int FLASH_CS = (1<<4);
  
  public NetworkConnection network;


  private AT45DB flash;
  private String flashFile;

  /**
   * Creates a new <code>SkyNode</code> instance.
   *
   */
  public TelosNode() {
    setMode(MODE_LEDS_OFF);
  }

  public AT45DB getFlash() {
    return flash;
  }

  public void setFlash(AT45DB flash) {
    this.flash = flash;
  }

  @Override
  void flashWrite(IOPort source, int data) {
    flash.setReset((data & FLASH_RESET) == 0);
    flash.setChipSelect((data & FLASH_CS) == 0);
  }

  // USART Listener
  public void dataReceived(USART source, int data) {
    radio.dataReceived(source, data);
    flash.dataReceived(source, data);
  }

  public String getName() {
    return "Telos";
  }

  public void setupNodePorts(boolean loadFlash) {
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
      if (loadFlash) {
        flash = new FileAT45DB(cpu, flashFile);
      }
      ((USART) usart0).setUSARTListener(this);
      port4 = (IOPort) cpu.getIOUnit("Port 4");
      if (port4 != null && port4 instanceof IOPort) {
        port4.setPortListener(this);
        radio.setSFDPort(port4, CC2420_SFD);
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

    setupNodePorts(true);

    stats.addMonitor(this);
    stats.addMonitor(radio);
    stats.addMonitor(cpu);

    network = new NetworkConnection();
    network.addPacketListener(new PacketListener() {
      public void transmissionEnded(byte[] receivedData) {
        radio.setIncomingPacket(receivedData);
      }
      public void transmissionStarted() {
      }
    });
    // TODO: remove this test...
    radio.setRFListener(new RFListener() {
      int len = 0;
      int pos = 0;
      byte[] buffer = new byte[128];
      // NOTE: len is not in the packet for now...
      public void receivedByte(byte data) {
//        System.out.println("*** RF Data :" + data);
        if (pos == 5) {
//          System.out.println("**** Setting length to:" + data);
          len = data;
        }
        buffer[pos++] = data;
        // len + 1 = pos + 5 (preambles)
        if (len > 0 && len + 1 == pos - 5) {
//          System.out.println("***** SENDING DATA!!!");
          byte[] packet = new byte[len];
          System.arraycopy(buffer, 5, packet, 0, len);
          network.dataSent(packet);
          pos = 0;
          len = 0;
        }
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
      dataChart.addDataSource(dss, "LEDS", stats.getDataSource("Telos", 0, OperatingModeStatistics.OP_INVERT));
      dataChart.addDataSource(dss, "Listen", stats.getDataSource("CC2420", CC2420.MODE_RX_ON));
      dataChart.addDataSource(dss, "Transmit", stats.getDataSource("CC2420", CC2420.MODE_TXRX_ON));
      dataChart.addDataSource(dss, "CPU", stats.getDataSource("MSP430 Core", MSP430.MODE_ACTIVE));
    }
  }


  public static void main(String[] args) throws IOException {
    TelosNode node = new TelosNode();
    ArgumentManager config = new ArgumentManager();
    config.handleArguments(args);
    node.setup(config);
    node.start();
  }
}
