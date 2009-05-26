package se.sics.mspsim.net;

/* IP network interface */
public interface NetworkInterface {
  public String getName();
  public void sendPacket(IPv6Packet packet);
  public void setIPStack(IPStack stack);
  public boolean isReady();
}
