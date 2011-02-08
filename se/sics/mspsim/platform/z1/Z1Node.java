package se.sics.mspsim.platform.z1;

import java.io.IOException;

import se.sics.mspsim.config.MSP430f2617Config;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.util.ArgumentManager;

public class Z1Node extends GenericNode implements PortListener {

    IOPort port1;
    IOPort port5;
    
    public static final int LEDS_CONF_RED    = 0x10;
    public static final int LEDS_CONF_GREEN  = 0x40;
    public static final int LEDS_CONF_YELLOW = 0x20;


    public Z1Node() {
        super("Z1", new MSP430f2617Config());
    }

    public void portWrite(IOPort source, int data) {
        System.out.println("Write to port: " + source + " => " + data);
        if (source == port5) {
            System.out.println("LEDS GREEN = " + ((data & LEDS_CONF_GREEN) > 0));
            System.out.println("LEDS RED = " + ((data & LEDS_CONF_RED) > 0));
            System.out.println("LEDS YELLOW = " + ((data & LEDS_CONF_YELLOW) > 0));
        }
    }

    private void setupNodePorts() {
        IOUnit unit = cpu.getIOUnit("P1");
        if (unit instanceof IOPort) {
            port1 = (IOPort) unit;
            port1.setPortListener(this);
        }
        unit = cpu.getIOUnit("P5");
        if (unit instanceof IOPort) {
            port5 = (IOPort) unit;
            port5.setPortListener(this);
        }
    }

    public void setupNode() {
        setupNodePorts();
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
