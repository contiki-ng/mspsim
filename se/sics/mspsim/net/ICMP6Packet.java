package se.sics.mspsim.net;

import java.io.PrintStream;

public class ICMP6Packet { //extends Packet {

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

  public static final String[] TYPE_NAME = new String[] {
    "ECHO_REQUEST", "ECHO_REPLY",
    "GROUP_QUERY", "GROUP_REPORT", "GROUP_REDUCTION",
    "ROUTER_SOLICITATION", "ROUTER_ADVERTISEMENT", 
    "NEIGHBOR_SOLICITATION", "NEIGHBOR_ADVERTISEMENT"};

  int type;
  int code;
  int checksum;
  byte[] targetAddress;

  int id;
  int seqNo;

  int flags;

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
        targetAddress = new byte[16];
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
}
