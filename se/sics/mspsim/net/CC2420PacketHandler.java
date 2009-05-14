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

import se.sics.mspsim.chip.RFListener;

public class CC2420PacketHandler extends AbstractPacketHandler implements RFListener {

  public static final String CC2420_LEN = "cc2420.len";
  
  private static final int SFD_SEARCH = 1;
  private static final int LEN = 2;
  private static final int PACKET = 3;  
  
  private static final byte[] PREAMBLE = {0, 0, 0, 0, 0x7a};
  
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
//        CC2420Packet packet = new CC2420Packet();
//        packet.setPayload(packetBuffer, PREAMBLE.length + 1, packetLen - 2);
        Packet packet = new Packet();
        packet.setBytes(packetBuffer, PREAMBLE.length + 1, packetLen - 2);
        packet.setAttribute(CC2420_LEN, packet.getTotalLength());
        dispatch(-1, packet);
        System.out.println("CC2420: Packet received");

        /* this is a packet that has passed the stack! */
        mode = SFD_SEARCH;
        pos = 0;
      }
      break;
    }
  }

  
  public void packetReceived(Packet container) {
    // Never any packets received here...
  }

  public void printPacket(PrintStream out, Packet packet) {
    int payloadLen = packet.getAttributeAsInt(CC2420_LEN);
    out.print("CC2420 | len:" + payloadLen + " | ");
    for (int i = 0; i < payloadLen; i++) {
      out.printf("%02x", packet.getData(i) & 0xff);
      if ((i & 3) == 3) {
        out.print(" ");
      }
    }
    out.println();
  }

  
  public void sendPacket(Packet packet) {
    packet.prependBytes(new byte[packet.getTotalLength()]);
    packet.prependBytes(PREAMBLE);
    byte[] data = packet.getBytes();
    System.out.println("Should send to radio!!!! " + packet.getTotalLength());
    // Stuff to send to radio!!!
  }

}
