package se.sics.mspsim.net;
import java.util.Timer;
import java.util.TimerTask;


public class TCPHandler extends TimerTask {
  /* MAX 16 simult. connections for now */
  private static final int MAX_CONNECTIONS = 16;
  private static final int MAX_LISTEN = 16;

  TCPConnection[] activeConnections = new TCPConnection[MAX_CONNECTIONS];
  TCPConnection[] listenConnections = new TCPConnection[MAX_LISTEN];
  
  int connectionNo = 0;
  int listenNo = 0;
  IPStack ipStack;
  Timer timer;
  
  public TCPHandler(IPStack stack) {
    this.ipStack = stack;
    timer = ipStack.getTimer();
    timer.schedule(this, 100, 100);
  }
  
  public synchronized TCPConnection addListenConnection(int port) {
    TCPConnection conn = listenConnections[listenNo++] =
      new TCPConnection(ipStack, null);

    conn.localPort = port;
    conn.state = TCPConnection.LISTEN;
    return conn;
  }

  private synchronized void addConnection(TCPConnection c) {
    activeConnections[connectionNo++] = c;
  }
  
  public void handlePacket(IPv6Packet packet) {
    TCPPacket tcpPacket = (TCPPacket) packet.getIPPayload();
    
    TCPConnection connection = findConnection(packet, tcpPacket);
    if (connection == null) {
      connection = findListenConnection(packet, tcpPacket);
      if (connection == null) {
        System.out.println("TCPHandler: can not find active or listen connection for this packet");        
      } else {
        if (tcpPacket.isSyn()) {
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

          addConnection(tc);

          /* established => report to listeners... */
          connection.newConnection(tc);
          
          tcpReply.ackNo = tc.receiveNext;
          tc.send(tcpReply);
          tc.sentUnack = tc.sendNext = tc.sendNext + 1;
          tc.serverConnection = connection;          
        }
      }
    } else {
      switch(connection.state) {
      case TCPConnection.SYN_RECEIVED:
        if (tcpPacket.isAck()) {
          System.out.println("TCPConnection: gotten ACK on syn! => ESTABLISHED!!");
          connection.state = TCPConnection.ESTABLISHED;
          connection.receive(tcpPacket);
          
          synchronized(connection) {
              /* for any early outputter to the output stream */
              connection.notify();
          }
        }
        break;
      case TCPConnection.ESTABLISHED:
        if (tcpPacket.isFin()) {
          connection.state = TCPConnection.CLOSE_WAIT;
        }
        
        connection.receive(tcpPacket);
        break;
      case TCPConnection.LAST_ACK:
        if (tcpPacket.isAck()) {
          connection.state = TCPConnection.CLOSED;
        }
        break;
      case TCPConnection.FIN_WAIT_1:
        if (tcpPacket.isAck()) {
          connection.state = TCPConnection.FIN_WAIT_2;
        }
        break;
      case TCPConnection.FIN_WAIT_2:
        if (tcpPacket.isFin()) {
          System.out.println("TCPHandler: setting connection in TIME_WAIT...");
          connection.state = TCPConnection.TIME_WAIT;
          connection.lastSendTime = System.currentTimeMillis();
        }
        break;
      case TCPConnection.CLOSE_WAIT:
        /* ignore... */
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

  /* The run method for checking connections, etc */
  public synchronized void run() {
    if (connectionNo > 0) {
      long time = System.currentTimeMillis();
      for (int i = 0; i < connectionNo; i++) {
        TCPConnection connection = activeConnections[i];
        switch (connection.state) {
        case TCPConnection.ESTABLISHED:
            /* here we should check for retransmissions... */
            if (connection.outSize() > 0 &&
        	(connection.lastSendTime + connection.retransmissionTime < time)) {
        	System.out.println("### Timeout - retransmitting...");
        	connection.resend();
            }
            break;
        case TCPConnection.CLOSE_WAIT:
          /* if nothing in buffer - close it! */
          if (connection.bufPos == connection.bufNextEmpty) {
            System.out.println("Closing - sending FIN");
            TCPPacket packet = connection.createPacket();
            packet.flags |= TCPPacket.FIN;
            connection.state = TCPConnection.LAST_ACK;
            connection.send(packet);
          } else {
            /* send something from the buffer */
          }
          break;
        case TCPConnection.TIME_WAIT:
          /* wait for a while ... */
          if (connection.lastSendTime < time + TCPConnection.TIME_WAIT_MILLIS) {
            System.out.println("TCPHandler: Connection is closed...");
            connection.state = TCPConnection.CLOSED;

            connectionNo--;
            if (i > 0) {
              /* move the last connection to this position - we do not need
               * this any more ??!!
               */
              activeConnections[i] = activeConnections[connectionNo];
              i--; /* allow processing of that connection too */
            }
          }
          break;
        }
      }
    }
  }
}
