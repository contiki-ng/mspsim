package se.sics.mspsim.net;

import java.io.PrintStream;

import se.sics.mspsim.util.Utils;

public class UDPPacket implements IPPayload {

  public final static int DISPATCH = 17;

  int sourcePort;
  int destinationPort;
  int length;
  int checkSum;
  byte[] payload;
  
  public UDPPacket replyPacket() {
    UDPPacket udp = new UDPPacket();
    udp.destinationPort = sourcePort;
    udp.sourcePort = destinationPort;
    return udp;
  }

  public byte[] getPayload() {
    return payload;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public int getSourcePort() {
    return sourcePort;
  }

  public void setSourcePort(int sourcePort) {
    this.sourcePort = sourcePort;
  }

  public int getDestinationPort() {
    return destinationPort;
  }

  public void setDestinationPort(int destinationPort) {
    this.destinationPort = destinationPort;
  }
  
  /* generate RAW UDP packet */
  public byte[] generatePacketData(IPv6Packet packet) {
      int size = payload != null ? payload.length : 0;
      size += 8;
      byte[] data = new byte[size];
      int pos = 0;
      data[pos++] = (byte)(sourcePort >> 8);
      data[pos++] = (byte)(sourcePort & 0xff);
      data[pos++] = (byte)(destinationPort >> 8);
      data[pos++] = (byte)(destinationPort & 0xff);
      data[pos++] = (byte)(size >> 8);
      data[pos++] = (byte)(size & 0xff);
      data[pos++] = 0;
      data[pos++] = 0;

      if (payload != null) {
          System.arraycopy(payload, 0, data, pos, payload.length);
      }
      
      packet.payloadLen = size;
      int sum = packet.upperLayerHeaderChecksum();
      
      sum = IPv6Packet.checkSum(sum, data, size);
      sum = (~sum) & 0xffff;
      data[6] = (byte) (sum >> 8);
      data[7] = (byte) (sum & 0xff);

      return data;
  }

  public byte getDispatch() {
    return DISPATCH;
  }

  public void printPacket(PrintStream out) {
    out.println("UDP Packet srcPort: " + sourcePort +
        " destPort: " + destinationPort);
    out.println("UDP length: " + length);
  }
  
  public void parsePacketData(IPv6Packet packet) {
    sourcePort = packet.get16(0);
    destinationPort = packet.get16(2);
    int length = packet.get16(4);
    checkSum = packet.get16(6);

    System.out.println("UDP Length: " + length);
    System.out.println("UDP payload length: " + packet.getPayloadLength());
    /* this will *crash* if packet does not contain all data */
    payload = new byte[length - 8];
    /* length is total UDP length */
    packet.copy(8, payload, 0, length - 8);

    /* checksum */
    packet.setData(2, (byte) 0);
    packet.setData(3, (byte) 0);
    byte[] data = packet.getPayload();
    int sum = packet.upperLayerHeaderChecksum();
    sum = IPv6Packet.checkSum(sum, data, data.length);
    sum = (~sum) & 0xffff;
    if (sum == checkSum) {
      System.out.println("UDP: Checksum matches!!!");
    } else {
      System.out.println("UDP: Checksum error: " + 
          Utils.hex16(checkSum) + " <?> " + Utils.hex16(sum));
    }
  }

  // TODO: HC01 should instead insert this data into the UDP packet so
  // that there is no need for special handling-
  public int doVirtualChecksum(IPv6Packet packet) {
    byte[] vheader = new byte[8];
    int length = payload.length + 8;
    vheader[0] = (byte) (sourcePort >> 8);
    vheader[1] = (byte) (sourcePort & 0xff);
    vheader[2] = (byte) (destinationPort >> 8);
    vheader[3] = (byte) (destinationPort & 0xff);
    vheader[4] = (byte) (length >> 8);
    vheader[5] = (byte) (length & 0xff);

    packet.payloadLen = length;
    int sum = packet.upperLayerHeaderChecksum();
    
    sum = IPv6Packet.checkSum(sum, vheader, 8);
    sum = IPv6Packet.checkSum(sum, payload, payload.length);
    sum = (~sum) & 0xffff;
    if (sum == checkSum) {
      System.out.println("UDP: Checksum matches!!!");
    } else {
      System.out.println("UDP: Checksum error: " + 
          Utils.hex16(checkSum) + " <?> " + Utils.hex16(sum));
    }
    return sum;
  }
}
