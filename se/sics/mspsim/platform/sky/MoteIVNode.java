package se.sics.mspsim.platform.sky;

import se.sics.mspsim.chip.CC2420;
import se.sics.mspsim.chip.DS2411;
import se.sics.mspsim.chip.PacketListener;
import se.sics.mspsim.chip.SHT11;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.USART;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.extutil.jfreechart.DataChart;
import se.sics.mspsim.extutil.jfreechart.DataSourceSampler;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.util.ELF;
import se.sics.mspsim.util.NetworkConnection;
import se.sics.mspsim.util.OperatingModeStatistics;

public abstract class MoteIVNode extends GenericNode implements PortListener, USARTListener {

  public static final int MODE_LEDS_OFF = 0;
  public static final int MODE_LEDS_1 = 1;
  public static final int MODE_LEDS_2 = 2;
  public static final int MODE_LEDS_3 = 3;
  public static final int MODE_MAX = MODE_LEDS_3;
  // Port 2.
  public static final int DS2411_DATA_PIN = 4;
  public static final int DS2411_DATA = 1 << DS2411_DATA_PIN;
  public static final int BUTTON_PIN = 7;

  /* P1.0 - Input: FIFOP from CC2420 */
  /* P1.3 - Input: FIFO from CC2420 */
  /* P1.4 - Input: CCA from CC2420 */
  public static final int CC2420_FIFOP = 0;
  public static final int CC2420_FIFO = 3;
  public static final int CC2420_CCA = 4;

  public static final int SHT11_CLK_PIN = 6;
  public static final int SHT11_DATA_PIN = 5;

  public static final int SHT11_CLK = 1 << SHT11_CLK_PIN;
  public static final int SHT11_DATA = 1 << SHT11_DATA_PIN;

  
  /* P4.1 - Input: SFD from CC2420 */
  /* P4.5 - Output: VREG_EN to CC2420 */
  /* P4.2 - Output: SPI Chip Select (CS_N) */
  public static final int CC2420_SFD = 1;
  public static final int CC2420_VREG = (1 << 5);
  public static final int CC2420_CHIP_SELECT = 0x04;
  
  public static final int BLUE_LED = 0x40;
  public static final int GREEN_LED = 0x20;
  public static final int RED_LED = 0x10;

  public boolean redLed;
  public boolean blueLed;
  public boolean greenLed;
  
  protected IOPort port1;
  protected IOPort port2;
  protected IOPort port4;
  protected IOPort port5;

  public CC2420 radio;
  public SHT11 sht11;
  public DS2411 ds2411;

  public SkyGui gui;
  public NetworkConnection network;

  protected String flashFile;

  public void setDebug(boolean debug) {
    cpu.setDebug(debug);
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

  public void setNodeID(int id) {
    ds2411.setMACID(id & 0xff, id & 0xff, id & 0xff, (id >> 8) & 0xff, id & 0xff, id & 0xff);
  }
  
  public void setupNodePorts() {
    sht11 = new SHT11(cpu);
    ds2411 = new DS2411(cpu);

    IOUnit unit = cpu.getIOUnit("Port 5");
    if (unit instanceof IOPort) {
      port5 = (IOPort) unit;
      port5.setPortListener(this);
    }

    unit = cpu.getIOUnit("Port 1");
    if (unit instanceof IOPort) {
      port1 = (IOPort) unit;
      port1.setPortListener(this);
      sht11.setDataPort(port1, SHT11_DATA_PIN);
    }

    unit = cpu.getIOUnit("Port 2");
    if (unit instanceof IOPort) {
      port2 = (IOPort) unit;
      ds2411.setDataPort(port2, DS2411_DATA_PIN);
      port2.setPortListener(this);
    }
    
    IOUnit usart0 = cpu.getIOUnit("USART 0");
    if (usart0 instanceof USART) {
      radio = new CC2420(cpu);
      radio.setCCAPort(port1, CC2420_CCA);
      radio.setFIFOPPort(port1, CC2420_FIFOP);
      radio.setFIFOPort(port1, CC2420_FIFO);
    
      ((USART) usart0).setUSARTListener(this);
      port4 = (IOPort) cpu.getIOUnit("Port 4");
      if (port4 != null) {
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
      if (fileName != null) {
        int ix = fileName.lastIndexOf('.');
        if (ix > 0) {
          fileName = fileName.substring(0, ix);
        }
        fileName = fileName + ".flash";
      }
    }
    if (DEBUG) System.out.println("Using flash file: " + (fileName == null ? "no file" : fileName));

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
      registry.registerComponent("nodegui", gui);

      // A HACK for some "graphs"!!!
      DataChart dataChart =  new DataChart(registry, "Duty Cycle", "Duty Cycle");
      registry.registerComponent("dutychart", dataChart);
      DataSourceSampler dss = dataChart.setupChipFrame(cpu);
      dataChart.addDataSource(dss, "LEDS", stats.getDataSource(getName(), 0, OperatingModeStatistics.OP_INVERT));
      dataChart.addDataSource(dss, "Listen", stats.getDataSource("CC2420", CC2420.MODE_RX_ON));
      dataChart.addDataSource(dss, "Transmit", stats.getDataSource("CC2420", CC2420.MODE_TXRX_ON));
      dataChart.addDataSource(dss, "CPU", stats.getDataSource("MSP430 Core", MSP430.MODE_ACTIVE));
    }
  }

  public void portWrite(IOPort source, int data) {
    if (source == port5) {
      redLed = (data & RED_LED) == 0;
      blueLed = (data & BLUE_LED) == 0;
      greenLed = (data & GREEN_LED) == 0;
      int newMode = (redLed ? 1 : 0) + (greenLed ? 1 : 0) + (blueLed ? 1 : 0);
      setMode(newMode);

      if (gui != null) {
        gui.repaint();
      }
    } else if (source == port4) {
      // Chip select = active low...
      radio.setChipSelect((data & CC2420_CHIP_SELECT) == 0);
      radio.setVRegOn((data & CC2420_VREG) != 0);
      //radio.portWrite(source, data);
      flashWrite(source, data);
    } else if (source == port1) {
      sht11.clockPin((data & SHT11_CLK) != 0);
      sht11.dataPin((data & SHT11_DATA) != 0);
    } else if (source == port2) {
      ds2411.dataPin((data & DS2411_DATA) != 0);
    }
  }
  
  public int getModeMax() {
    return MODE_MAX;
  }

  protected abstract void flashWrite(IOPort source, int data);

}
