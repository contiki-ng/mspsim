/**
 * 
 */
package se.sics.mspsim.net;

import java.io.PrintStream;

import se.sics.mspsim.util.Utils;

/**
 * @author joakim
 *
 */
public class TCPPacket implements IPPayload {

  public static final int DEFAULT_WINDOW = 60;
  public static final int DEFAULT_MSS = 60;
  
  public static final int DISPATCH = 6;
  
  public static final int OPT_MSS = 2;
  public static final int OPT_MSS_LEN = 4;
  
  public static final int URG = 0x20;
  public static final int ACK = 0x10;
  public static final int PSH = 0x08;
  public static final int RST = 0x04;
  public static final int SYN = 0x02;
  public static final int FIN = 0x01;
  
  int sourcePort;
  int destinationPort;
  int seqNo;
  int ackNo;
  int offset = 5; /* no options, no padding => 5 x 4 = 20 bytes header */
  int flags;
  int window = DEFAULT_WINDOW;
  int checksum;
  int urgentPointer;
  
  byte[] payload;
  
  /* (non-Javadoc)
   * @see se.sics.mspsim.net.IPPayload#generatePacketData(se.sics.mspsim.net.IPv6Packet)
   */
  public byte[] generatePacketData(IPv6Packet packet) {
    int size = 20;
    if ((flags & SYN) > 0) {
      size += 4;
    }
    /* offset to the first payload byte div. by 4 */
    offset = size / 4;

    if (payload != null) {
      size += payload.length;
    }
    byte[] data = new byte[size];
    int pos = 0;
    data[pos++] = (byte)(sourcePort >> 8);
    data[pos++] = (byte)(sourcePort & 0xff);
    data[pos++] = (byte)(destinationPort >> 8);
    data[pos++] = (byte)(destinationPort & 0xff);
    IPv6Packet.set32(data, pos, seqNo);
    pos += 4;
    IPv6Packet.set32(data, pos, ackNo);
    pos += 4;
    data[pos++] = (byte)(offset << 4);
    data[pos++] = (byte) flags;

    data[pos++] = (byte)(window >> 8);
    data[pos++] = (byte)(window & 0xff);

    data[pos++] = 0; /* checksum */
    data[pos++] = 0;

    data[pos++] = (byte)(urgentPointer >> 8);
    data[pos++] = (byte)(urgentPointer & 0xff);

    /* if this is a syn then send MSS */
    if ((flags & SYN) > 0) {
      data[pos++] = OPT_MSS;
      data[pos++] = OPT_MSS_LEN;
      data[pos++] = (byte)(DEFAULT_MSS >> 8);
      data[pos++] = (byte)(DEFAULT_MSS & 0xff);
    }
    /* no options, no padding for now */
    if (payload != null) {
//      System.out.println("Adding payload to packet!!!" + payload.length);
      System.arraycopy(payload, 0, data, pos, payload.length);
    }

    packet.payloadLen = size;
    int sum = packet.upperLayerHeaderChecksum();
    
    sum = IPv6Packet.checkSum(sum, data, size);
    sum = (~sum) & 0xffff;
    data[16] = (byte) (sum >> 8);
    data[17] = (byte) (sum & 0xff);

    return data;
  }

  public boolean isAck() {
    return (flags & ACK) > 0;
  }

  public boolean isPush() {
    return (flags & PSH) > 0;
  }
  
  public boolean isSyn() {
    return (flags & SYN) > 0;
  }

  public boolean isFin() {
    return (flags & FIN) > 0;
  }
  
  public boolean isReset() {
    return (flags & RST) > 0;
  }
  
  /* (non-Javadoc)
   * @see se.sics.mspsim.net.IPPayload#getDispatch()
   */
  public byte getDispatch() {
    return DISPATCH;
  }

  public byte[] getPayload() {
    return payload;
  }
  /* (non-Javadoc)
   * @see se.sics.mspsim.net.IPPayload#parsePacketData(se.sics.mspsim.net.IPv6Packet)
   */
  public void parsePacketData(IPv6Packet packet) {
    sourcePort = packet.get16(0);
    destinationPort = packet.get16(2);
    seqNo = packet.get32(4);
    ackNo = packet.get32(8);
    offset = (packet.getData(12) & 0xff) >> 4;
    flags = packet.getData(13);
    window = packet.get16(14);
    checksum = packet.get16(16);
    urgentPointer = packet.get16(18);

    /* checksum */
    packet.setData(16, (byte) 0);
    packet.setData(17, (byte) 0);
    byte[] data = packet.getPayload();
    int sum = packet.upperLayerHeaderChecksum();
    sum = IPv6Packet.checkSum(sum, data, data.length);
    sum = (~sum) & 0xffff;
    if (sum == checksum) {
      // System.out.println("TCP: Checksum matches!!!");
    } else {
      System.out.println("TCP: Checksum error: " + 
          Utils.hex16(checksum) + " <?> " + Utils.hex16(sum));
    }
    if (data.length - (offset * 4) > 0) {
      int len = data.length - (offset * 4);
      payload = new byte[len];
      System.arraycopy(data, (offset * 4), payload, 0, len);
    }
  }

  /* (non-Javadoc)
   * @see se.sics.mspsim.net.IPPayload#printPacket(java.io.PrintStream)
   */
  public void printPacket(PrintStream out) {
    out.print("[TCP " + sourcePort +
          " -> " + destinationPort + " Flag: " + Utils.hex8(flags) +
          " seq: " + Long.toString(seqNo & 0xffffL, 16) +
          " ack:" + Long.toString(ackNo &  0xffffL, 16));
    if (payload != null) {
      System.out.print("|");
      int len = 8;
      if (payload.length < len) len = payload.length;
      for (int i = 0; i < len; i++) {
        out.print((char) payload[i]);
      }
    }
    System.out.println("]");
  }

  public TCPPacket replyPacket() {
    TCPPacket packet = new TCPPacket();
    packet.sourcePort = destinationPort;
    packet.destinationPort = sourcePort;    
    return packet;
  }
}