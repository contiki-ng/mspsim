package se.sics.mspsim.net;

public class NeighborManager implements Runnable {

    private NeighborTable neigborTable;
    private IPStack ipStack;
    private long nextRS = 0;
    private boolean duplicateDetectionNS;

    public NeighborManager(IPStack stack, NeighborTable table) {
      neigborTable = table;
      ipStack = stack;
      new Thread(this).start();
    }

    public void run() {
      while(true) {
        try {
          Thread.sleep(1000);
          long time = System.currentTimeMillis();
          if (!duplicateDetectionNS) {
            /* send a duplicate detection message */
            System.out.println("NeighborManager: sending neighbor solicitation (DAD)");
            duplicateDetectionNS = true;
            ICMP6Packet icmp = new ICMP6Packet();
            icmp.setType(ICMP6Packet.NEIGHBOR_SOLICITATION);
            icmp.targetAddress = ipStack.myLinkAddress;
            IPv6Packet ipp = new IPv6Packet(icmp);
            ipp.setDestinationAddress(ipStack.myLocalSolicited);
            ipp.setSourceAddress(ipStack.myLocalIPAddress);
            ipStack.sendPacket(ipp, null);
          } else if (!ipStack.isRouter() && neigborTable.getDefrouter() == null && nextRS < time) {
            System.out.println("NeighborManager: sending router solicitation");
            nextRS = time + 10000;
            ICMP6Packet icmp = new ICMP6Packet();
            icmp.setType(ICMP6Packet.ROUTER_SOLICITATION);
            icmp.addLinkOption(ICMP6Packet.SOURCE_LINKADDR,
                ipStack.getLinkLayerAddress());
            IPv6Packet ipp = new IPv6Packet(icmp);
            ipp.setDestinationAddress(IPStack.ALL_ROUTERS);
            ipp.setSourceAddress(ipStack.myLocalIPAddress);
            ipStack.sendPacket(ipp, null);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    
    public void receiveNDMessage(IPv6Packet packet) {
      /* payload is a ICMP6 packet */
      ICMP6Packet payload = (ICMP6Packet) packet.getIPPayload();
      Neighbor nei = null;
      switch (payload.type) {
      case ICMP6Packet.ROUTER_SOLICITATION:
        nei = neigborTable.addNeighbor(packet.sourceAddress, packet.getLinkSource());
        if (nei != null) {
          nei.setState(Neighbor.REACHABLE);
        }
        break;
      case ICMP6Packet.ROUTER_ADVERTISEMENT:
        nei = neigborTable.addNeighbor(packet.sourceAddress, packet.getLinkSource());
        neigborTable.setDefrouter(nei);
        nei.setState(Neighbor.REACHABLE);
        break;
      }

    }
}
