package se.sics.mspsim.net;

public interface UDPListener {
    public void packetReceived(IPv6Packet ip, UDPPacket upd);
}
