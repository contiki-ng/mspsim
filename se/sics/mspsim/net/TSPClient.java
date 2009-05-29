package se.sics.mspsim.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TSPClient implements NetworkInterface {

  public static final int DEFAULT_PORT = 3653;
  private static final byte[] VERSION = "VERSION=2.0.0\r\n".getBytes();
  private static final byte[] AUTH_PLAIN = "AUTHENTICATE PLAIN\r\b".getBytes();
  private static final byte[] AUTH_ANON = "AUTHENTICATE ANONYMOUS\r\b".getBytes();
  enum WriterState {WAIT, STARTED, CAPABILITIES_RECEIVED, AUTHENTICATE_REQ_OK,
    TUNNEL_CONF_RECEIVED, TUNNEL_UP};
  enum ReaderState {CAP_EXPECTED, AUTH_ACK_EXPECTED, AUTH_OK_EXPECTED, TUNNEL_CONF_EXPECTED, 
    TUNNEL_UP};

  private static final Pattern prefixPattern = 
    Pattern.compile("(?m).+?<prefix (.+?)>(.+?)</prefix>");
  private static final Pattern myIPPattern = 
    Pattern.compile("(?s).+?<client>.+?ipv6\">(.+?)</address>");

  private IPStack ipStack;
  
  WriterState writerState = WriterState.STARTED;
  ReaderState readerState = ReaderState.CAP_EXPECTED;
  
  DatagramSocket connection; //args[0], DEFAULT_PORT);
  
  DatagramPacket receiveP;
  InetAddress serverAddr;
  int seq = 0;
  
  private String user;
  private String password;
  private boolean userLoggedIn = false;
  
  public TSPClient(String host) throws SocketException, UnknownHostException {
    this(host, null, null);
  }
  
  public TSPClient(String host, String user, String password) throws SocketException, UnknownHostException {
    this.user = user;
    this.password = password;
 
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
  
  public String getName() {
    return "tsp";
  }
  
  public static TSPClient startTSPTunnel(IPStack ipStack, String server, String user, String password) {
    try {
      TSPClient tunnel = new TSPClient(server, user, password);
      tunnel.setIPStack(ipStack);
      tunnel.waitSetup();
      return tunnel;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  
  public void setIPStack(IPStack ipStack) {
    this.ipStack = ipStack;
  }
  
  public boolean isReady() {
    return writerState == WriterState.TUNNEL_UP;
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
         setReaderState(ReaderState.AUTH_OK_EXPECTED, WriterState.WAIT);
       } else {
         sendPacket(AUTH_PLAIN);
         setReaderState(ReaderState.AUTH_ACK_EXPECTED, WriterState.WAIT);
       }
       break;
     case AUTHENTICATE_REQ_OK:
       if (user == null || userLoggedIn) {
         sendTunnelReq();
         setReaderState(ReaderState.TUNNEL_CONF_EXPECTED, WriterState.WAIT);
       } else {
         // send login with user/pass!!!
         sendAuth();
         userLoggedIn = true;
         setReaderState(ReaderState.AUTH_OK_EXPECTED, WriterState.WAIT);
       }
       break;
     case TUNNEL_CONF_RECEIVED:
       String accept = "<tunnel action=\"accept\"></tunnel>\r\n";
       accept = "Content-length: " + accept.length() + "\r\n" + accept;
       sendPacket(accept.getBytes());
       System.out.println("*** Tunnel UP!");
       setReaderState(ReaderState.TUNNEL_UP, WriterState.TUNNEL_UP);
       notifyReady();
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
  
  private synchronized void notifyReady() {
    notifyAll();
  }

  private void sendAuth() throws IOException {
    String auth = "\0" + user + "\0" + password + "\r\n";
    sendPacket(auth.getBytes());
  }
  
  private void sendTunnelReq() throws IOException {
    InetAddress myAddr = InetAddress.getLocalHost();
    byte[] addr = myAddr.getAddress();
    String myAddress = String.format("%d.%d.%d.%d",
        addr[0] & 0xff, addr[1] & 0xff, addr[2] & 0xff, addr[3] & 0xff);
    String router = "";
    if (user != null) {
      router = "<router><prefix length=\"64\"/></router>";
    }
    String tunnelConf =
      "<tunnel action=\"create\" type=\"v6udpv4\"><client><address type=\"ipv4\">" +
      myAddress + "</address><keepalive interval=\"30\"></keepalive>" + router +
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
      case AUTH_ACK_EXPECTED:
        writerState = WriterState.AUTHENTICATE_REQ_OK;
        break;
      case AUTH_OK_EXPECTED:
        // Check if auth is really ok!!!
        writerState = WriterState.AUTHENTICATE_REQ_OK;
        break;
      case TUNNEL_CONF_EXPECTED:
        if (user != null) {
          Matcher m = prefixPattern.matcher(sData);
          if (m.find()) {
            System.out.println("Prefix: " + m.group(2) + " arg:" + m.group(1)); 
            if (ipStack != null) {
              byte[] prefix = getPrefix(m.group(2));
              /* this is hardcoded for 64 bits for now */
              ipStack.setPrefix(prefix, 64);
            }
          }
        } else {
          Matcher m = myIPPattern.matcher(sData);
          if (m.find()) {
            if (ipStack != null) {
              System.out.println("### Got IP address: " + m.group(1));
              byte[] prefix = getPrefix(m.group(1));
              byte[] macAddr = new byte[8];
              ipStack.makeLLAddress(prefix, macAddr);
              ipStack.setLinkLayerAddress(macAddr);
              ipStack.setIPAddress(prefix);
            }
          } else {
            System.out.println("NOT MATCH!!!");
          }
        }
        writerState = WriterState.TUNNEL_CONF_RECEIVED;
        break;
      case TUNNEL_UP:
        System.out.println("*** Tunneled packet received!!!");
        if (ipStack != null) {
          IPv6Packet packet = new IPv6Packet();
          packet.setBytes(data, 0, receiveP.getLength());
          packet.parsePacketData(packet);
          packet.netInterface = this;
          ipStack.receivePacket(packet);
        }
        break;
      }
    }
  }

  // handles format XXXX:XXXX:XXXX ...
  private byte[] getPrefix(String prefix) {
    prefix = prefix.trim();
    String[] parts = prefix.split(":");
    // each XXXX should be two bytes...
    byte[] prefixBytes = new byte[parts.length * 2];
    for (int i = 0; i < parts.length; i++) {
      System.out.println("## Parsing: " + parts[i]);
      int val = Integer.parseInt(parts[i], 16);
      prefixBytes[i * 2] = (byte) (val >> 8);
      prefixBytes[i * 2 + 1] = (byte) (val & 0xff);    
    }
    return prefixBytes;
  }

  private void sendPacket(byte[] packetData) throws IOException {
    byte[] pData;
    if (writerState != WriterState.TUNNEL_UP) {
      pData = new byte[8 + packetData.length];
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
    } else {
      pData = packetData;
    }
    DatagramPacket packet = new DatagramPacket(pData, pData.length, serverAddr, DEFAULT_PORT);
    connection.send(packet);
    System.out.println("Packet sent... " + pData.length + " => C:" +
        new String(packetData));
    
  }

  
  public void sendPacket(IPv6Packet packet) {
    byte[] data = packet.generatePacketData(packet);
    System.out.println("Sending IPv6Packet on tunnel: " + data);
    System.out.print("Packet: ");
    packet.printPacket(System.out);
    System.out.println();
    try {
      sendPacket(data);
    } catch (IOException e) {
      e.printStackTrace();
    }
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
    if (args.length == 1) {
      TSPClient tspc = new TSPClient(args[0]);
    } else if (args.length == 3) {
      TSPClient tspc = new TSPClient(args[0], args[1], args[2]);      
    }
  }

  public synchronized boolean waitSetup() {
    if (!isReady()) {
      try {
        wait(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return isReady();
  }
}
