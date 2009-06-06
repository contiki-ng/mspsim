package se.sics.mspsim.net;


public class TCPHandler {
  /* MAX 16 simult. connections for now */
  private static final int MAX_CONNECTIONS = 16;
  private static final int MAX_LISTEN = 16;
  
  TCPConnection[] activeConnections = new TCPConnection[MAX_CONNECTIONS];
  TCPConnection[] listenConnections = new TCPConnection[MAX_LISTEN];
  
  int connectionNo = 0;
  int listenNo = 0;
  IPStack ipStack;
  
  public TCPHandler(IPStack stack) {
    this.ipStack = stack;
  }
  
  public synchronized TCPConnection addListenConnection(int port) {
    TCPConnection conn = listenConnections[listenNo++] =
      new TCPConnection(ipStack, null);

    conn.localPort = port;
    conn.state = TCPConnection.LISTEN;
    return conn;
  }
  
  public void handlePacket(IPv6Packet packet) {
    TCPPacket tcpPacket = (TCPPacket) packet.getIPPayload();
    
    TCPConnection connection = findConnection(packet, tcpPacket);
    if (connection == null) {
      connection = findListenConnection(packet, tcpPacket);
      if (connection == null) {
        System.out.println("TCPHandler: can not find active or listen connection for this packet");        
      } else {
        System.out.println("TCPHandler: found listen connection!!!");
        if ((tcpPacket.flags & TCPPacket.SYN) > 0) {
          TCPPacket tcpReply = createAck(tcpPacket, TCPPacket.SYN);
          TCPConnection tc = new TCPConnection(ipStack, packet.netInterface);
          /* setup the connection */
          tc.externalIP = packet.sourceAddress;
          tc.externalPort = tcpPacket.sourcePort;
          tc.localIP = ipStack.myIPAddress;
          tc.localPort = tcpPacket.destinationPort;
          tc.state = TCPConnection.SYN_RECEIVED;
          tc.receiveNext = tcpPacket.seqNo + 1;
          tc.sentUnack = tc.sendNext = tcpReply.seqNo = (int) (System.currentTimeMillis() * 7);
          activeConnections[connectionNo++] = tc;
          tcpReply.ackNo = tcpPacket.seqNo + 1;
          System.out.println("TCPHandler: Sending: " + tcpReply);
          tc.send(tcpReply);
          tc.sentUnack = tc.sendNext = tc.sendNext + 1;
          connection.newConnection(tc);
        }
      }
    } else {
      System.out.println("TCPHandler: found connection!!!");
      
      switch(connection.state) {
      case TCPConnection.SYN_RECEIVED:
        if ((tcpPacket.flags & TCPPacket.ACK) > 0) {
          System.out.println("TCPConnection: gotten ACK on syn! => ESTABLISHED!!");
          connection.state = TCPConnection.ESTABLISHED;
        }
        break;
      case TCPConnection.ESTABLISHED:
        System.out.println("Gotten packet of real data!!!");
        int plen = 0;
        if (tcpPacket.payload != null) {
          /* payload received !!! */
          System.out.println("Payload of: " + tcpPacket.payload.length);
          plen = tcpPacket.payload.length;
        }

        /* we should check if we have acked the last data from the other */
        if (tcpPacket.isAck() && 
            (tcpPacket.payload == null || tcpPacket.payload.length == 0)) {
          return;
        }

        connection.receive(tcpPacket);
        break;
      }     
    }
  }
  
  static TCPPacket createAck(TCPPacket tcpPacket, int flags) {
    TCPPacket tcpReply = tcpPacket.replyPacket();
    tcpReply.flags |= flags | TCPPacket.ACK;
    return tcpReply;
  }
  
  private TCPConnection findConnection(IPv6Packet packet, TCPPacket tcpPacket) {
    for (int i = 0; i < connectionNo; i++) {
      if (activeConnections[i].matches(packet, tcpPacket)) {
        return activeConnections[i];
      }
    }
    return null;
  }

  private TCPConnection findListenConnection(IPv6Packet packet, TCPPacket tcpPacket) {
    for (int i = 0; i < listenNo; i++) {
      if (listenConnections[i].matches(packet, tcpPacket)) {
        return listenConnections[i];
      }
    }
    return null;
  }

}
