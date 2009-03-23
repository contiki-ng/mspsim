package se.sics.mspsim.net;

public class ICMP6PacketHandler extends AbstractPacketHandler {

  public void packetReceived(Packet container) {
    ICMP6Packet icmpPacket = new ICMP6Packet();
    container.setPayloadPacket(icmpPacket);
    icmpPacket.containerPacket = container;
    icmpPacket.setPacketData(container, container.getPayload(),
        container.getPayload().length);
  }

  public void sendPacket(Packet payload) {
  }

}
