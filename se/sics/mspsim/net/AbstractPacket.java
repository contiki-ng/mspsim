package se.sics.mspsim.net;

import java.io.PrintStream;
import java.util.ArrayList;

public abstract class AbstractPacket implements Packet {

  ArrayList<AbstractPacket> packetHandlers = new ArrayList<AbstractPacket>();
  
  public void addInnerPacketHandler(AbstractPacket packet) {
    packetHandlers.add(packet);
  }

  public void notifyPacketHandlers(byte[] payload, int len) {    
    for (int i = 0; i < packetHandlers.size(); i++) {
      try {
        packetHandlers.get(i).setPacketData(payload, len);        
      } catch (Exception e) {
      }
    }
  }
  
  public void printPacketStack(PrintStream out) {
    printPacket(out);
    for (int i = 0; i < packetHandlers.size(); i++) {
      /* only the valid packets should print anything... */
      packetHandlers.get(i).printPacketStack(out);
    }
  }
}
