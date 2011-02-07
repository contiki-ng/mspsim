package se.sics.mspsim.platform.z1;

import java.io.IOException;

import se.sics.mspsim.config.MSP430f2617Config;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.platform.esb.ESBNode;
import se.sics.mspsim.util.ArgumentManager;

public class Z1Node extends GenericNode implements PortListener {

    IOPort port1;

    public Z1Node() {
        super("Z1", new MSP430f2617Config());
    }

    public void portWrite(IOPort source, int data) {
        System.out.println("Write to port: " + source + " => " + data);
    }

    private void setupNodePorts() {
        IOUnit unit = cpu.getIOUnit("P1");
        if (unit instanceof IOPort) {
            port1 = (IOPort) unit;
            port1.setPortListener(this);
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
