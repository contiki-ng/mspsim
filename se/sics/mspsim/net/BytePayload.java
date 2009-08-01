package se.sics.mspsim.net;

import java.io.PrintStream;

/* keep the packet payload untouched ... */
public class BytePayload implements IPPayload {

  byte[] payloadData;
  byte dispatch;
  
  public BytePayload(IPv6Packet packet) {
    parsePacketData(packet);
  }
  
  public byte[] generatePacketData(IPv6Packet packet) {
    return payloadData;
  }

  public byte getDispatch() {
    return dispatch;
  }

  public void parsePacketData(IPv6Packet packet) {
    dispatch = packet.getDispatch();
    payloadData = packet.getPayload();
    int len = 0;
    if (payloadData != null) len = payloadData.length;
    System.out.println("BytePayload: got " + len + " bytes data");
  }

  public void printPacket(PrintStream out) {
  }
}
