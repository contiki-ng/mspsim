package se.sics.mspsim.net;
import java.io.PrintStream;
import java.util.Vector;

import se.sics.mspsim.util.Utils;

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

  public static final int SOURCE_LINKADDR = 1;
  public static final int TARGET_LINKADDR = 2;
  public static final int PREFIX_INFO = 3;
  public static final int MTU_INFO = 5;
  
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

  byte hopLimit = (byte) 128;
  byte autoConfigFlags;
  int routerLifetime = 100; /* time in seconds for keeping the router as default */
  int reachableTime = 360000; /* time in millis when node still should be counted as reachable */
  int retransmissionTimer = 0; /* time in millis between solicitations */
  int mtuSize = 1280;

  byte[] echoData;
  
  // for 1.3 compatibility
  private Vector options = new Vector();
  
  /* prefix info option - type = 3, len = 4 (64x4 bits), prefix = 64 bits */
  private final static byte[] defaultPrefixInfo = 
    new byte[] {3, 4, 64, (byte) (ON_LINK | AUTOCONFIG),
    (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, /* valid lifetime -1 seconds for now*/
    (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, /* prefered lifetime -1 seconds lifetime of autoconf addr */
    0, 0, 0, 0, /* reserved */
    /* the prefix ... */
    (byte)0xaa, (byte)0xaa, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0,  0, 0, 0, 0
  };

  /* default MTU is 1280 (5x256) which also is the smallest allowed */
  byte[] mtuOption = new byte[] {5, 1, 0, 0, 0, 0, 5, 0};

  void updateRA(IPStack stack) {
    byte[] llAddr = stack.getLinkLayerAddress();
    options.removeAllElements();
    byte[] prefixInfo = new byte[defaultPrefixInfo.length];
    System.arraycopy(defaultPrefixInfo, 0, prefixInfo, 0, defaultPrefixInfo.length);
    byte[] prefix = stack.prefix;
    System.arraycopy(prefix, 0, prefixInfo, 16, prefix.length);
    options.addElement(prefixInfo);
    options.addElement(mtuOption);
    addLinkOption(SOURCE_LINKADDR, llAddr);
  }
  
  void addLinkOption(int type, byte[] llAddr) {
    byte[] opt;
    if (llAddr.length > 6) {
      opt = new byte[16];
    } else {
      opt = new byte[8];
    }
    opt[0] = (byte) type;
    opt[1] = (byte) (opt.length / 8);
    System.arraycopy(llAddr, 0, opt, 2, llAddr.length);    
    options.addElement(opt);
  }

  public int getType() {
    return type;
  }
  public void setType(int type) {
    this.type = type;
  }

  public int getFlags() {
    return flags;
  }
  public void setFlags(int flags) {
    this.flags = flags;
  }
  
  public byte[] getOption(int type) {
    for (int i = 0; i < options.size(); i++) {
      if (((byte[])options.elementAt(i))[0] == type) {
        return (byte[]) options.elementAt(i);
      }
    }
    return null;
  }
  
  public byte[] getEchoData() {
    return echoData;
  }
  
  public void setEchoData(byte[] edata) {
    echoData = edata;
  }
  
  public void printPacket(PrintStream out) {
    String typeS = "" + type;
    if (type >= 128) {
      int tS = type - 128;
      if (tS < TYPE_NAME.length) {
        typeS = TYPE_NAME[tS];
      }
    }
    out.println("ICMPv6 Type: " + type + " (" + typeS + ") Code: " + code + " id: " +
        id + " seq: " + seqNo);
    if (targetAddress != null) {
      out.print("ICMPv6 Target address: ");
      IPv6Packet.printAddress(out, targetAddress);
      out.println();
    }
    if (type == ROUTER_ADVERTISEMENT) {
      System.out.println("ICMPv6 Route Advertisement");
      System.out.println("  Hop Limit: " + (hopLimit & 0xff));
      System.out.println("  autoConfig: " + (autoConfigFlags & 0xff));
      System.out.println("  routerLifeTime: " + routerLifetime + " (sec)");
      System.out.println("  reachableTime: " + reachableTime + " (msec)");
      System.out.println("  retransmissionTimer: " + retransmissionTimer + " (msec)");
      System.out.println("  autoConf: " + autoConfigFlags);
      byte[] prefixInfo = getOption(PREFIX_INFO);
      int bits = prefixInfo[2];
      int bytes = bits / 8;
      out.print("RA Prefix: ");
      for (int i = 0; i < bytes; i++) {
        out.print(Utils.hex8(prefixInfo[16 + i]));
        if ((i & 1) == 1) out.print(":");
      }
      out.println("/" + bits);
      out.println("RA Valid Lifetime: " + Packet.get32(prefixInfo, 4));
      out.println("RA Pref. Lifetime: " + Packet.get32(prefixInfo, 8));
      byte[] srcLink = getOption(SOURCE_LINKADDR);
      if (srcLink != null) {
        /* assuming 8 bytes for the mac ??? */
        System.out.print("Source Link: ");
        IPv6Packet.printMACAddress(out, srcLink, 2, 8);
        System.out.println();
      }
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
        int dataLen = packet.getPayloadLength() - 8;
        if (dataLen > 0) {
          echoData = new byte[dataLen];
          packet.copy(8, echoData, 0, dataLen);
        }
        break;
      case NEIGHBOR_SOLICITATION:
      case NEIGHBOR_ADVERTISEMENT:
        if (type == NEIGHBOR_ADVERTISEMENT) {
          flags = packet.getData(4) & 0xff;
        }
        targetAddress = new byte[16];
        packet.copy(8, targetAddress, 0, 16);
        handleOptions(packet, 24);
        break;
      case ROUTER_SOLICITATION:
        break;
      case ROUTER_ADVERTISEMENT:
        hopLimit = packet.getData(4);
        autoConfigFlags = packet.getData(5);
        routerLifetime = packet.get16(6);
        reachableTime = packet.get32(8);
        retransmissionTimer = packet.get32(12);
        handleOptions(packet, 16);
        break;
      }

      byte[] data = packet.getPayload();
      System.out.println("Payloadsize: " + data.length);
      int sum = packet.upperLayerHeaderChecksum();
      sum = IPv6Packet.checkSum(sum, data, data.length);
      sum = (~sum) & 0xffff;
      if (sum == checksum) {
        System.out.println("ICMPv6: Checksum matches!!!");
      } else {
        System.out.println("ICMPv6: Checksum error: " + 
            Utils.hex16(checksum) + " <?> " + Utils.hex16(sum));
      }
    }
  }

  /* create generic options array instead... */
  private void handleOptions(IPv6Packet packet, int pos) {
    int size = packet.getPayloadLength();
    System.out.println("ICMPv6 Options: total size: " + size + " pos: " + pos);
    while (pos < size) {
      int type = packet.getData(pos);
      int oSize = (packet.getData(pos + 1) & 0xff) * 8;
      System.out.println("Handling option: " + type + " size " + oSize);
      if (oSize == 0) return;
      byte[] option = new byte[oSize];
      packet.copy(pos, option, 0, oSize);
      options.addElement(option);
      pos += oSize;
    }
  }
  
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
      if (echoData != null) {
        for (int i = 0; i < echoData.length; i++) {
          buffer[pos++] = echoData[i];
        }
      }
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
      pos = addOptions(buffer, pos);
      break;
    case ROUTER_ADVERTISEMENT:
      buffer[pos++] = hopLimit;
      buffer[pos++] = autoConfigFlags;
      buffer[pos++] = (byte) ((routerLifetime >> 8) & 0xff);
      buffer[pos++] = (byte) (routerLifetime & 0xff);
      IPv6Packet.set32(buffer, pos, reachableTime);
      pos += 4;
      IPv6Packet.set32(buffer, pos, retransmissionTimer);
      pos += 4;
      /* add options */
      pos = addOptions(buffer, pos);
      break;
    }

    byte[] packetData = new byte[pos];
    System.arraycopy(buffer, 0, packetData, 0, pos);

    /* TODO: this should probably be taken care of in another way - 
     * for example by allowing the IPPayload packets to set the data
     * into the payload which sets the payload length...  
     */
    packet.payloadLen = pos;
    int sum = packet.upperLayerHeaderChecksum();
    sum = IPv6Packet.checkSum(sum, packetData, packetData.length);
    sum = (~sum) & 0xffff;

    packetData[2] = (byte) (sum >> 8);
    packetData[3] = (byte) (sum & 0xff);

    return packetData;
  }

  private int addOptions(byte[] buffer, int pos) {
    for (int i = 0; i < options.size(); i++) {
      byte[] option = (byte[]) options.elementAt(i);
      System.out.println("Adding option: " + option[0] + " len: " + option[1] +
          "/" + option.length + " at " + pos);
      System.arraycopy(option, 0, buffer, pos, option.length);
      pos += option.length;
    }
    return pos;
  }
  
  public byte getDispatch() {
    return DISPATCH;
  }
  
  
  public static void main(String[] args) {
    byte[] pData = Utils.hexconv("6000000000403a3f200105c01000000a00000000000001ad200105c011024e000212740504030201800070e3de1b00011b87194ad859000008090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f3031323334353637");
    IPv6Packet packet = new IPv6Packet();
    packet.setBytes(pData);
    packet.parsePacketData(packet);
    if (packet.nextHeader == 58) {
      ICMP6Packet icmpPacket = new ICMP6Packet();
      icmpPacket.parsePacketData(packet);
      icmpPacket.printPacket(System.out);
    }
  }
}
