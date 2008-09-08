package se.sics.mspsim.platform.sky;

import se.sics.mspsim.chip.CC2420;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.util.ELF;

public abstract class MoteIVNode extends GenericNode implements PortListener, USARTListener {

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
  

  public SkyGui gui;

  public void setDebug(boolean debug) {
    cpu.setDebug(debug);
  }

  public MSP430 getCPU() {
    return cpu;
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
    }
  }
  
  public int getModeMax() {
    return MODE_MAX;
  }
  
  abstract void flashWrite(IOPort source, int data);
  
}
