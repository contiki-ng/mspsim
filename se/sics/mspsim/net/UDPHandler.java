package se.sics.mspsim.net;

import java.io.IOException;

public class UDPHandler {
    private static final int MAX_LISTENERS = 16;
    public UDPListener[] listeners = new UDPListener[MAX_LISTENERS];
    public int[] listenPorts = new int[MAX_LISTENERS];    
    int noListeners = 0;
    
    public void addUDPListener(UDPListener listener, int port) throws IOException {
	if (noListeners < MAX_LISTENERS) {
	    System.out.println("UDPHandler: adding listener for " + port);
	    listeners[noListeners] = listener;
	    listenPorts[noListeners++] = port;
	} else {
	    throw new IOException("Too many open connections...");
	}
    }

    public void handlePacket(IPv6Packet packet, UDPPacket udpPacket) {
	for (int i = 0; i < noListeners; i++) {
	    if (listenPorts[i] == udpPacket.destinationPort) {
		listeners[i].packetReceived(packet, udpPacket);
		/* only one connection per port... */
		return;
	    }
	}
    }
}
