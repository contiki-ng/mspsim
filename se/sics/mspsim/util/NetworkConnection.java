/**
 * Copyright (c) 2007, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of MSPSim.
 *
 * $Id: $
 *
 * -----------------------------------------------------------------
 *
 * NetworkConnection
 *
 * Author  : Joakim Eriksson
 * Created : 31 mar 2008
 * Updated : $Date:$
 *           $Revision:$
 */
package se.sics.mspsim.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import se.sics.mspsim.chip.PacketListener;

/**
 * @author joakim
 *
 */
public class NetworkConnection implements Runnable {

  private final static boolean DEBUG = true;  
  private final static int DEFAULT_PORT = 4711;
  
  ServerSocket serverSocket = null;
  
  ArrayList<ConnectionThread> connections = new ArrayList<ConnectionThread>();
  
  private PacketListener listener;
  
  public NetworkConnection() {
    if (connect(DEFAULT_PORT)) {
      System.out.println("NetworkConnection: Connected to network...");
    } else {
      setupServer(DEFAULT_PORT);
      System.out.println("NetworkConnection: Setup network server...");
    }
  }
  
  // TODO: this should handle several listeners!!!
  public void addPacketListener(PacketListener pl) {
    listener = pl;
  }

  private void setupServer(int port) {
    try {
      serverSocket = new ServerSocket(port);
      if (DEBUG) System.out.println("NetworkConnection: setup of server socket finished... ");
      new Thread(this).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public void run() {
    System.out.println("NetworkConnection: Accepting new connections...");
    while (true) {
      try {
        Socket s = serverSocket.accept();
        if (DEBUG) System.out.println("NetworkConnection: New connection accepted...");
        connections.add(new ConnectionThread(s));
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }
  
  // Data incoming from the network!!! - forward to radio and if server, to
  // all other nodes
  private void dataReceived(byte[] data, ConnectionThread source) {
    if (listener != null) {
      // Send this data to the transmitter in this node!
      listener.transmissionStarted();      
      listener.transmissionEnded(data);
    }

    // And if this is the server, propagate to the others
    if (serverSocket != null) {
      dataSent(data, source);
    }
  }
  

  // Data was sent from the radio in the node (or other node) and should
  // be sent out to other nodes!!!
  public void dataSent(byte[] receivedData) {
    dataSent(receivedData, null);
  }
 
  // Data was sent either from radio, or came from another "radio" -
  // and if so it should be propagated to all others.
  public void dataSent(byte[] receivedData, ConnectionThread source) {
    if (connections.size() > 0) {
      ConnectionThread[] cthr = connections.toArray(new ConnectionThread[connections.size()]);
      for (int i = 0; i < cthr.length; i++) {
        if (cthr[i].isClosed()) {
          connections.remove(cthr);
          // Do not write back to the source
        } else if (cthr[i] != source){
          try {
            cthr[i].output.write(receivedData, 0, receivedData.length);
            if (DEBUG) {
              System.out.println("NetworkConnection: wrote " + receivedData.length + " bytes");
              printPacket(receivedData);
            }
          } catch (IOException e) {
            e.printStackTrace();
            cthr[i].close();
          }
        }
      }
    }
  }

  private void printPacket(byte[] data) {
    for (int i = 0, len = data.length; i < len; i++) {
      System.out.print(Utils.hex8(data[i]) + " ");
    }
    System.out.println();
  }
  
  private boolean connect(int port) {
    try {
      Socket socket = new Socket("127.0.0.1", port);
      connections.add(new ConnectionThread(socket));
    } catch (UnknownHostException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return false;
    }    
    return true;
  }
  
  class ConnectionThread implements Runnable {
    Socket socket;
    DataInputStream input;
    OutputStream output;
    
    public ConnectionThread(Socket socket) throws IOException {
      this.socket = socket;
      input = new DataInputStream(socket.getInputStream());
      output = socket.getOutputStream();
      new Thread(this).start();
    }
    
    public void close() {
      try {
        input.close();
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      socket = null;
    }
    
    public boolean isClosed() {
      return socket == null;
    }
    
    @Override
    public void run() {
      if (DEBUG) System.out.println("NetworkConnection: Started connection thread...");
      try {
        while (socket != null) {
          int len = input.read();
          if (len > 0) {
            byte[] buffer = new byte[len + 1];
            buffer[0] = (byte) (len & 0xff);
            input.readFully(buffer, 1, len);
            if (DEBUG) {
              System.out.println("NetworkConnection: Read packet with " + len + " bytes");
              printPacket(buffer);
            }
            dataReceived(buffer, this);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
        close();
      }
    }
  }
}
