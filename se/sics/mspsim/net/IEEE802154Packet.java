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
 * IEEE802154Packet
 *
 * Author  : Joakim Eriksson
 * Created : mar 2009
 * Updated : $Date:$
 *           $Revision:$
 */

package se.sics.mspsim.net;

import java.io.PrintStream;

public class IEEE802154Packet extends AbstractPacket {

  public static final int SHORT_ADDRESS = 2;
  public static final int LONG_ADDRESS = 3;
  
  // TODO: remove long addresses...
  private int type = 0;
  private int security = 0;
  private int pending = 0;
  private int ackRequired = 0;
  private int panCompression = 0;
  private int destAddrMode;
  private int frameVersion;
  private int srcAddrMode;
  private byte seqNumber;
  private int destPanID;
  private long destAddr;
  private long srcAddr;
  private byte[] destAddress = new byte[8];
  private byte[] sourceAddress = new byte[8];
  private int srcPanID;
  
  public IEEE802154Packet(Packet container) {
    byte[] payload = container.getPayload();
    setPacketData(container, payload, payload.length);
  }

  public byte[] getDestinationAddress() {
    return destAddress;
  }
  
  public byte[] getSourceAddress() {
    return sourceAddress; 
  }
  
  public void printPacket(PrintStream out) {
    out.printf("802.15.4 from %4x/", srcPanID);
    printAddress(out, srcAddrMode, srcAddr);
    out.printf(" to %4x/", destPanID);
    printAddress(out, destAddrMode, destAddr);
    out.printf(" seqNo: %d len: %d\n", seqNumber, payloadLen);
    if (payloadPacket != null) {
      payloadPacket.printPacket(out);
    }
  }

  public void setPacketData(Packet container, byte[] data, int len) {
    container.setPayloadPacket(this);
    containerPacket = container;
    
    type = data[0] & 7;
    security = (data[0] >> 3) & 1;
    pending = (data[0] >> 4) & 1;
    ackRequired = (data[0] >> 5) & 1;
    panCompression  = (data[0]>> 6) & 1;
    destAddrMode = (data[1] >> 2) & 3;
    frameVersion = (data[1] >> 4) & 3;
    srcAddrMode = (data[1] >> 6) & 3;
    seqNumber = data[2];

    int pos = 3;
    if (destAddrMode > 0) {
      destPanID = (data[pos] & 0xff) + ((data[pos + 1] & 0xff) << 8);
      pos += 2;
      if (destAddrMode == SHORT_ADDRESS) {
        destAddr = (data[pos] & 0xff) + ((data[pos + 1] & 0xff) << 8);
        destAddress[1] = data[pos];
        destAddress[0] = data[pos + 1];
        pos += 2;
      } else if (destAddrMode == LONG_ADDRESS) {
        destAddr = data[pos] + ((data[pos + 1] & 0xffL) << 8) +
        ((data[pos + 2] & 0xffL) << 16) + ((data[pos + 3] & 0xffL) << 24) +
        ((data[pos + 4] & 0xffL) << 32) + ((data[pos + 5] & 0xffL)<< 40) +
        ((data[pos + 6] & 0xffL) << 48) + ((data[pos + 7] & 0xffL) << 56);

        for (int i = 0; i < 8; i++) {
          destAddress[i] = data[pos + 7 - i];          
        }

        pos += 8;
      }
    }

    if (srcAddrMode > 0) {
      if (panCompression == 0){
        srcPanID = (data[pos] & 0xff) + ((data[pos + 1] & 0xff) << 8);
        pos += 2;
      } else {
        srcPanID = destPanID;
      }
      if (srcAddrMode == SHORT_ADDRESS) {
        srcAddr = (data[pos] & 0xff) + ((data[pos + 1] & 0xff) << 8);
        sourceAddress[1] = data[pos];
        sourceAddress[0] = data[pos + 1];        
        pos += 2;
      } else if (srcAddrMode == LONG_ADDRESS) {
        srcAddr = data[pos] + ((data[pos + 1] & 0xffL) << 8) +
          ((data[pos + 2] & 0xffL) << 16) + ((data[pos + 3] & 0xffL) << 24) +
          ((data[pos + 4] & 0xffL) << 32) + ((data[pos + 5] & 0xffL)<< 40) +
          ((data[pos + 6] & 0xffL) << 48) + ((data[pos + 7] & 0xffL) << 56);

        for (int i = 0; i < 8; i++) {
          sourceAddress[i] = data[pos + 7 - i];          
        }
        pos += 8;
      }
    }
    /* two bytes in the end that are not included in payload! */
    setPayload(data, pos, len - pos - 2);
  }

  private void printAddress(PrintStream out, int type, long addr) {
    if (type == SHORT_ADDRESS) {
      out.printf("%04x", addr & 0xffff);
    } else if (type == LONG_ADDRESS) {
      out.printf("%04x:%04x:%04x:%04x", (addr >> 48) & 0xffff, (addr >> 32) & 0xffff, (addr >> 16) & 0xffff, addr & 0xffff);
    }
  }
}