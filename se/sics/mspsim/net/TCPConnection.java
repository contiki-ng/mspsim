package se.sics.mspsim.net;

import se.sics.mspsim.util.Utils;

public class TCPConnection {
  // States of the connection
  public static final int CLOSED = 0;
  public static final int LISTEN = 1;
  public static final int SYN_SENT = 2;
  public static final int SYN_RECEIVED = 3;
  public static final int ESTABLISHED = 4;
  public static final int CLOSE_WAIT = 5;
  public static final int LAST_ACK = 6;
  public static final int FIN_WAIT_1 = 7;
  public static final int FIN_WAIT_2 = 8;
  public static final int CLOSING = 9;
  public static final int TIME_WAIT = 10;

  // my port & IP (IP can be null here...)
  int localPort;
  byte[] localIP;
  // other port
  int externalPort = -1;
  byte[] externalIP;

  int state;
  
  /* sent unacked and nextSend byte */
  int sentUnack;
  int sendNext;
  int sendWindow = TCPPacket.DEFAULT_WINDOW;

  /* last received seqNo + payloadLen*/
  int receiveNext;
  int receiveWindow = TCPPacket.DEFAULT_WINDOW;
  
  /* check if incoming packet matches */
  public boolean matches(IPv6Packet packet, TCPPacket tcpPacket) {
    if ((externalPort == -1 || tcpPacket.sourcePort == externalPort) &&
        tcpPacket.destinationPort == localPort &&
        (localIP == null || Utils.equals(localIP, packet.destAddress)) &&
        (externalIP == null || Utils.equals(externalIP, packet.sourceAddress))) {
      return true;
    }
    return false;
  }

  public void updateOnSend(TCPPacket tcpPacket) {
    sendNext += tcpPacket.payload.length;
  }
    
  public void updateOnReceive(TCPPacket tcpPacket) {
    int plen = tcpPacket.payload == null ? 0 : tcpPacket.payload.length;
    receiveNext = tcpPacket.seqNo + plen;
  }


  
}
