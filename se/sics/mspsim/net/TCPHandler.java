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
    TCPConnection conn = listenConnections[0] = new TCPConnection();
    /* fake a port 23 - telnet */
    conn.localPort = 23;
    conn.state = TCPConnection.LISTEN;
    listenNo++;
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
          IPv6Packet reply = createAck(packet, tcpPacket, TCPPacket.SYN);
          TCPPacket tcpReply = (TCPPacket) reply.getIPPayload();
          TCPConnection tc = new TCPConnection();
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
          ipStack.sendPacket(reply, packet.netInterface);
          tc.sentUnack = tc.sendNext = tc.sendNext + 1;
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
        int flag = 0;
        if (tcpPacket.isAck()) {
          /* check if correct ack - we are only sending a packet a time... */
          if (connection.sendNext == tcpPacket.ackNo) {
            /* no more unacked data */
            connection.sentUnack = tcpPacket.ackNo;
            /* this means that we can send more data !!*/
          } else {
            System.out.println("TCPHandler: Unexpected ACK no: " +
                Integer.toString(tcpPacket.ackNo, 16) +
                " sendNext: " + Integer.toString(connection.sendNext, 16));
          }
        }
        int plen = 0;
        if (tcpPacket.payload != null) {
          /* payload received !!! */
          System.out.println("Payload of: " + tcpPacket.payload.length);
          plen = tcpPacket.payload.length;
        }

        if (connection.receiveNext == tcpPacket.seqNo) {
          System.out.println("TCPHandler: data received ok!!!");
        } else {
          System.out.println("TCPHandler: seqNo error: receiveNext: " +
              connection + " != seqNo: " + tcpPacket.seqNo);
        }
        
        connection.receiveNext = tcpPacket.seqNo + plen;
        
        IPv6Packet reply = createAck(packet, tcpPacket, flag);
        TCPPacket tcpReply = (TCPPacket) reply.getIPPayload();
        tcpReply.ackNo = tcpPacket.seqNo + plen;
        tcpReply.seqNo = connection.sendNext;
        System.out.println("TCPHandler: Sending ACK: ");
        ipStack.sendPacket(reply, packet.netInterface);
        break;
      }
      
    }
  }

  private IPv6Packet createAck(IPv6Packet packet, TCPPacket tcpPacket, int flags) {
    TCPPacket tcpReply = tcpPacket.replyPacket();
    IPv6Packet reply = packet.replyPacket(tcpReply);
    reply.sourceAddress = ipStack.myIPAddress;
    tcpReply.flags |= flags | TCPPacket.ACK;
    return reply;
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
