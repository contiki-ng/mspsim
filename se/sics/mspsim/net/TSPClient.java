package se.sics.mspsim.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TSPClient {

  public static final int DEFAULT_PORT = 3653;
  private static final byte[] VERSION = "VERSION=2.0.0\r\n".getBytes();
  private static final byte[] AUTH_PLAIN = "AUTHENTICATE PLAIN\r\b".getBytes();
  private static final byte[] AUTH_ANON = "AUTHENTICATE ANONYMOUS\r\b".getBytes();
  enum WriterState {WAIT, STARTED, CAPABILITIES_RECEIVED, AUTHENTICATE_REQ_OK,
    TUNNEL_CONF_RECEIVED, TUNNEL_UP};
  enum ReaderState {CAP_EXPECTED, AUTH_OK_EXPECTED, TUNNEL_CONF_EXPECTED, 
    TUNNEL_UP};

  private static final Pattern prefixPattern = 
    Pattern.compile("(?m).+?<prefix (.+?)>(.+?)</prefix>");
    
    
  WriterState writerState = WriterState.STARTED;
  ReaderState readerState = ReaderState.CAP_EXPECTED;
  
  DatagramSocket connection; //args[0], DEFAULT_PORT);
  
  DatagramPacket receiveP;
  InetAddress serverAddr;
  int seq = 0;
  
  private String user;
  private String password;
  
  public TSPClient(String host) throws SocketException, UnknownHostException {
    connection = new DatagramSocket();
    serverAddr = InetAddress.getByName(host);
    //connection.connect(serverAddr, DEFAULT_PORT);
    receiveP = new DatagramPacket(new byte[1280], 1280);
    
    Runnable writer = new Runnable() {
      public void run() { 
        try {
          writer();
        } catch (IOException e) {
          e.printStackTrace();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    };
    Runnable reader = new Runnable() {
      public void run() { 
        try {
          reader();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
    
    new Thread(writer).start(); 
    new Thread(reader).start(); 
  }
  
  int wWait = 0;
  private void writer() throws IOException, InterruptedException {
   System.out.println("Writer started. sending version...");
   while (true) {
     switch (writerState) {
     case STARTED:
       sendPacket(VERSION);
       setReaderState(ReaderState.CAP_EXPECTED, WriterState.WAIT);
       break;
     case WAIT:
       Thread.sleep(100);
       wWait++;
       if (wWait > 10) {
         System.out.println("Waited for " + wWait);
       }
       break;
     case CAPABILITIES_RECEIVED:
       System.out.println("Writer: sending AUTH");
       if (user == null) {
         sendPacket(AUTH_ANON);
       } else {
         sendPacket(AUTH_PLAIN);
       }
       setReaderState(ReaderState.AUTH_OK_EXPECTED, WriterState.WAIT);
       break;
     case AUTHENTICATE_REQ_OK:
       if (user == null) {
         sendTunnelReq();
       } else {
         // send login with user/pass!!!
       }
       setReaderState(ReaderState.TUNNEL_CONF_EXPECTED, WriterState.WAIT);
       break;
     case TUNNEL_CONF_RECEIVED:
       String accept = "<tunnel action=\"accept\"></tunnel>\r\n";
       accept = "Content-length: " + accept.length() + "\r\n" + accept;
       sendPacket(accept.getBytes());
       System.out.println("*** Tunnel UP!");
       setReaderState(ReaderState.TUNNEL_UP, WriterState.TUNNEL_UP);
       break;
     case TUNNEL_UP:
       /* all ok - do nothing but sleep.*/
       Thread.sleep(100);
       break;
     default:
       System.out.println("In mode: " + writerState);
       Thread.sleep(1000);
     }
   }
  }

  private void sendTunnelReq() throws IOException {
    InetAddress myAddr = InetAddress.getLocalHost();
    byte[] addr = myAddr.getAddress();
    String myAddress = String.format("%d.%d.%d.%d",
        addr[0] & 0xff, addr[1] & 0xff, addr[2] & 0xff, addr[3] & 0xff);
    String tunnelConf =
      "<tunnel action=\"create\" type=\"v6udpv4\"><client><address type=\"ipv4\">" +
      myAddress + "</address><keepalive interval=\"30\"></keepalive>" +
      "</client></tunnel>\r\n";
    tunnelConf = "Content-length: " + tunnelConf.length() + "\r\n" +
      tunnelConf;
    sendPacket(tunnelConf.getBytes());
  }
  
  private void setReaderState(ReaderState rs, WriterState ws) {
    readerState = rs;
    writerState = ws;
    wWait = 0;
  }
  
  private void reader() throws IOException {
    while(true) {
      System.out.println("Receiving packet...");
      connection.receive(receiveP);
      System.out.println("Packet received: " + receiveP.getLength());
      byte[] data = receiveP.getData();
      for (int i = 0, n = receiveP.getLength(); i < n; i++) {
        if (i < 8 || writerState == WriterState.TUNNEL_UP) {
          System.out.printf("%02x", data[i]);
        } else {
          System.out.print((char) data[i]);
        }
      }
      String sData = new String(data, 8, receiveP.getLength() - 8);
      String[] parts = sData.split("\n");
      if (parts.length > 1) {
        System.out.println("Response size: " + parts[0]);
        System.out.println("Response code: " + parts[1]);
      }
      switch (readerState) {
      case CAP_EXPECTED:
        writerState = WriterState.CAPABILITIES_RECEIVED;
        break;
      case AUTH_OK_EXPECTED:
        writerState = WriterState.AUTHENTICATE_REQ_OK;
        break;
      case TUNNEL_CONF_EXPECTED:
        if (user != null) {
          Matcher m = prefixPattern.matcher(sData);
          if (m.find()) {
            System.out.println("Prefix: " + m.group(2) + " arg:" + m.group(1)); 
          }
        }
        writerState = WriterState.TUNNEL_CONF_RECEIVED;
        break;
      case TUNNEL_UP:
        System.out.println("*** Tunneled packet received!!!");
        break;
      }
    }
  }
  

  private void sendPacket(byte[] packetData) throws IOException {
    byte[] pData = new byte[8 + packetData.length];
    pData[0] = (byte) (0xf0 | (seq >>24) & 0xf);
    pData[1] = (byte) ((seq >> 16) & 0xff);
    pData[2] = (byte) ((seq >> 8) & 0xff);
    pData[3] = (byte) (seq & 0xff);
    
    long time = System.currentTimeMillis() / 1000;
    pData[4] = (byte) ((time >> 24) & 0xff);
    pData[5] = (byte) ((time >> 16) & 0xff);
    pData[6] = (byte) ((time >> 8) & 0xff);
    pData[7] = (byte) ((time >> 0) & 0xff);
    seq++;

    System.arraycopy(packetData, 0, pData, 8, packetData.length);

    DatagramPacket packet = new DatagramPacket(pData, pData.length, serverAddr, DEFAULT_PORT);
    connection.send(packet);
    System.out.println("Packet sent... " + pData.length + " => C:" +
        new String(packetData));
    
  }
  
  
  public static void main(String[] args) throws UnknownHostException, IOException {
//    Pattern pattern = Pattern.compile("(?m).+?<server>(.+?)</server>.+?");
//    String data = "<tunnel action=\"info\" type=\"v6udpv4\" lifetime=\"604800\">" +
//    "<server>" +
//    "<address type=\"ipv4\">81.171.72.11</address>" +
//    "<address type=\"ipv6\">2001:05c0:1400:000b:0000:0000:0000:1634</address>" +
//    "</server>" +
//    "<client><address type=\"ipv4\">85.228.25.3</address>" +
//    "<address type=\"ipv6\">2001:05c0:1400:000b:0000:0000:0000:1635</address>" +
//    "<address type=\"dn\">Joakim.broker.freenet6.net</address>" +
//    "<router>" +
//    "<prefix length=\"56\">2001:05c0:1501:e300:0000:0000:0000:0000</prefix>" +
//    "</router>" +
//    "<keepalive interval=\"30\">" +
//    "<address type=\"ipv6\">2001:05c0:1400:000b:0000:0000:0000:1634</address>" +
//    "</keepalive></client></tunnel>";

//    Matcher m = pattern.matcher(data);
//    if (m.find()) {
//     System.out.println("Match: " + m.group(1)); 
//    } else {
//     System.out.println("No match"); 
//    }
//    
    
    TSPClient tspc = new TSPClient(args[0]);
  }
}
