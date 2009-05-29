/**
 * Copyright (c) 2009, Swedish Institute of Computer Science.
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
 * IPStack
 * 
 * An IPStack with configurable link layer, etc.
 *
 * Author  : Joakim Eriksson
 * Created : mar 2009
 * Updated : $Date:$
 *           $Revision:$
 */

package se.sics.mspsim.net;

import se.sics.mspsim.util.Utils;

public class IPStack {

  public static final byte[] ALL_NODES = {(byte) 0xff, 0x02, 0, 0, 0, 0, 0, 0,
       0, 0, 0, 0, 0, 0, 0, 1};
  public static final byte[] ALL_ROUTERS = {(byte) 0xff, 0x02, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 2};
  public static final byte[] UNSPECIFIED = {(byte) 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0};
  
  byte[] prefix = null;
  int prefixSize = 0;
  
  byte[] myIPAddress = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
      0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x00};
  byte[] myLocalIPAddress = new byte[] { (byte)0xfe, (byte)0x80,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x00};
  
  byte[] myLocalSolicited = new byte[] {(byte) 0xff, 0x02, 0, 0,
      0, 0, 0, 0,  0, 0, 0, 0x01,  (byte) 0xff, 0, 0, 0};

  /* currently assumes only one link-layer and one address */
  byte[] myLinkAddress = new byte[] {0x00, 0x12, 0x75, 0x04, 0x05, 0x06, 0x07, 0x08};

  
  
  byte[] linkBroadcast = new byte[] {(byte) 0xff, (byte) 0xff};
  
  private PacketHandler linkLayerHandler;
  private IPPacketer defaultPacketer = new HC01Packeter();
  private ICMP6PacketHandler icmp6Handler;

  /* is router -> router behavior */
  private boolean isRouter = false;

  private NetworkInterface tunnel;
  //TSPClient
  /* this needs to be generalized later... and down to lowpan too... */
  //private HC01Packeter ipPacketer = new HC01Packeter();

  private NeighborTable neighborTable = new NeighborTable();
  private NetworkEventListener networkEventListener;
  
  // TODO: read from configfile...

  public IPStack() {
    icmp6Handler = new ICMP6PacketHandler(this);
    prefix = new byte[] {(byte) 0xaa, (byte)0xaa, 0, 0, 0, 0, 0, 0};
    prefixSize = 64; /* link size */
    configureIPAddress();
    new NeighborManager(this, neighborTable);
  }

  public NeighborTable getNeighborTable() {
    return neighborTable;
  }
  
  public void setLinkLayerHandler(PacketHandler handler) {
    linkLayerHandler = handler;
  }

  public void setNetworkEventListener(NetworkEventListener li) {
    networkEventListener = li;
  }
  
  public void setTunnel(NetworkInterface tunnel) {
    this.tunnel = tunnel;
  }
  
  public void setPrefix(byte[] prefix, int size) {
    this.prefix = prefix;
    prefixSize = size;
    configureIPAddress();
  }
  
  public boolean isOnLink(byte[] address) {
    /* bc or link local */
    if (address[0] == ((byte) 0xff) || (address[0] == ((byte) 0xfe) &&
        address[1] == ((byte)0x80))) {
      return true;
    }

    /* unspecified - on link ?? */
    if (Utils.equals(UNSPECIFIED, address)) return true;
    /* prefix match? */
    for (int i = 0; i < prefixSize / 8; i++) {
      if (address[i] != prefix[i]) return false;
    }
    return true;
  }
  
  public void configureIPAddress() {
    if (prefix != null) {
      System.arraycopy(prefix, 0, myIPAddress, 0, prefixSize / 8);
    }
    for (int i = 0; i < 8; i++) {
      myLocalIPAddress[8 + i] = myIPAddress[8 + i] = myLinkAddress[i];
    }
    /* autoconfig ?? */
    myLocalIPAddress[8] = myIPAddress[8] = (byte) (myIPAddress[8] ^ 0x02);

    /* create multicast solicited address */
    for (int i = 13; i < 16; i++) {
      myLocalSolicited[i] = myIPAddress[i];
    }
    
    System.out.print("***** Configured IP address: ");
    IPv6Packet.printAddress(System.out, myIPAddress);
    System.out.println();
    System.out.print("***** Configured Local IP address: ");
    IPv6Packet.printAddress(System.out, myLocalIPAddress);
    System.out.println();
    System.out.print("***** Configured Solicited IP address: ");
    IPv6Packet.printAddress(System.out, myLocalSolicited);
    System.out.println();
  }
  
  private boolean findRoute(IPv6Packet packet) {
    // this does not do anything yet... we assume that the low 8 byte is MAC
    if (packet.getLinkDestination() == null) {
      /* find a MAC address for this packets destination... */
      byte[] destAddr = packet.getDestinationAddress();
      /* is it a bc to all nodes? */
      if (Utils.equals(ALL_ROUTERS, destAddr)) {
        packet.setAttribute("link.destination", linkBroadcast);
      } else if (Utils.equals(ALL_NODES, destAddr)) {
        packet.setAttribute("link.destination", linkBroadcast);
      } else {
        byte[] destMAC;
        Neighbor n = neighborTable.getNeighbor(destAddr);
        if (n == null) {
          if (neighborTable.getDefrouter() != null) {
            destMAC = neighborTable.getDefrouter().linkAddress;
          } else {
          /* fill the array with a autoconf address ... */
          destMAC = new byte[8];
          makeLLAddress(destAddr, destMAC);
          }
        } else {
          destMAC = n.linkAddress;
        }
        packet.setAttribute("link.destination", destMAC);
      }
    }
    packet.setAttribute("link.source", myLinkAddress);
    return true;
  }

  public void makeLLAddress(byte[] ipAddr, byte[] macAddr) {
    for (int i = 0; i < macAddr.length; i++) {
      macAddr[i] = ipAddr[8 + i];
    }
    macAddr[0] = (byte) (macAddr[0] ^ 0x02);
  }
  
  /* send a packet - can be bound for specific interface */
  public void sendPacket(IPv6Packet packet, NetworkInterface nIf) {
    /* find route checks if there are link addr, and otherwise sets them */
    if (nIf == linkLayerHandler ||
        (nIf == null && isOnLink(packet.getDestinationAddress()))) {
      if (findRoute(packet)) {
        linkLayerHandler.sendPacket(packet);
      }
    } else {
      System.out.println("*** Should go out on tunnel: " + tunnel);
      System.out.print("MyAddress: ");
      IPv6Packet.printAddress(System.out, myIPAddress);
      System.out.print(", Dest: ");
      IPv6Packet.printAddress(System.out, packet.getDestinationAddress());
      if (tunnel != null && tunnel.isReady()) {
        tunnel.sendPacket(packet);
      }
    }
  }
  
  public void receivePacket(IPv6Packet packet) {
    System.out.println("IPv6 packet received!");
    packet.printPacket(System.out);

    if (isForMe(packet.getDestinationAddress())){
      System.out.println("#### PACKET FOR ME!!! " + packet.getDispatch());
      switch (packet.nextHeader) {
      case ICMP6Packet.DISPATCH:
        icmp6Handler.handlePacket(packet);
        if (networkEventListener != null) {
          networkEventListener.packetHandled(packet);
        }
        break;
      case UDPPacket.DISPATCH:
        // TODO: move to HC01 compression handler... => generate raw UDP
        if (packet.getIPPayload() != null) {
          packet.getIPPayload().printPacket(System.out);
        } else {
          UDPPacket p = new UDPPacket();
          p.parsePacketData(packet);
          p.printPacket(System.out);
          packet.setIPPayload(p);
        }
        if (networkEventListener != null) {
          System.out.println("UDP: Notifying event listener...");
          networkEventListener.packetHandled(packet);
        }
        break;
      }
    } else if (!isOnLink(packet.getDestinationAddress()) &&
        packet.netInterface != tunnel) {
      System.out.println("**** Should go out on tunnel!!!!" + tunnel);
      if (packet.ipPayload == null) {
        packet.setIPPayload(new BytePayload(packet));
      }
      /* will this work ??? */
      System.out.print("MyAddress: ");
      IPv6Packet.printAddress(System.out, myIPAddress);
      System.out.print(", Dest: ");
      IPv6Packet.printAddress(System.out, packet.getDestinationAddress());
      if (tunnel != null && tunnel.isReady()) {
        tunnel.sendPacket(packet);
      }
    } else if (packet.netInterface != linkLayerHandler) {
      /* Can not be from link layer (default) -- */
      /* if HC01 - we need to handle UDP at least... */
      System.out.println("#### PACKET FOR: " + packet.getDestinationAddress() + " sent to link");
      if (packet.ipPayload == null) {
        packet.setIPPayload(new BytePayload(packet));
        if (findRoute(packet)) {
          linkLayerHandler.sendPacket(packet);
        }
      }
    } else {
      System.out.println("#### PACKET ignored...");
    }
  }

  /* is the packet for me ? */
  private boolean isForMe(byte[] address) {
    System.out.print("=== is for me? ");
    IPv6Packet.printAddress(System.out, address);
    if (Utils.equals(myIPAddress, address) ||
        Utils.equals(myLocalIPAddress, address) ||
        Utils.equals(myLocalSolicited, address)) return true;
    if (isRouter && Utils.equals(ALL_ROUTERS, address)) return true;
    if (Utils.equals(ALL_NODES, address)) return true;
    if (Utils.equals(UNSPECIFIED, address)) return true;
    return false;
  }
  
  public void setLinkLayerAddress(byte[] addr) {
    myLinkAddress = addr;
    configureIPAddress();
  }
  
  public void setIPAddress(byte[] addr) {
    myIPAddress = addr;
  }
  
  public byte[] getLinkLayerAddress() {
    return myLinkAddress;
  }
  
  public byte[] getIPAddress() {
    return myIPAddress;
  }

  public IPPacketer getPacketer() {
    return getDefaultPacketer();
  }

  public void setDefaultPacketer(IPPacketer defaultPacketer) {
    this.defaultPacketer = defaultPacketer;
  }

  public IPPacketer getDefaultPacketer() {
    return defaultPacketer;
  }
  
  public void setRouter(boolean isRouter) {
    this.isRouter = isRouter;
  }
  
  public boolean isRouter() {
    return isRouter ;
  }
  
}