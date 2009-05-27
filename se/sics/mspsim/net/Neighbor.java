package se.sics.mspsim.net;

public class Neighbor {

  public static final int INCOMPLETE = 0;
  public static final int REACHABLE = 1;
  public static final int STALE = 2;
  public static final int DELAY = 3;
  public static final int PROBE = 4;
  public static final int NO_STATE = 5;

  
  byte[] ipAddress;
  byte[] linkAddress;
  NetworkInterface netInterface;
  int state = INCOMPLETE;
    
  public byte[] getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(byte[] ipAddress) {
    this.ipAddress = ipAddress;
  }
  
  public void setState(int state) {
    this.state = state;
  }
}
