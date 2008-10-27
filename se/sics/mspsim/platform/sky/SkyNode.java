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
import se.sics.mspsim.chip.FileM25P80;
import se.sics.mspsim.chip.M25P80;
import se.sics.mspsim.chip.PacketListener;
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
 * Emulation of Sky Mote
 */
public class SkyNode extends MoteIVNode {

  public static final boolean DEBUG = false;

  public NetworkConnection network;

  private M25P80 flash;
  private String flashFile;

  /**
   * Creates a new <code>SkyNode</code> instance.
   *
   */
  public SkyNode() {
    setMode(MODE_LEDS_OFF);
  }

  public M25P80 getFlash() {
    return flash;
  }

  public void setFlash(M25P80 flash) {
    this.flash = flash;
    registry.registerComponent("xmem", flash);
  }

  // USART Listener
  public void dataReceived(USART source, int data) {
    radio.dataReceived(source, data);
    flash.dataReceived(source, data);
    /* if nothing selected, just write back a random byte to these devs */
    if (!radio.getChipSelect() && !flash.getChipSelect()) {
      source.byteReceived(0);
    }
  }

  @Override
  void flashWrite(IOPort source, int data) {
    flash.portWrite(source, data);
  }
  
  public String getName() {
    return "Tmote Sky";
  }

  public void setupNodePorts() {
    super.setupNodePorts();
    if (flashFile != null) {
      setFlash(new FileM25P80(cpu, flashFile));
    }
  }

  public void setupNode() {
    // create a filename for the flash file
    // This should be possible to take from a config file later!
    String fileName = config.getProperty("flashfile");
    if (fileName == null) {
      fileName = firmwareFile;
      if (fileName != null) {
        int ix = fileName.lastIndexOf('.');
        if (ix > 0) {
          fileName = fileName.substring(0, ix);
        }
        fileName = fileName + ".flash";
      }
    }
    System.out.println("Using flash file: " + (fileName == null ? "no file" : fileName));

    this.flashFile = fileName;

    setupNodePorts();

    stats.addMonitor(this);
    stats.addMonitor(radio);
    stats.addMonitor(cpu);

    if (config.getPropertyAsBoolean("enableNetwork", false)) {
      network = new NetworkConnection();
      final RadioWrapper radioWrapper = new RadioWrapper(radio);
      radioWrapper.setPacketListener(new PacketListener() {
        public void transmissionStarted() {
        }
        public void transmissionEnded(byte[] receivedData) {
          //        System.out.println("**** Sending data len = " + receivedData.length);
          //        for (int i = 0; i < receivedData.length; i++) {
          //          System.out.println("Byte: " + Utils.hex8(receivedData[i]));
          //        }
          network.dataSent(receivedData);
        }
      });

      network.addPacketListener(new PacketListener() {
        public void transmissionStarted() {
        }
        public void transmissionEnded(byte[] receivedData) {
          //        System.out.println("**** Receiving data = " + receivedData.length);
          radioWrapper.packetReceived(receivedData);
        }
      });
    }

    // UART0 TXreg = 0x77?
//    cpu.setBreakPoint(0x77, new CPUMonitor() {
//      public void cpuAction(int type, int adr, int data) {
//        System.out.println("Write to USART0 TX: " + data + " at " +
//            SkyNode.this.elf.getDebugInfo(SkyNode.this.cpu.readRegister(0)));
//      }
//    });

    if (!config.getPropertyAsBoolean("nogui", true)) {
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
    if (config.getProperty("nogui") == null) {
      config.setProperty("nogui", "false");
    }
    /* Ensure auto-run of a start script */
    if (config.getProperty("autorun") == null) {
      config.setProperty("autorun", "scripts/autorun.sc");
    }
    
    node.setupArgs(config);
  }

}
