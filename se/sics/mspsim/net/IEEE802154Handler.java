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
 *
 * Author  : Joakim Eriksson
 * Created :  mar 2009
 * Updated : $Date:$
 *           $Revision:$
 */

package se.sics.mspsim.net;

import java.io.PrintStream;

public class IEEE802154Handler extends AbstractPacketHandler {

  public static final String SOURCE_PAN_ID = "802154.sourcePAN";
  public static final String SOURCE_MODE = "802154.sourceMode";
  public static final String DESTINATION_PAN_ID = "802154.destPAN";
  public static final String DESTINATION_MODE = "802154.destMode";
  
  public static final String SEQ_NO = "802154.seqno";
  public static final String PAYLOAD_LEN = "802154.len";
  
  public static final int SHORT_ADDRESS = 2;
  public static final int LONG_ADDRESS = 3;
  
  /* create a 802.15.4 packet of the bytes and "dispatch" to the
   * next handler
   */
  public void packetReceived(Packet packet) {
//    IEEE802154Packet newPacket = new IEEE802154Packet(packet);
    /* no dispatch at this level ?! */

    int type = packet.getData(0) & 7;
    int security = (packet.getData(0) >> 3) & 1;
    int pending = (packet.getData(0) >> 4) & 1;
    int ackRequired = (packet.getData(0) >> 5) & 1;
    int panCompression  = (packet.getData(0)>> 6) & 1;
    int destAddrMode = (packet.getData(1) >> 2) & 3;
    int frameVersion = (packet.getData(1) >> 4) & 3;
    int srcAddrMode = (packet.getData(1) >> 6) & 3;
    int seqNumber = packet.getData(2);
    
    int pos = 3;
    int destPanID = 0;
    if (destAddrMode > 0) {
      destPanID = (packet.getData(pos) & 0xff) + ((packet.getData(pos + 1) & 0xff) << 8);
      packet.setAttribute(DESTINATION_PAN_ID, destPanID);
      byte[] destAddress = null;
      pos += 2;
      if (destAddrMode == SHORT_ADDRESS) {
        destAddress = new byte[2];
        destAddress[1] = packet.getData(pos);
        destAddress[0] = packet.getData(pos + 1);
        pos += 2;
      } else if (destAddrMode == LONG_ADDRESS) {
        destAddress = new byte[8];
        for (int i = 0; i < 8; i++) {
          destAddress[i] = packet.getData(pos + 7 - i);
        }
        pos += 8;
      }
      packet.setAttribute(Packet.LL_DESTINATION, destAddress);
    }

    if (srcAddrMode > 0) {
      int srcPanID = 0;
      if (panCompression == 0){
        srcPanID = (packet.getData(pos) & 0xff) + ((packet.getData(pos + 1) & 0xff) << 8);
        pos += 2;
      } else {
        srcPanID = destPanID;
      }
      packet.setAttribute(SOURCE_PAN_ID, srcPanID);
      byte[] sourceAddress = null;
      if (srcAddrMode == SHORT_ADDRESS) {
        sourceAddress = new byte[2];
        sourceAddress[1] = packet.getData(pos);
        sourceAddress[0] = packet.getData(pos + 1);        
        pos += 2;
      } else if (srcAddrMode == LONG_ADDRESS) {
        sourceAddress = new byte[8];
        for (int i = 0; i < 8; i++) {
          sourceAddress[i] = packet.getData(pos + 7 - i);
        }
        pos += 8;
      }
      packet.setAttribute(Packet.LL_SOURCE, sourceAddress);
    }
    packet.incPos(pos);
    packet.setAttribute(PAYLOAD_LEN, packet.getPayloadLength());

    dispatch(-1, packet);
  }
  
  /* create a 802.15.4 packet with the given packet as payload, and
   * deliver to the lower layer handler */
  @Override
  public void sendPacket(Packet payload) {
    // TODO Auto-generated method stub
  }

  public void printPacket(PrintStream out, Packet packet) {
    out.printf("802.15.4 from %4x/", packet.getAttributeAsInt(SOURCE_PAN_ID));
    printAddress(out, packet.getAttributeAsInt(SOURCE_MODE),
        (byte[]) packet.getAttribute(Packet.LL_SOURCE));
    out.printf(" to %4x/", packet.getAttributeAsInt(DESTINATION_PAN_ID));
    printAddress(out, packet.getAttributeAsInt(DESTINATION_MODE),
          (byte[]) packet.getAttribute(Packet.LL_DESTINATION));
    out.printf(" seqNo: %d len: %d\n", packet.getAttributeAsInt(SEQ_NO),
          packet.getAttributeAsInt(PAYLOAD_LEN));
  }

  private void printAddress(PrintStream out, int type, byte[] addr) {
    if (type == SHORT_ADDRESS) {
      out.printf("%02x02x", addr[0], addr[1]);
    } else if (type == LONG_ADDRESS) {
      out.printf("%02x02x:%02x02x:%02x02x:%02x02x", addr[0], addr[1], 
          addr[2], addr[3], addr[4], addr[5], addr[6], addr[7]);
    }
  }
}
