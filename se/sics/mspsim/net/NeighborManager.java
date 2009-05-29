package se.sics.mspsim.net;

public class NeighborManager implements Runnable {

    private NeighborTable neigborTable;
    private IPStack ipStack;

    public NeighborManager(IPStack stack, NeighborTable table) {
      neigborTable = table;
      ipStack = stack;
      new Thread(this).start();
    }

    public void run() {
      while(true) {
        try {
          Thread.sleep(1000);
          if (neigborTable.getDefrouter() == null) {
            IPv6Packet ipp = new IPv6Packet();
            ipp.setDestinationAddress(IPStack.ALL_ROUTERS);
            ICMP6Packet icmp = new ICMP6Packet();
            icmp.setType(ICMP6Packet.ROUTER_SOLICITATION);
            icmp.addLinkOption(ICMP6Packet.SOURCE_LINKADDR,
                ipStack.getLinkLayerAddress());
            ipStack.sendPacket(ipp, null);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
}
