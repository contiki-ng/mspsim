package se.sics.mspsim.net;

import java.io.PrintStream;

public class ICMP6Packet implements IPPayload {

  public static final int DISPATCH = 58;
  
  public static final int ECHO_REQUEST = 128;
  public static final int ECHO_REPLY = 129;
  public static final int GROUP_QUERY = 130;
  public static final int GROUP_REPORT = 131;
  public static final int GROUP_REDUCTION = 132;
  public static final int ROUTER_SOLICITATION = 133;
  public static final int ROUTER_ADVERTISEMENT = 134;
  public static final int NEIGHBOR_SOLICITATION = 135;
  public static final int NEIGHBOR_ADVERTISEMENT = 136;

  public static final int FLAG_ROUTER = 0x80;
  public static final int FLAG_SOLICITED = 0x40;
  public static final int FLAG_OVERRIDE = 0x20;

  public static final int ON_LINK = 0x80;
  public static final int AUTOCONFIG = 0x40;
  
  public static final String[] TYPE_NAME = new String[] {
    "ECHO_REQUEST", "ECHO_REPLY",
    "GROUP_QUERY", "GROUP_REPORT", "GROUP_REDUCTION",
    "ROUTER_SOLICITATION", "ROUTER_ADVERTISEMENT", 
    "NEIGHBOR_SOLICITATION", "NEIGHBOR_ADVERTISEMENT"};

  int type;
  int code;
  int checksum;
  byte[] targetAddress = new byte[16];

  int id;
  int seqNo;

  int flags;

  byte hopLimit;
  byte autoConfigFlags;
  int routerLifetime = 600; /* time in seconds for keeping the router as default */ 
  int reachableTime = 10000; /* time in millis when node still should be counted as reachable */
  int retransmissionTimer = 1000; /* time in millis between solicitations */

  /* source link layer option - type = 1, len = 1 (64 bits) */
  byte[] srcLinkOptionShort = new byte[] {1, 1, 0, 0, 0, 0, 0, 0};
  byte[] srcLinkOptionLong = new byte[] {1, 2, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0};  
  /* prefix info option - type = 3, len = 4 (64x4 bits), prefix = 64 bits */
  byte[] prefixInfo = new byte[] {3, 4, 64, (byte) (ON_LINK | AUTOCONFIG),
        0, 0, 1, 0, /* valid lifetime - 256 seconds for now*/
        0, 1, 0, 0, /* prefered lifetime - 65535 seconds lifetime of autoconf addr */
        0, 0, 0, 0, /* reserved */
        /* the prefix ... */
        (byte)0xaa, (byte)0xaa, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0
  };
  /* default MTU is 1280 (5x256) which also is the smallest allowed */
  byte[] mtuOption = new byte[] {5, 1, 0, 0, 0, 0, 5, 0};
  
  public void printPacket(PrintStream out) {
    String typeS = "" + type;
    if (type >= 128) {
      int tS = type - 128;
      if (tS < TYPE_NAME.length) {
        typeS = TYPE_NAME[tS];
      }
    }
    out.printf("ICMPv6 Type: %d (%s) Code: %d id: %04x seq: %04x\n", type, typeS,
        code, id, seqNo);
    if (targetAddress != null) {
      out.print("ICMPv6 Target address: ");
      IPv6Packet.printAddress(out, targetAddress);
      out.println();
    }
    /* ICMP can not have payload ?! */  
  }
  
  public void parsePacketData(IPv6Packet packet) {
    if (packet.nextHeader == 58) {
      type = packet.getData(0) & 0xff;
      code = packet.getData(1) & 0xff;
      checksum = ((packet.getData(2) & 0xff) << 8) | packet.getData(3) & 0xff;
      /* test the checksum ... - set checksum to zero*/
      packet.setData(2, (byte) 0);
      packet.setData(3, (byte) 0);

      switch (type) {
      case ECHO_REQUEST:
      case ECHO_REPLY:
        id = packet.get16(4);
        seqNo = packet.get16(6);
        break;
      case NEIGHBOR_SOLICITATION:
      case NEIGHBOR_ADVERTISEMENT:
        if (type == NEIGHBOR_ADVERTISEMENT) {
          flags = packet.getData(4) & 0xff;
        }
        packet.copy(8, targetAddress, 0, 16);
        break;
      }

      int sum = packet.upperLayerHeaderChecksum();
      byte[] data = packet.getPayload();
      System.out.println("Payloadsize: " + data.length);
      sum = IPv6Packet.checkSum(sum, data, data.length);
      sum = (~sum) & 0xffff;
      if (sum == checksum) {
        System.out.println("ICMPv6: Checksum matches!!!");
      } else {
        System.out.printf("ICMPv6: Checksum error: %04x <?> %04x\n", checksum, sum);
      }
    }
  }

  @Override
  public byte[] generatePacketData(IPv6Packet packet) {
    byte[] buffer = new byte[127];
    buffer[0] = (byte) type;
    buffer[1] = (byte) code;
    /* crc goes at 2/3 */
    int pos = 4;
    switch (type) {
    case ECHO_REQUEST:
    case ECHO_REPLY:
      buffer[pos++] = (byte) (id >> 8);
      buffer[pos++] = (byte) (id & 0xff);
      buffer[pos++] = (byte) (seqNo >> 8);
      buffer[pos++] = (byte) (seqNo & 0xff);
      break;
    case NEIGHBOR_SOLICITATION:
    case NEIGHBOR_ADVERTISEMENT:
      if (type == NEIGHBOR_ADVERTISEMENT) {
        buffer[pos++] = (byte) flags;
      }
      pos = 8;
      for (int i = 0; i < targetAddress.length; i++) {
        buffer[pos++] = targetAddress[i];
      }
      break;
    case ROUTER_ADVERTISEMENT:
      buffer[pos++] = hopLimit;
      buffer[pos++] = autoConfigFlags;
      buffer[pos++] = (byte) (routerLifetime >> 8);
      buffer[pos++] = (byte) (routerLifetime & 0xff);
      IPv6Packet.set32(buffer, pos, reachableTime);
      pos += 4;
      IPv6Packet.set32(buffer, pos, retransmissionTimer);
      pos += 4;
      /* add options */
      System.arraycopy(srcLinkOptionLong, 0, buffer, pos, srcLinkOptionLong.length);
      pos += mtuOption.length;
      System.arraycopy(mtuOption, 0, buffer, pos, mtuOption.length);
      pos += mtuOption.length;
      System.arraycopy(prefixInfo, 0, buffer, pos, prefixInfo.length);
      pos += prefixInfo.length;
      break;
    }
    
    byte[] packetData = new byte[pos];
    System.arraycopy(buffer, 0, packetData, 0, pos);
    return packetData;
  }

  @Override
  public byte getDispatch() {
    return DISPATCH;
  }
}
