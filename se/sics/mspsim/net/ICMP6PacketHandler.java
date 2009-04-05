package se.sics.mspsim.net;

public class ICMP6PacketHandler extends AbstractPacketHandler {

  public void packetReceived(Packet container) {
    ICMP6Packet icmpPacket = new ICMP6Packet();
    IPv6Packet ipv6 = (IPv6Packet) container;
    container.setPayloadPacket(icmpPacket);
    icmpPacket.containerPacket = container;
    icmpPacket.setPacketData(container, container.getPayload(),
        container.getPayload().length);

    /* handle packet - just a test for now */
    switch (icmpPacket.type) {
    case ICMP6Packet.NEIGHBOR_SOLICITATION:
      ICMP6Packet p = new ICMP6Packet();
      p.targetAddress = icmpPacket.targetAddress;
      p.type = ICMP6Packet.NEIGHBOR_ADVERTISEMENT;
      p.flags = ICMP6Packet.FLAG_SOLICITED |
        ICMP6Packet.FLAG_OVERRIDE;

      IPv6Packet ipp = new IPv6Packet();
      ipp.version = 6;
      ipp.flowLabel = 0;
      ipp.setPayloadPacket(p);
      ipp.nextHeader = IPv6Packet.ICMP6_DISPATCH;
      // ipp.destAddress = ???;
      break;
    }
  }

    public void sendPacket(Packet payload) {
    /* ICMP does not carry payload ?? */
  }
  
}
