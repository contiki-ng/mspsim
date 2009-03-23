package se.sics.mspsim.net;

public interface PacketHandler {

  public void addUpperLayerHandler(int protoID,PacketHandler handler);
  public void setLowerLayerHandler(PacketHandler handler);
  public void packetReceived(Packet container);
  public void sendPacket(Packet payload);
  
}
