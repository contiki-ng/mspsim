package se.sics.mspsim.net;

public class ICMP6PacketHandler {

  IPStack ipStack;
  
  public ICMP6PacketHandler(IPStack stack) {
    ipStack = stack;
  }
  
  public void handlePacket(IPv6Packet packet) {
    ICMP6Packet icmpPacket = new ICMP6Packet();
    icmpPacket.parsePacketData(packet);

    /* handle packet - just a test for now */
    switch (icmpPacket.type) {
    case ICMP6Packet.NEIGHBOR_SOLICITATION:
      ICMP6Packet p = new ICMP6Packet();
      p.targetAddress = icmpPacket.targetAddress;
      p.type = ICMP6Packet.NEIGHBOR_ADVERTISEMENT;
      p.flags = ICMP6Packet.FLAG_SOLICITED |
        ICMP6Packet.FLAG_OVERRIDE;

      IPv6Packet ipp = new IPv6Packet();
      ipp.nextHeader = IPv6Packet.ICMP6_DISPATCH;
      // is this ok?
      ipp.destAddress = packet.sourceAddress;
      ipStack.sendPacket(ipp);
      break;
    }
  }
}
