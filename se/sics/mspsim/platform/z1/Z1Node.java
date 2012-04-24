package se.sics.mspsim.platform.z1;

import java.io.IOException;

import se.sics.mspsim.chip.CC2420;
import se.sics.mspsim.chip.FileM25P80;
import se.sics.mspsim.chip.M25P80;
import se.sics.mspsim.config.MSP430f2617Config;
import se.sics.mspsim.core.EmulationException;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.core.USARTSource;
import se.sics.mspsim.core.USCI;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.ui.SerialMon;
import se.sics.mspsim.util.ArgumentManager;

public class Z1Node extends GenericNode implements PortListener, USARTListener {

    /* P1.2 - Input: FIFOP from CC2420 */
    /* P1.3 - Input: FIFO from CC2420 */
    /* P1.4 - Input: CCA from CC2420 */
    public static final int CC2420_FIFOP = 2;
    public static final int CC2420_FIFO = 3;
    public static final int CC2420_CCA = 4;

    /* P4.1 - Input: SFD from CC2420 */
    /* P4.5 - Output: VREG_EN to CC2420 */
    /* P3.0 - Output: SPI Chip Select (CS_N) */
    public static final int CC2420_SFD = 1;
    public static final int CC2420_VREG = (1 << 5);
    public static final int CC2420_CHIP_SELECT = 0x01;


    IOPort port1;
    IOPort port3;
    IOPort port4;
    IOPort port5;

    public static final int LEDS_CONF_RED    = 0x10;
    public static final int LEDS_CONF_GREEN  = 0x40;
    public static final int LEDS_CONF_YELLOW = 0x20;

    private M25P80 flash;
    private String flashFile;
    public CC2420 radio;


    public Z1Node() {
        super("Z1", new MSP430f2617Config());
    }

    public M25P80 getFlash() {
        return flash;
    }

    public void setFlash(M25P80 flash) {
        this.flash = flash;
        registry.registerComponent("xmem", flash);
    }

    public void dataReceived(USARTSource source, int data) {
        // USCI s = (USCI) source;
        radio.dataReceived(source, data);
        flash.dataReceived(source, data);
        /* if nothing selected, just write back a random byte to these devs */
        if (!radio.getChipSelect() && !flash.getChipSelect()) {
            source.byteReceived(0);
        }
    }

    public void portWrite(IOPort source, int data) {
        if (source == port5) {
            System.out.println("LEDS GREEN = " + ((data & LEDS_CONF_GREEN) > 0));
            System.out.println("LEDS RED = " + ((data & LEDS_CONF_RED) > 0));
            System.out.println("LEDS YELLOW = " + ((data & LEDS_CONF_YELLOW) > 0));
        }
        if (source == port3) {
            // Chip select = active low...
            radio.setChipSelect((data & CC2420_CHIP_SELECT) == 0);
        }
        if (source == port4) {
            radio.setVRegOn((data & CC2420_VREG) != 0);
            //radio.portWrite(source, data);
            flash.portWrite(source, data);
        }
    }

    private void setupNodePorts() {
        if (flashFile != null) {
            setFlash(new FileM25P80(cpu, flashFile));
        }

        port1 = cpu.getIOUnit(IOPort.class, "P1");
        port1.addPortListener(this);
        port3 = cpu.getIOUnit(IOPort.class, "P3");
        port3.addPortListener(this);
        port4 = cpu.getIOUnit(IOPort.class, "P4");
        port4.addPortListener(this);
        port5 = cpu.getIOUnit(IOPort.class, "P5");
        port5.addPortListener(this);

        IOUnit usart0 = cpu.getIOUnit("USCI B0");
        if (usart0 instanceof USCI) {
            radio = new CC2420(cpu);
            radio.setCCAPort(port1, CC2420_CCA);
            radio.setFIFOPPort(port1, CC2420_FIFOP);
            radio.setFIFOPort(port1, CC2420_FIFO);

            ((USARTSource) usart0).addUSARTListener(this);
            radio.setSFDPort(port4, CC2420_SFD);
        } else {
            throw new EmulationException("Could not setup mote - missing USCI B0");
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

        if (!config.getPropertyAsBoolean("nogui", true)) {
            setupGUI();

            // Add some windows for listening to serial output
            IOUnit usart = cpu.getIOUnit("USCI A0");
            if (usart instanceof USARTSource) {
                SerialMon serial = new SerialMon((USARTSource)usart, "USART1 Port Output");
                registry.registerComponent("serialgui", serial);
            }
        }


    }

    public void setupGUI() {
        System.out.println("No gui for Z1 yet...");
    }

    public int getModeMax() {
        return 0;
    }

    public static void main(String[] args) throws IOException {
        Z1Node node = new Z1Node();
        ArgumentManager config = new ArgumentManager();
        config.handleArguments(args);
        node.setupArgs(config);
    }

}
