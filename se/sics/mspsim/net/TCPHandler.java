package se.sics.mspsim.net;
import java.io.IOException;
import java.io.PrintStream;
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
      c.pos = (byte) connectionNo;
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
                  try {
                      tc.send(tcpReply);
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
                  tc.sentUnack = tc.sendNext = tc.sendNext + 1;
                  tc.serverConnection = connection;          
              } else {
                  System.out.println("TCPHandler: dropping packet & sending RST - likely for old connection?");
                  TCPPacket tcpReply = tcpPacket.replyPacket();
                  tcpReply.flags = TCPPacket.RST | TCPPacket.ACK;
                  tcpReply.seqNo = tcpPacket.ackNo;
                  tcpReply.ackNo = tcpPacket.seqNo + 1;
                  IPv6Packet ipReply = packet.replyPacket(tcpReply);
                  ipStack.sendPacket(ipReply, packet.netInterface);
              }
          }
      } else {
      if (tcpPacket.isReset()) {
          /* something is very wrong - just close and remove... */
          connection.state = TCPConnection.CLOSED;
      }
      switch(connection.state) {
      case TCPConnection.SYN_RECEIVED:
        if (tcpPacket.isAck()) {
          System.out.println("TCPConnection: gotten ACK on syn! => ESTABLISHED!! " + connection.pos);
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
          System.out.println("TCPConnection: received FIN => CLOSE_WAIT!!!");
          connection.state = TCPConnection.CLOSE_WAIT;
        }
        
        connection.receive(tcpPacket);
        break;
      case TCPConnection.LAST_ACK:
	  if (tcpPacket.isAck()) {
	      System.out.println("Received ACK on FIN => CLOSED! " + connection.pos);
	      connection.state = TCPConnection.CLOSED;
	  }
        break;
      case TCPConnection.FIN_WAIT_1:
        if (tcpPacket.isAck()) {
          connection.state = TCPConnection.FIN_WAIT_2;
        }
        if (tcpPacket.isFin()) {
            connection.state = TCPConnection.TIME_WAIT;
        }
        connection.receive(tcpPacket);
        break;
      case TCPConnection.FIN_WAIT_2:
        if (tcpPacket.isFin()) {
          System.out.println("TCPHandler: setting connection in TIME_WAIT... " + connection.pos);
          connection.state = TCPConnection.TIME_WAIT;
          connection.lastSendTime = System.currentTimeMillis();
          connection.receiveNext++;
          connection.sendAck(tcpPacket);
        } else {
          connection.sendReset();
        }
        break;
      case TCPConnection.CLOSE_WAIT:
        /* ignore... */
          connection.sendReset();
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
            if (connection.timeout != -1) {
        	/* assume that we acked last incoming packet...? */
        	if (connection.lastSendTime + connection.timeout < time) {
        	    connection.close();
        	}
            }
            break;
        case TCPConnection.CLOSE_WAIT:
            /* if nothing in buffer - close it! */
            if (connection.outSize() == 0) {
        	System.out.println("Closing - sending FIN");
        	connection.state = TCPConnection.LAST_ACK;
        	connection.sendFIN();
            } else {
        	/* send something from the buffer */
        	connection.resend();
          }
          break;
        case TCPConnection.FIN_WAIT_1:
        case TCPConnection.FIN_WAIT_2:
            if (connection.lastSendTime + connection.retransmissionTime < time) {
                /* should probably resend the FIN! */
                connection.resend();
            }
            break;
        case TCPConnection.TIME_WAIT:
            /* wait for a while ... */
            if (connection.lastSendTime + TCPConnection.TIME_WAIT_MILLIS < time) {
        	System.out.println("TCPHandler: TIME_WAIT over => CLOSED!");
        	connection.state = TCPConnection.CLOSED;
            }
            break;
        case TCPConnection.CLOSED:
            System.out.println("TCPHandler: Connection is closed... removing connection " + connection.pos);
            connection.closed();
            connectionNo--;
            if (connectionNo > 0) {
        	/* move the last connection to this position - we do not need
        	 * this any more ??!!
        	 */
        	activeConnections[i] = activeConnections[connectionNo];
        	activeConnections[i].pos = (byte) i;
        	i--; /* allow processing of that connection too */
            }
            break;
        }
      }
    }
  }

  public void printStatus(PrintStream out) {
	  out.println("---- TCP Connection info ----");
	  for (int i = 0; i < connectionNo; i++) {
		  out.println("* Connection " + i + " in state: " + activeConnections[i].state);
	  }
  }

}
