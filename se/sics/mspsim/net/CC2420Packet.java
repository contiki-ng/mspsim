/**
 * Copyright (c) 2008, Swedish Institute of Computer Science.
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
 * CC2420Packet
 *
 * Author  : Joakim Eriksson
 * Created :  mar 2009
 * Updated : $Date:$
 *           $Revision:$
 */

package se.sics.mspsim.net;

import java.io.PrintStream;
import se.sics.mspsim.chip.RFListener;

public class CC2420Packet extends AbstractPacket implements RFListener {

  private static final int SFD_SEARCH = 1;
  private static final int LEN = 2;
  private static final int PACKET = 3;  
  
  private static final byte[] PREAMBLE = {0, 0, 0, 0, 0x7a};
  private int len;
  private byte[] payload;
    
  public byte[] getDataField(String name) {
    return null;
  }

  public int getIntField(String name) {
    return 0;
  }

  public byte[] getPayload() {
    return null;
  }

  public int getSize() {
    return len;
  }

  /* should this be the interface??? or only RF_LIST ???*/
  /* maybe this is default and CC2420 is "special" */
  public void setPacketData(byte[] data, int plen) {
    if (data.length > PREAMBLE.length) {
      int pos = 0;
      for (int i = 0; i < PREAMBLE.length; i++) {
        if (data[i] != PREAMBLE[i]) {
          throw new IllegalStateException("Illegal packet data");
        }
        pos++;
      }
      len = data[pos] & 0xff;

      /* this will have RSSI and other junk last in the packet */
      if (len + 5 <= plen) {
        payload = new byte[len & 0xff];
        System.arraycopy(data, pos + 1, payload, 0, len);
        /* ignore RSSI, etc for now */
        System.out.println("Valid CC2420 packet received...");
        valid = true;
        notifyPacketHandlers(payload, payload.length);
        return;
      } else {
        throw new IllegalStateException("Illegal packet data");
      }
    }
  }

  public void clear() {
    pos = 0;
    mode = SFD_SEARCH;
    valid = false;
  }
  
  byte[] packetBuffer = new byte[256];
  int mode = SFD_SEARCH;
  int pos;
  int packetLen;
  int sfdSearch = 0;
  
  public void receivedByte(byte data) {
    packetBuffer[pos++] = data;
    switch (mode) {
    case SFD_SEARCH:
      if (sfdSearch < 4 && data == 0)
        sfdSearch++;
      if (sfdSearch == 4 && data == 0x7a) {
        mode = LEN;
        sfdSearch = 0;
      }
      break;
    case LEN:
      mode = PACKET;
      packetLen = data & 0xff;
      System.out.println("Packet len: " + packetLen);
      break;
    case PACKET:
      if (pos == packetLen + PREAMBLE.length + 1) {
        /* the packet is in!!! */
        setPacketData(packetBuffer, pos);
        mode = SFD_SEARCH;
      }
      break;
    }
  }

  public void printPacket(PrintStream out) {
    if (valid) {
      out.println("CC2420 | len:" + len + "|");
    }
  }

  
  public static void main(String[] args) {
    CC2420Packet p = new CC2420Packet();
    p.addInnerPacketHandler(new IEEE802154Packet());
    int[] data = new int[] {0,0,0,0,0x7a,48,
        0x41, 0xCC, 0x74, 0xCD, 0xAB, 0x55, 0x44, 0x33,
        0xFE, 0xFF, 0x22, 0x11, 0x02, 0x16, 0x15, 0x14,
        0xFE, 0xFF, 0x13, 0x12, 0x02, 0x03, 0xC0, 0x9D,
        0x06, 0x80, 0x00, 0x01, 0xAD, 0x39, 0x00, 0x50, 
        0xAE, 0xC4, 0x9D, 0xC6, 0x00, 0x00, 0x00, 0x01,
        0x50, 0x10, 0x13, 0x10, 0xE7, 0xBF, 0x00, 0x00};
    for (int i = 0; i < data.length; i++) {
      p.receivedByte((byte) (data[i] & 0xff));
    }
    p.printPacketStack(System.out);
  }
}
