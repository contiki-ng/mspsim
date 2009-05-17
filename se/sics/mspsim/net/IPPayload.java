package se.sics.mspsim.net;

public interface IPPayload {

  public byte getDispatch();
  
  /* Call this when payload needs to be externalized
   */
  public byte[] generatePacketData(IPv6Packet packet);
  
  /* 
   * Call this to parse the IP packet 
   */
  public void parsePacketData(IPv6Packet packet); 
}

