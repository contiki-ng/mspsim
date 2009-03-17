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

import java.io.PrintStream;
import java.util.ArrayList;

public abstract class AbstractPacket implements Packet {

  byte[] payload;
  int payloadLen;
  
  ArrayList<AbstractPacket> packetHandlers = new ArrayList<AbstractPacket>();
  boolean valid = false;
  
  public void addInnerPacketHandler(AbstractPacket packet) {
    packetHandlers.add(packet);
  }

  public boolean validPacket() {
    return valid;
  }
  
  public void clear() {
    valid = false;
  }
  
  public byte[] getPayload() {
    return payload;
  }
  
  void setPayload(byte[] data, int startPos, int len) {
    payloadLen = len;
    payload = new byte[payloadLen];
    System.arraycopy(data, startPos, payload, 0, payloadLen);
    valid = true;
    notifyPacketHandlers(payload, payloadLen);
  }
  
  public void notifyPacketHandlers(byte[] payload, int len) {    
    for (int i = 0; i < packetHandlers.size(); i++) {
      try {
        packetHandlers.get(i).setPacketData(payload, len);        
      } catch (Exception e) {
      }
    }
  }
  
  public void printPacketStack(PrintStream out) {
    printPacket(out);
    for (int i = 0; i < packetHandlers.size(); i++) {
      /* only the valid packets should print anything... */
      packetHandlers.get(i).printPacketStack(out);
    }
  }
}
