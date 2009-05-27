package se.sics.mspsim.net;

public interface NetworkEventListener {

  public void packetHandled(IPv6Packet packet);
  
}
