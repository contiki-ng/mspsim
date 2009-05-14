package se.sics.mspsim.net;

/* creates and parser IP packets */
public interface IPPacketer {

  public byte getDispatch();

  
  /* before calling this method a call to route is needed to get
   * the link layer addresses into the IPv6Packet
   */
  public byte[] generatePacketData(IPv6Packet packet);

  
  /* before calling this method the IPv6Packet needs to have its
   * link layer addresses added from the link layer
   */
  public void parsePacketData(IPv6Packet packet);
  
}
