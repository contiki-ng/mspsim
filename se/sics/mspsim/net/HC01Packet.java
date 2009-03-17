package se.sics.mspsim.net;

public class HC01Packet extends IPv6Packet {
public final static int IPHC_TTL_1 =  0x08;
public final static int IPHC_TTL_64 = 0x10;
public final static int IPHC_TTL_255 = 0x18;
public final static int IPHC_TTL_I    = 0x00;

  public static final int HC01_DISPATCH = 0x03;
  
  public void setPacketData(byte[] data, int len) {
    int pos = 3;
    if (data[0] != HC01_DISPATCH) return;
    if ((data[1] & 0x40) == 0) {
      if ((data[1] & 0x80) == 0) {
        version = (data[pos] & 0xf0) >> 4;
        trafficClass = ((data[pos] & 0x0f)<<4) + ((data[pos + 1] & 0xff) >> 4);
        flowLabel = (data[pos + 1] & 0x0f) << 16 + (data[pos + 2] & 0xff) << 8 +
          data[pos + 3] & 0xff;
        pos += 4;
      } else {
        version = 6;
        trafficClass = 0;
        flowLabel = (data[pos] & 0x0f) << 16 
        + (data[pos + 1] & 0xff) << 8 + data[pos + 2] & 0xff;;
        pos += 3;
      }
    } else {
      version = 6;
      flowLabel = 0;
      if ((data[1] & 0x80) == 0) {
        trafficClass = (data[pos] & 0xff);
        pos++;
      } else {
        trafficClass = 0;
      }
    }
    /* encoding of TTL */
    switch (data[1] & 0x18) {
    case IPHC_TTL_1:
      hopLimit = 1;
      break;
    case IPHC_TTL_64:
      hopLimit = 64;
      break;
    case IPHC_TTL_255:
      hopLimit = (byte) 0xff;
      break;
    case IPHC_TTL_I:
      hopLimit = data[pos++];
      break;
    }
    
    
    
  }
  
}
