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

// remove
public abstract class AbstractPacket extends Packet {

  byte[] payload;
  int payloadLen;
  
  Packet containerPacket;
  Packet payloadPacket;
  
  public byte[] getPayload() {
    return payload;
  }
  
  public byte[] getSourceAddress() {
    return null;
  }

  public byte[] getDestinationAddress() {
    return null;
  }

  public AbstractPacket createReply() {
    return null;
  }
  
  void setPayload(byte[] data, int startPos, int len) {
    payloadLen = len;
    payload = new byte[payloadLen];
    System.arraycopy(data, startPos, payload, 0, payloadLen);
  }

  static int get32(byte[] data, int pos) {
    if (data.length > pos + 3)
    return ((data[pos] & 0xff) << 24) | ((data[pos++] & 0xff) << 16) |
        ((data[pos] & 0xff) << 8) | (data[pos + 1] & 0xff);
    return 0;
  }
  
  static int get16(byte[] data, int pos) {
    if (data.length > pos + 1)
      return ((data[pos] & 0xff) << 8) | data[pos + 1] & 0xff;
    return 0;
  }

  static byte[] getAddress(byte[] data, int pos) {
    byte[] targetAddress = new byte[16];
    System.arraycopy(data, pos, targetAddress, 0, 16);
    return targetAddress;
  }
  
}