package se.sics.mspsim.net;

import java.io.IOException;
import java.io.OutputStream;

public class TCPOutputStream extends OutputStream {

  /* a tiny output buffer for buffering to a TCP packet */
  byte[] output = new byte[40];
  int pos = 0;
  TCPConnection connection;
  
  TCPOutputStream(TCPConnection connection) {
    this.connection = connection;
  }

  public synchronized void write(int data) throws IOException {
    if (connection.state != TCPConnection.ESTABLISHED) {
      throw new IOException("TCP connection not open state: " + connection.state);
    }
    output[pos++] = (byte) (data & 0xff);

    /* oops, the buffer is full... - send packet immediately */
    if (pos == output.length) {
      flush();
    }
  }

  public synchronized void flush() throws IOException {
    byte[] buffer = new byte[pos];
    for (int i = 0; i < pos; i++) {
      buffer[i] = output[i];
    }
    pos = 0;
    connection.send(buffer);
  }
}
