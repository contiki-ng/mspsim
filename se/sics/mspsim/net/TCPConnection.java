package se.sics.mspsim.net;

import java.io.InputStream;
import java.io.OutputStream;

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
  
  public static final long TIME_WAIT_MILLIS = 1000;
  private static final int OUT_BUFFER = 128;
  
  /* retransmission time in milliseconds */
  int retransmissionTime = 1000;
  
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
  
  private IPStack ipStack;
  private NetworkInterface netInterface;
  private TCPListener tcpListener;
  TCPConnection serverConnection;
  
  long lastSendTime;
  
  private byte[] outgoingBuffer = new byte[OUT_BUFFER];
  int bufPos = 0;
  int bufNextEmpty = 0;
  /* sentUnack == bufPos */
  
  private TCPInputStream inputStream;
  private TCPOutputStream outputStream;
  
  TCPConnection(IPStack stack, NetworkInterface nIf) {
    ipStack = stack;
    netInterface = nIf;
  }
  
  public InputStream getInputStream() {
      if (inputStream == null) {
	  System.out.println("TCPConnection: creating new input stream...");
	  inputStream = new TCPInputStream(this);
	  /* steal the listener... */
	  tcpListener = inputStream.listener;
      }
      return inputStream;
  }
  
  public OutputStream getOutputStream() {
    if (outputStream == null) {
      outputStream = new TCPOutputStream(this);
    }
    return outputStream;
  }
  
  public void setTCPListener(TCPListener listener) {
    if (tcpListener == null) {
      tcpListener = listener;
    } else {
      throw new IllegalStateException("TCPListener already set: " + tcpListener);
    }
  }
  
  public void newConnection(TCPConnection c) {
    if (tcpListener != null) {
      tcpListener.newConnection(c);
    }
  }
  
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

  /* send packet + update sendNext - this should take into account ext window */
  /* is this what mess up the stuff */
  public void send(TCPPacket tcpPacket) {
    IPv6Packet packet = new IPv6Packet(tcpPacket, localIP, externalIP);
    tcpPacket.seqNo = sendNext;
    tcpPacket.ackNo = receiveNext;
    if (tcpPacket.payload != null) {
      copyToBuffer(tcpPacket.payload);
      sendNext += tcpPacket.payload.length;
      System.out.println("SEND: Updated sendNext: " + sendNext +
	      " outSize: " + outSize() + " seqDiff: " +
	      (sendNext - sentUnack));
    }
    lastSendTime = System.currentTimeMillis();
    tcpPacket.printPacket(System.out);
    ipStack.sendPacket(packet, netInterface);
  }

  /* number of currently un-acked bytes in buffer */
  int outSize() {
    int bytesToSend = bufNextEmpty - bufPos;
    if (bytesToSend < 0) bytesToSend += outgoingBuffer.length;
    return bytesToSend;
  }
  
  private synchronized void copyToBuffer(byte[] data) {
    int empty = outgoingBuffer.length - outSize();
    while (empty < data.length || state != TCPConnection.ESTABLISHED) {
      /* need to block this tread until place for data is available...*/
      try {
	  System.out.println("blocking output...");
	  wait(1000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return;
      }
      empty = outgoingBuffer.length - outSize();
    }
    for (int i = 0; i < data.length; i++) {
	outgoingBuffer[bufNextEmpty++] = data[i];
	if (bufNextEmpty >= outgoingBuffer.length)
	    bufNextEmpty = 0;
    }
  }
  
  synchronized void resend() {
    int size = outSize();
    System.out.println("### Bytes to resend: " + outSize() + " seqDiff: " +
	    (sendNext - sentUnack));
    /* nothing to resend... */
    if (size == 0) return;
    
    if (outSize() < size) size = outSize();
    /* ensure small payload size... this should be handled at IP level...*/
    if (size > 40) {
	size = 40;
    }
    byte[] data = new byte[size];
    int pos = bufPos;
    for (int i = 0; i < data.length; i++) {
      data[i] = outgoingBuffer[pos++];
      if (pos >= outgoingBuffer.length) {
        pos = 0;
      }
    }
    
    System.out.println("**** TCPConnection resending data: size = " + size);
    
    TCPPacket tcpPacket = createPacket(); 
    IPv6Packet packet = new IPv6Packet(tcpPacket, localIP, externalIP);
    tcpPacket.seqNo = sentUnack;
    tcpPacket.payload = data;
    lastSendTime = System.currentTimeMillis();
    tcpPacket.printPacket(System.out);
    ipStack.sendPacket(packet, netInterface);
  }
  
  synchronized void receive(TCPPacket tcpPacket) {
    int plen = tcpPacket.payload == null ? 0 : tcpPacket.payload.length;

    if (tcpPacket.isAck()) {
      /* check if correct ack - this is the "max" case*/
      if (sentUnack <= tcpPacket.ackNo && sendNext >= tcpPacket.ackNo) {
        /* no more unacked data */
        int noAcked = tcpPacket.ackNo - sentUnack;
        sentUnack = tcpPacket.ackNo;
        bufPos += noAcked;
        if (bufPos >= outgoingBuffer.length)
            bufPos -= outgoingBuffer.length;
        System.out.println("ACK for " + noAcked + " bytes. pos: " + bufPos +
              " nxtE:" + bufNextEmpty + " unack: " + Integer.toString(sentUnack & 0xffff, 16) + " sendNext: " 
              + Integer.toString(sendNext & 0xffff, 16) + " outSize: " + outSize() + 
              " seqDiff: " + (sendNext - sentUnack) + " plen: " + plen);
        notify();
        /* this means that we can send more data !!*/
      } else {
	  System.out.println("TCPHandler: Unexpected ACK no: " +
		  Integer.toString(tcpPacket.ackNo & 0xffff, 16) +
		  " sendNext: " + Integer.toString(sendNext & 0xffff, 16) + " sentUnack: " +
		  Integer.toString(sentUnack & 0xffff,16));
	  if (tcpPacket.ackNo == sentUnack) {
	      resend();
	  }
      }
    }
    
    if (receiveNext == tcpPacket.seqNo) {
      //System.out.println("TCPHandler: data received ok!!!");
	/* only ack if new data arrived! */

	/* update what to expect next - after this packet! */
	receiveNext = tcpPacket.seqNo + plen;

	if (plen > 0) {
	    /* ack the new data! - this could be done from the connection itself!!*/	    
	    sendAck(tcpPacket);

	    if (tcpListener != null) {
		tcpListener.tcpDataReceived(this, tcpPacket);
	    } else {
		System.out.println("*** ERROR: dropped data: did not have listener...");
	    }
	}
    } else {
	/* error - did we miss a packet??? - send ack to say where we are...*/
	System.out.println("TCPHandler: seq error: expSeq: " +
		Integer.toString(receiveNext & 0xffff, 16) + " != seqNo: " +
		Integer.toString(tcpPacket.seqNo & 0xffff, 16));
	sendAck(tcpPacket);
    }
    
    if (tcpPacket.isFin()) {
      if (tcpListener != null && plen > 0) {
        /* notify app that the other side is closing... */
        tcpListener.connectionClosed(this);
      }
    }
  }

  private void sendAck(TCPPacket tcpPacket) {
      TCPPacket tcpReply = TCPHandler.createAck(tcpPacket, 0);
      tcpReply.ackNo = receiveNext;
      tcpReply.seqNo = sendNext;

      send(tcpReply);
  }

  public void send(byte[] bytes) {
    TCPPacket tcpPacket = createPacket();
    tcpPacket.payload = bytes;
    send(tcpPacket);
  }

  public void close() {
    if (state == ESTABLISHED) {
      TCPPacket packet = createPacket();
      packet.flags |= TCPPacket.FIN;
      state = FIN_WAIT_1;
      send(packet);
    }
  }
  
  public TCPPacket createPacket() {
    TCPPacket tcpPacket = new TCPPacket();
    tcpPacket.sourcePort = localPort;
    tcpPacket.destinationPort = externalPort;
    return tcpPacket;
  } 
}