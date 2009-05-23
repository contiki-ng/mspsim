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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.util.Utils;

public class IPStack {

  public static final byte[] ALL_NODES = {(byte) 0xff, 0x02, 0, 0, 0, 0, 0, 0,
       0, 0, 0, 0, 0, 0, 0, 1};
  public static final byte[] ALL_ROUTERS = {(byte) 0xff, 0x02, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 1};
  
  byte[] prefix = null;
  int prefixSize = 0;
  
  byte[] myIPAddress = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
      0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x00};
  byte[] myLocalIPAddress = new byte[] { (byte)0xfe, (byte)0x80,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, 0x00};
  /* currently assumes only one link-layer and one address */
  byte[] myLinkAddress = new byte[] {0x00, 0x12, 0x75, 0x04, 0x05, 0x06, 0x07, 0x08};
  
  byte[] linkBroadcast = new byte[] {(byte) 0xff, (byte) 0xff};
  
  private PacketHandler linkLayerHandler;
  private IPPacketer defaultPacketer = new HC01Packeter();
  private ICMP6PacketHandler icmp6Handler;

  /* is router -> router behavior */
  private boolean isRouter = true;
  
  /* this needs to be generalized later... and down to lowpan too... */
  //private HC01Packeter ipPacketer = new HC01Packeter();
  
  public IPStack() {
    icmp6Handler = new ICMP6PacketHandler(this);
    prefix = new byte[] {(byte) 0xaa, (byte)0xaa, 0, 0, 0, 0, 0, 0};
    prefixSize = 64; /* link size */
    configureIPAddress();
  }

  public void setLinkLayerHandler(PacketHandler handler) {
    linkLayerHandler = handler;
  }

  public void setPrefix(byte[] prefix, int size) {
    this.prefix = prefix;
    prefixSize = size;
    configureIPAddress();
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
  }
  
  private boolean findRoute(IPv6Packet packet) {
    // this does not do anything yet... we assume that the low 8 byte is MAC
    if (packet.getLinkDestination() == null) {
      /* find a MAC address for this packets destination... */
      byte[] destAddr = packet.getDestinationAddress();
      /* is it a bc to all nodes? */
      if (Arrays.equals(ALL_NODES, destAddr)) {
        packet.setAttribute("link.destination", linkBroadcast);
      } else {
        byte[] destMAC = new byte[8];
        /* fill the array with a autoconf address ... */
        makeLLAddress(destAddr, destMAC);
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
  
  public void sendPacket(IPv6Packet packet) {
    /* find route checks if there are link addr, and otherwise sets them */
    if (findRoute(packet)) {
      linkLayerHandler.sendPacket(packet);
    }
  }
  
  public void receivePacket(IPv6Packet packet) {
    System.out.println("IPv6 packet received!!!");
    packet.printPacket(System.out);

    switch (packet.nextHeader) {
    case ICMP6Packet.DISPATCH:
      icmp6Handler.handlePacket(packet);
      break;
    }
  }
    
  public void setLinkLayerAddress(byte[] addr) {
    myLinkAddress = addr;
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
  
  public static void main(String[] args) throws IOException {
    IEEE802154Handler ieeeHandler = new IEEE802154Handler();
    IPStack ipStack = new IPStack();
    LoWPANHandler lowpanHandler = new LoWPANHandler(ipStack);
    ieeeHandler.addUpperLayerHandler(0, lowpanHandler);
    lowpanHandler.setLowerLayerHandler(ieeeHandler);
    ipStack.setLinkLayerHandler(lowpanHandler);

    String line;
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    while((line = reader.readLine()) != null) {
      if (line.length() > 10) {
        System.out.println("---------- packet read ------");
        byte[] data = Utils.hexconv(line);
        Packet packet = new Packet();
        packet.setBytes(data, 0, data.length);
        ieeeHandler.packetReceived(packet);
      }
    }
  }

  public boolean isRouter() {
    return isRouter ;
  }
}
