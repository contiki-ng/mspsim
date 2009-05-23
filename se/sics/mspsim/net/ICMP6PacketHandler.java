package se.sics.mspsim.net;

public class ICMP6PacketHandler {

  IPStack ipStack;

  public ICMP6PacketHandler(IPStack stack) {
    ipStack = stack;
  }

  public void handlePacket(IPv6Packet packet) {
    ICMP6Packet icmpPacket = new ICMP6Packet();
    icmpPacket.parsePacketData(packet);

    icmpPacket.printPacket(System.out);

    /* handle packet - just a test for now */
    ICMP6Packet p;
    IPv6Packet ipp;
    switch (icmpPacket.type) {
    case ICMP6Packet.NEIGHBOR_SOLICITATION:
      p = new ICMP6Packet();
      p.targetAddress = icmpPacket.targetAddress;
      p.type = ICMP6Packet.NEIGHBOR_ADVERTISEMENT;      
      p.flags = ICMP6Packet.FLAG_SOLICITED |
      ICMP6Packet.FLAG_OVERRIDE;
      if (ipStack.isRouter()) {
        p.flags |= ICMP6Packet.FLAG_ROUTER;
      }
        /* always send the linkaddr option */
      p.addLinkOption(ICMP6Packet.TARGET_LINKADDR, ipStack.getLinkLayerAddress());
      ipp = new IPv6Packet();
      ipp.setIPPayload(p);
      // is this ok?
      ipp.destAddress = packet.sourceAddress;
      ipStack.sendPacket(ipp);
      break;
    case ICMP6Packet.ROUTER_SOLICITATION:
      p = new ICMP6Packet();
      p.targetAddress = icmpPacket.targetAddress;
      p.type = ICMP6Packet.ROUTER_ADVERTISEMENT;
      p.flags = ICMP6Packet.FLAG_SOLICITED |
        ICMP6Packet.FLAG_OVERRIDE;

      /* ensure that the RA is updated... */
      p.updateRA(ipStack);
      
      ipp = new IPv6Packet();
      ipp.setIPPayload(p);
      // is this ok?
      //ipp.destAddress = packet.sourceAddress;
      ipp.destAddress = IPStack.ALL_NODES; //packet.sourceAddress;
      ipp.sourceAddress = ipStack.myLocalIPAddress;
      System.out.print("Created ICMP6 RA for ");
      IPv6Packet.printAddress(System.out, ipp.destAddress);
      
      ipStack.sendPacket(ipp);
      break;
    }
  }
}
