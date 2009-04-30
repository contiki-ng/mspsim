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
 * Original Authors (Contiki Code): 
 * 
 * Created : mar 2009
 * Updated : $Date:$
 *           $Revision:$
 */

package se.sics.mspsim.net;
import se.sics.mspsim.util.Utils;

public class HC01PacketHandler extends AbstractPacketHandler {
  /*
   * Values of fields within the IPHC encoding first byte
   * (C stands for compressed and I for inline)
   */
  public final static int IPHC_TC_C = 0x80;
  public final static int IPHC_VF_C = 0x40;
  public final static int IPHC_NH_C = 0x20;  
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

  /* Link local context number */
  public final static int IPHC_ADDR_CONTEXT_LL = 0;
  /* 16-bit multicast addresses compression */
  public final static int IPHC_MCAST_RANGE = 0xA0;
  
  /* Min and Max compressible UDP ports */
  public final static int UDP_PORT_MIN = 0xF0B0;
  public final static int UDP_PORT_MAX = 0xF0BF;   /* F0B0 + 15 */
  
  public static final int HC01_DISPATCH = 0x03;

  /* move these to IPv6 Packet !! */
  public final static int PROTO_ICMP  = 1;
  public final static int PROTO_TCP   = 6;
  public final static int PROTO_UDP   = 17;
  public final static int PROTO_ICMP6 = 58;
  
  private static class AddrContext {
    int used;
    int number;
    byte[] prefix = new byte[8];
    
    public boolean matchPrefix(byte[] address) {
      for (int i = 0; i < prefix.length; i++) {
        if (prefix[i] != address[i])
          return false;
      }
      return true;
    }
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


  /**
   * \brief check whether we can compress the IID in
   * address to 16 bits.
   * This is used for unicast addresses only, and is true
   * if first 49 bits of IID are 0
   * @return 
   */
  private boolean is16bitCompressable(byte[] address) {
    return ((address[8] | address[9] | address[10] | address[11] |
        address[12] | address[13]) == 0) &&
        (address[14] & 0x80) == 0;
  }
     
  /**
   * \brief check whether the 9-bit group-id of the
   * compressed multicast address is known. It is true
   * if the 9-bit group is the all nodes or all routers
   * group.
   * \param a is typed u8_t *
   */
//  #define sicslowpan_is_mcast_addr_decompressable(a) \
//     (((*a & 0x01) == 0) &&                           \
//      ((*(a + 1) == 0x01) || (*(a + 1) == 0x02)))

  /**
   * \brief check whether the 112-bit group-id of the
   * multicast address is mappable to a 9-bit group-id
   * It is true if the group is the all nodes or all
   * routers group.
  */
//  #define sicslowpan_is_mcast_addr_compressable(a) \
//    ((((a)->u16[1]) == 0) &&                       \
//     (((a)->u16[2]) == 0) &&                       \
//     (((a)->u16[3]) == 0) &&                       \
//     (((a)->u16[4]) == 0) &&                       \
//     (((a)->u16[5]) == 0) &&                       \
//     (((a)->u16[6]) == 0) &&                       \
//     (((a)->u8[14]) == 0) &&                       \
//     ((((a)->u8[15]) == 1) || (((a)->u8[15]) == 2)))
  
  
  
  public void packetReceived(Packet container) {
    byte[] payload = container.getPayload();
    IPv6Packet packet = new IPv6Packet();
    container.setPayloadPacket(packet);
    packet.containerPacket = container;
    setPacketData(packet, payload, payload.length);
    dispatch(packet.nextHeader, packet);
  }

  /* this is used for sending packets! */
  /* we need lots of more info here !!! target, reply-of-packet, etc */
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

  /* HC01 header compression from 40 bytes to less... */
  public byte[] getPacketData(IPv6Packet packet) {
    int enc1 = 0, enc2 = 0;
    byte[] data = new byte[40];
    int pos = 3;
    
    if (packet.flowLabel == 0) {
      /* compress version and flow label! */
      enc1 |= IPHC_VF_C;
    }
    if (packet.trafficClass == 0) {
      enc1 |=  IPHC_TC_C;
    }

    /* write version and flow if needed */ 
    if ((enc1 & IPHC_VF_C) == 0) {
      pos += packet.writeVFlow(data, pos);
    }
    /* write traffic class if needed */
    if ((enc1 & IPHC_TC_C) == 0) {
      data[pos++] = (byte) (packet.trafficClass & 0xff);
    }
    
    /* Note that the payload length is always compressed */

    /* TODO: compress UDP!!! */
    data[pos++] = (byte) (packet.nextHeader & 0xff);

    switch (packet.hopLimit) {
    case 1:
      enc1 |= IPHC_TTL_1;
      break;
    case 64:
      enc1 |= IPHC_TTL_64;
      break;
    case 255:
      enc1 |= IPHC_TTL_255;
      break;
    default:
      data[pos++] = (byte) (packet.hopLimit & 0xff);
    }
    
    int context;
    if ((context = lookupContext(packet.sourceAddress)) != -1) {
      /* elide the prefix */
      enc2 |= context << 4;
      if (packet.isSourceMACBased()) {
        /* elide the IID */
        enc2 |= IPHC_SAM_0;
      } else if (is16bitCompressable(packet.sourceAddress)){
        enc2 |= IPHC_SAM_16;
        data[pos++] = packet.sourceAddress[14];
        data[pos++] = packet.sourceAddress[15];
      } else {
        enc2 |= IPHC_SAM_64;
        System.arraycopy(packet.sourceAddress, 8, data, pos, 8);
        pos += 8;
      }
    } else {
      enc2 |= IPHC_SAM_I;
      System.arraycopy(packet.sourceAddress, 0, data, pos, 16);
      pos += 16;
    }
    
    /* destination  compression */
    if(packet.isMulticastDestination()) {
      /* Address is multicast, try to compress */
      if(isMulticastCompressable(packet.destAddress)) {
        enc2 |= IPHC_DAM_16;
        /* 3 first bits = 101 */
        data[pos] = (byte) IPHC_MCAST_RANGE;
        /* bits 3-6 = scope = bits 8-11 in 128 bits address */
        data[pos++] |= (packet.destAddress[1] & 0x0F) << 1;
        /*
         * bits 7 - 15 = 9-bit group
         * We just copy the last byte because it works
         * with currently supported groups
         */
        data[pos++] = packet.destAddress[15];
      } else {
        /* send the full address */
        enc2 |= IPHC_DAM_I;
        System.arraycopy(packet.destAddress, 0, data, pos, 16);
        pos += 16;
      }
    } else {
      /* Address is unicast, try to compress */
      if((context = lookupContext(packet.destAddress)) != -1) {
        /* elide the prefix */        
        enc2 |= context;
        if(packet.isDestinationMACBased()) {
          /* elide the IID */
          enc2 |= IPHC_DAM_0;
        } else {
          if(is16bitCompressable(packet.destAddress)) {
            /* compress IID to 16 bits */
            enc2 |= IPHC_DAM_16;
            data[pos++] = packet.destAddress[14];
            data[pos++] = packet.destAddress[15];
          } else {
            /* do not compress IID */
            enc2 |= IPHC_DAM_64;
            System.arraycopy(data, pos, packet.destAddress, 8, 8);
            pos += 8;
          }
        }
      } else {
        /* send the full address */
        enc2 |= IPHC_DAM_I;
        System.arraycopy(data, pos, packet.destAddress, 0, 16);
        pos += 16;
      }
    }
    
   // uncomp_hdr_len = UIP_IPH_LEN;
   // TODO: add udp header compression!!! 
    
    data[0] = HC01_DISPATCH;
    data[1] = (byte) (enc1 & 0xff);
    data[2] = (byte) (enc2 & 0xff);
    
    System.out.println("HC01 Header compression: size " + pos);
    
    byte[] dataPacket = new byte[pos];
    System.arraycopy(data, 0, dataPacket, 0, pos);
    return dataPacket;
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
        System.out.println("*** Multicast address!!! HC01: " + data[pos] + "," + data[pos + 1]);
        java.util.Arrays.fill(packet.destAddress, 0, 16, (byte)0);
        packet.destAddress[0] = (byte) 0xff; 
        packet.destAddress[1] = (byte)(((data[pos] & 0xff) >> 1) & 0x0F);
        packet.destAddress[15] = (byte) (data[pos + 1] & 0xff);
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
        System.out.println("HC01: Next header UDP!");
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
  
  private boolean isMulticastCompressable(byte[] address) {
    return false;
  }
  
  
  private AddrContext lookupContext(int index) {
    if (index < contexts.length)
      return contexts[index];
    return null;
  }
  
  private int lookupContext(byte[] address) {
    for (int i = 0; i < contexts.length; i++) {
      if (contexts[i] != null && contexts[i].matchPrefix(address)) {
        return i;
      }
    }
    return -1;
  }
}
