package se.sics.mspsim.net;

public class Neighbor {
  /*   From the RFC - States of the neighbors:
   * 
   *   INCOMPLETE  Address resolution is in progress and the link-layer
   *   address of the neighbor has not yet been determined. 
   *   
   *   REACHABLE   Roughly speaking, the neighbor is known to have been
   *   reachable recently (within tens of seconds ago).
   *   
   *   STALE       The neighbor is no longer known to be reachable but
   *   until traffic is sent to the neighbor, no attempt
   *   should be made to verify its reachability.
   *   
   *   DELAY       The neighbor is no longer known to be reachable, and
   *   traffic has recently been sent to the neighbor.
   *   Rather than probe the neighbor immediately, however,
   *   delay sending probes for a short while in order to
   *   give upper-layer protocols a chance to provide
   *   reachability confirmation.
   *   
   *   PROBE       The neighbor is no longer known to be reachable, and
   *   unicast Neighbor Solicitation probes are being sent to
   *   verify reachability.
   *   
   */

  public static final int INCOMPLETE = 0;
  public static final int REACHABLE = 1;
  public static final int STALE = 2;
  public static final int DELAY = 3;
  public static final int PROBE = 4;
  public static final int NO_STATE = 5;

  
  byte[] ipAddress;
  byte[] linkAddress;
  NetworkInterface netInterface;
  long reachableUntil;
  long lastNDSent;
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
