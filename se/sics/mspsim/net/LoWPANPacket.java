package se.sics.mspsim.net;

import java.io.PrintStream;

public class LoWPANPacket extends AbstractPacket {

  int dispatch = 0;

  public LoWPANPacket() {
  }
  
  public LoWPANPacket(byte[] data) {
    dispatch = data[0];
    setPayload(data, 1, data.length - 1);
  }
  
  public byte[] getSourceAddress() {
    return containerPacket.getSourceAddress();
  }

  public byte[] getDestinationAddress() {
    return containerPacket.getDestinationAddress();
  }
  
  public void printPacket(PrintStream out) {
    out.println("6LoWPAN Dispatch: " + dispatch);
    if (payloadPacket != null) {
      payloadPacket.printPacket(out);
    }
  }
}
