package se.sics.mspsim.net;

import java.io.IOException;
import java.io.InputStream;

public class TCPInputStream extends InputStream {

  private TCPConnection connection;
  /* buffer 128 bytes for input... */
  private byte[] inputBuffer = new byte[128];
  private int firstByte = 0;
  private int nextEmpty = 0;
  boolean closed = false;
  long lastReadCall = 0;
  
  TCPListener listener = new TCPListener() {
    public void connectionClosed(TCPConnection connection) {
	closed = true;
	System.out.println("InputStream - connection closed...");
	notifyReader();
    }
    public void newConnection(TCPConnection connection) {
    }
    public void tcpDataReceived(TCPConnection source, TCPPacket packet) {
      byte[] payload = packet.payload;
      if (payload == null || payload.length == 0) return;
      /* add to cyclic buffer... */
      /* what if we can not accept all data??? */
      int pos = 0;
      /* check if it fits!!! */
      if (inputBuffer.length - available() > payload.length) {
        while (pos < payload.length) {
          inputBuffer[nextEmpty++] = payload[pos++];
          if (nextEmpty >= inputBuffer.length) {
            nextEmpty = 0;
          }
        }
        /* notify the possibly sleeping threads that we have data!!! */
        notifyReader();
      } else {
        System.out.println("ERROR!!!! packet does not fit buffer... should not ack!!!");
      }
    }
  };
  
  TCPInputStream(TCPConnection connection) {
    this.connection = connection;
  }

  private synchronized void notifyReader() {
    notify();
  }
  
  public int available() {
    int ava = nextEmpty - firstByte;
    if (ava < 0) ava += inputBuffer.length;
    return ava;
  }
  
  public void close() {
    connection.close();
    closed = true;
    notifyReader();
  }
  
  public int read() throws IOException {
    if (closed) {
      return -1;
    }
    synchronized(this) {
        lastReadCall = System.currentTimeMillis();
        while (!closed && firstByte == nextEmpty) {
            try {
                wait(1000);
                System.out.println("TCPInputStream: waiting for input...");
                if ((connection.timeout != -1) &&
                        lastReadCall + connection.timeout < System.currentTimeMillis()) {
                    throw new IOException("I/O operation: Read timed out...");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    if (!closed) {
      int data = inputBuffer[firstByte++];
      if (firstByte >= inputBuffer.length)
        firstByte = 0;
      return (data & 0xff);
    } else {
      return -1;
    }
  }
}