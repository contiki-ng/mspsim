package se.sics.mspsim.net;

import java.io.PrintStream;

public class CC2420Packet extends AbstractPacket {

  public void printPacket(PrintStream out) {
    out.print("CC2420 | len:" + payload.length + " | ");
    for (int i = 0; i < payload.length; i++) {
      out.printf("%02x", payload[i] & 0xff);
      if ((i & 3) == 3) {
        out.print(" ");
      }
    }
    out.println();
    if (payloadPacket != null) {
      payloadPacket.printPacket(out);
    }
  }
}
