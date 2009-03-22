package se.sics.mspsim.net;

import java.io.PrintStream;

public class ICMP6Packet extends AbstractPacket {

  public static final int ECHO_REQUEST = 128;
  public static final int ECHO_REPLY = 129;
  public static final int GROUP_QUERY = 130;
  public static final int GROUP_REPORT = 131;
  public static final int GROUP_REDUCTION = 132;
  public static final int ROUTER_SOLICITATION = 133;
  public static final int ROUTER_ADVERTISEMENT = 134;
  public static final int NEIGHBOR_SOLICITATION = 135;
  public static final int NEIGHBOR_ADVERTISEMENT = 136;

  public static final String[] TYPE_NAME = new String[] {
    "ECHO_REQUEST", "ECHO_REPLY",
    "GROUP_QUERY", "GROUP_REPORT", "GROUP_REDUCTION",
    "ROUTER_SOLICITATION", "ROUTER_ADVERTISEMENT", 
    "NEIGHBOR_SOLICITATION", "NEIGHBOR_ADVERTISEMENT"};
    
  int type;
  int code;
  int checksum;
  
  IPv6Packet ip;
  
  public byte[] getDataField(String name) {
    return null;
  }

  public int getIntField(String name) {
    return 0;
  }

  public int getSize() {
    return 0;
  }

  public void printPacket(PrintStream out) {
    String typeS = "" + type;
    if (type >= 128) {
      int tS = type - 128;
      if (tS < TYPE_NAME.length) {
        typeS = TYPE_NAME[tS];
      }
    }
    
    
    out.printf("ICMPv6 Type: %d (%s) Code: %d Chk: %04x \n", type, typeS,
        code, checksum);
  }

  public void setPacketData(Packet packet, byte[] data, int len) {
    valid = false;
    if (packet instanceof IPv6Packet) {
      ip = (IPv6Packet) packet;
      
      if (ip.nextHeader == 58)
      type = data[0] & 0xff;
      code = data[1] & 0xff;
      checksum = ((data[2] & 0xff) << 8) | data[3] & 0xff;
      valid = true;
      /* test the checksum ... */
//      int sum = ip.upperLayerHeaderChecksum();
//      System.out.printf("*** My Checksum: %04x", 
//          IPv6Packet.checkSum(sum, data, len));
      
    }
  }

}
