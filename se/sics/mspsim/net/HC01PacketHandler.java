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
 * AbstractPacket
 *
 * Author  : Joakim Eriksson
 * Created : mar 2009
 * Updated : $Date:$
 *           $Revision:$
 */

package se.sics.mspsim.net;

import se.sics.mspsim.util.Utils;

public class HC01PacketHandler extends AbstractPacketHandler {
  public final static int IPHC_TTL_1 =  0x08;
  public final static int IPHC_TTL_64 = 0x10;
  public final static int IPHC_TTL_255 = 0x18;
  public final static int IPHC_TTL_I    = 0x00;

  /* Values of fields within the IPHC encoding second byte */
  public final static int IPHC_SAM_I =  0x00;
  public final static int IPHC_SAM_64 =  0x40;
  public final static int IPHC_SAM_16 =  0x80;
  public final static int IPHC_SAM_0 = 0xC0;
  public final static int IPHC_DAM_I = 0x00;
  public final static int IPHC_DAM_64 = 0x04;
  public final static int IPHC_DAM_16 = 0x08;
  public final static int IPHC_DAM_0 = 0x0C;

  public final static int NHC_UDP_ID =  0xF8;
  public final static int NHC_UDP_C =  0xFB;
  public final static int NHC_UDP_I = 0xF8;

  /* Min and Max compressible UDP ports */
  public final static int UDP_PORT_MIN = 0xF0B0;
  public final static int UDP_PORT_MAX = 0xF0BF;   /* F0B0 + 15 */
  
  public static final int HC01_DISPATCH = 0x03;

  public final static int PROTO_ICMP  = 1;
  public final static int PROTO_TCP   = 6;
  public final static int PROTO_UDP   = 17;
  public final static int PROTO_ICMP6 = 58;

  private static class AddrContext {
    int used;
    int number;
    byte[] prefix = new byte[8];
  }

  private AddrContext[] contexts = new AddrContext[4];

  public HC01PacketHandler() {
    // set-up some fake contexts just to get started...
    contexts[0] = new AddrContext();
    contexts[1] = new AddrContext();
    contexts[2] = new AddrContext();
    contexts[3] = new AddrContext();

    contexts[0].used = 1;
    contexts[0].number = 0;
    contexts[0].prefix[0] = (byte) 0xfe;
    contexts[0].prefix[1] = (byte) 0x80;

    contexts[1].used = 1;
    contexts[1].number = 1;
    contexts[1].prefix[0] = (byte) 0xaa;
    contexts[1].prefix[1] = (byte) 0xaa;
    
  }

  public void packetReceived(Packet container) {
    byte[] payload = container.getPayload();
    IPv6Packet packet = new IPv6Packet();
    container.setPayloadPacket(packet);
    packet.containerPacket = container;
    setPacketData(packet, payload, payload.length);
    dispatch(packet.nextHeader, packet);
  }

  public void sendPacket(Packet payload) {
  }
  
  private byte[] getLinkSourceAddress(AbstractPacket packet) {
    while (packet != null && !(packet instanceof IEEE802154Packet))
      packet = (AbstractPacket) packet.containerPacket;
    if (packet != null) {
      IEEE802154Packet ieeePacket = (IEEE802154Packet) packet;
      return ieeePacket.getSourceAddress();
    }
    return null;
  }

  private byte[] getLinkDestinationAddress(AbstractPacket packet) {
    while (packet != null && !(packet instanceof IEEE802154Packet))
      packet = (AbstractPacket) packet.containerPacket;
    if (packet != null) {
      IEEE802154Packet ieeePacket = (IEEE802154Packet) packet;
      return ieeePacket.getDestinationAddress();
    }
    return null;
  }

  
  public void setPacketData(IPv6Packet packet, byte[] data, int len) {
    /* first two is ... */
    int pos = 2;
    if ((data[0] & 0x40) == 0) {
      if ((data[0] & 0x80) == 0) {
        packet.version = (data[pos] & 0xf0) >> 4;
      packet.trafficClass = ((data[pos] & 0x0f)<<4) + ((data[pos + 1] & 0xff) >> 4);
      packet.flowLabel = (data[pos + 1] & 0x0f) << 16 + (data[pos + 2] & 0xff) << 8 +
      data[pos + 3] & 0xff;
      pos += 4;
      } else {
        packet.version = 6;
        packet.trafficClass = 0;
        packet.flowLabel = (data[pos] & 0x0f) << 16 
        + (data[pos + 1] & 0xff) << 8 + data[pos + 2] & 0xff;;
        pos += 3;
      }
    } else {
      packet.version = 6;
      packet.flowLabel = 0;
      if ((data[0] & 0x80) == 0) {
        packet.trafficClass = (data[pos] & 0xff);
        pos++;
      } else {
        packet.trafficClass = 0;
      }
    }
    
    /* next header not compressed -> get it */
    if ((data[0] & 0x20) == 0) {
      packet.nextHeader = data[pos++];
    }
    
    /* encoding of TTL */
    switch (data[0] & 0x18) {
    case IPHC_TTL_1:
      packet.hopLimit = 1;
      break;
    case IPHC_TTL_64:
      packet.hopLimit = 64;
      break;
    case IPHC_TTL_255:
      packet.hopLimit = 0xff;
      break;
    case IPHC_TTL_I:
      packet.hopLimit = data[pos++];
      break;
    }

    /* 0, 1, 2, 3 as source address ??? */
    int srcAddress = (data[1] & 0x30) >> 4;
    AddrContext context = lookupContext(srcAddress);
    switch (data[1] & 0xc0) {
    case IPHC_SAM_0:
      if(context == null) {
        System.out.println("sicslowpan uncompress_hdr: error context not found\n");
        return;
      }
      /* set hi address as prefix from context */
      System.arraycopy(context.prefix, 0, packet.sourceAddress, 0, 8);
      /* infer IID from L2 address */
      byte[] linkAddress = getLinkSourceAddress(packet);
      System.arraycopy(linkAddress, 0, packet.sourceAddress, 8, 8);
      /* TODO: clean autoconf stuff up */
      packet.sourceAddress[8] ^= 0x02;
      break;
    case IPHC_SAM_16:
      if((data[pos] & 0x80) == 0) {
        /* unicast address */
        if(context == null) {
          System.out.println("sicslowpan uncompress_hdr: error context not found\n");
          return;
        }
        /* set hi address as prefix from context */
        System.arraycopy(context.prefix, 0, packet.sourceAddress, 0, 8);
        /* copy 6 NULL bytes then 2 last bytes of IID */
        java.util.Arrays.fill(packet.sourceAddress, 8, 14, (byte)0);
        packet.sourceAddress[14] = data[pos];
        packet.sourceAddress[15] = data[pos + 1];
        pos += 2;
      } else {
        /* [ignore] multicast address check the 9-bit group-id is known */
        java.util.Arrays.fill(packet.sourceAddress, 0, 16, (byte)0);
        packet.sourceAddress[0] = (byte)0xff;
        packet.sourceAddress[1] = (byte)(((data[pos] & 0xff) >> 1) & 0x0f);
        packet.sourceAddress[15] = data[pos + 1];
        pos += 2;
      }
      break;
    case IPHC_SAM_64:
      if(context == null) {
        System.out.println("sicslowpan uncompress_hdr: error context not found\n");
        return;
      }
      /* copy prefix from context */
      System.arraycopy(context.prefix, 0, packet.sourceAddress, 0, 8);
      /* copy IID from packet */
      System.arraycopy(data, pos, packet.sourceAddress, 8, 8);
      pos += 8;
      break;
    case IPHC_SAM_I:
      /* copy whole address from packet */
      System.arraycopy(data, pos, packet.sourceAddress, 0, 16);
      pos += 16;
      break;
    }

    /* Destination address */
    context = lookupContext(data[2] & 0x03);

    switch(data[1] & 0x0C) {
    case IPHC_DAM_0:
      if(context == null) {
        System.out.println("sicslowpan uncompress_hdr: error context not found\n");
        return;
      }
      /* copy prefix from context */
      System.arraycopy(context.prefix, 0, packet.destAddress, 0, 8);
      /* infer IID from L2 address */
      /* figure out a way to pick this up from link-layer !!! */
      byte[] destAddress = getLinkDestinationAddress(packet);
      System.arraycopy(destAddress, 0, packet.destAddress, 8, 8);
      /* cleanup autoconf stuff later ... */
      packet.destAddress[8] ^= 0x02;
      break;
    case IPHC_DAM_16:
      if((data[pos] & 0x80) == 0) {
        /* unicast address */
        if(context == null) {
          System.out.println("sicslowpan uncompress_hdr: error context not found\n");
          return;
        }
        System.arraycopy(context.prefix, 0, packet.destAddress, 0, 8);
        /* copy 6 NULL bytes then 2 last bytes of IID */
        packet.destAddress[14] = data[pos];
        packet.destAddress[15] = data[pos + 1];
        pos += 2;
      } else {
        /* [ignore] multicast address check the 9-bit group-id is known */
        System.out.println("*** Multicast address!!! HC01");
        java.util.Arrays.fill(packet.destAddress, 0, 16, (byte)0);
        packet.destAddress[0] = (byte)0xff; 
        packet.destAddress[1] = (byte)(((data[pos] & 0xff) >> 1) & 0x0F);
        packet.destAddress[15] = data[pos + 1];
        pos += 2;
      }
      break;
    case IPHC_DAM_64:
      if(context == null) {
        System.out.println("sicslowpan uncompress_hdr: error context not found\n");
        return;
      }
      /* copy prefix from context */
      System.arraycopy(context.prefix, 0, packet.destAddress, 0, 8);
      /* copy IID from packet */
      System.arraycopy(data, pos, packet.destAddress, 8, 8);
      pos += 8;
      break;
    case IPHC_DAM_I:
      /* copy whole address from packet */
      System.arraycopy(data, pos, packet.destAddress, 0, 16);
      pos += 16;
      break;
    }

    if ((data[0] & 0x20) != 0) {
      /* The next header is compressed, NHC is following */
      if ((data[pos] & 0xfc) == NHC_UDP_ID) {
        packet.nextHeader = PROTO_UDP;
        int srcPort = 0;
        int destPort = 0;
        int checkSum = 0;
        switch(data[pos] & 0xff) {
        case NHC_UDP_C:
          /* 1 byte for NHC, 1 byte for ports, 2 bytes chksum */
          srcPort = UDP_PORT_MIN + ((data[pos + 1] & 0xff) >> 4);
          destPort = UDP_PORT_MIN + (data[pos + 1] & 0x0F);
          checkSum = ((data[pos + 2] & 0xff) << 8) + (data[pos + 3] & 0xff);
          pos += 4;
          break;
        case NHC_UDP_I:
          /* 1 byte for NHC, 4 byte for ports, 2 bytes chksum */
          srcPort = ((data[pos + 1] & 0xff)<< 8) + (data[pos + 2] & 0xff);
          destPort = ((data[pos + 3] & 0xff)<< 8) + (data[pos + 4] & 0xff);
          checkSum = ((data[pos + 5] & 0xff)<< 8) + (data[pos + 6] & 0xff);
          pos += 7;
          break;
        default:
          System.out.println("sicslowpan uncompress_hdr: error unsupported UDP compression\n");
        return;
        }
        System.out.println("DestPort: " + destPort);
        System.out.println("SourcePort: " + srcPort);
        System.out.println("Checksum: " + srcPort);
      }
    }

    boolean frag = false;
    /* fragment handling ... */
    if (!frag) {
      /* this does not handle the UDP header compression yet... */
      int plen = len - pos;
      packet.setPayload(data, pos, plen);
    } else {
    }

    System.out.println("Encoding 0: " + Utils.hex8(data[0]) +
        " Encoding 1: " + Utils.hex8(data[1]));

    System.out.println("TTL: " + packet.hopLimit);
    System.out.print("Src Addr: ");
    IPv6Packet.printAddress(System.out, packet.sourceAddress);
    System.out.print(" Dest Addr: ");
    IPv6Packet.printAddress(System.out, packet.destAddress);
    System.out.println();
    // packet.setPayload(data, 40, ???);
  }

  private AddrContext lookupContext(int index) {
    if (index < contexts.length)
      return contexts[index];
    return null;
  }
}