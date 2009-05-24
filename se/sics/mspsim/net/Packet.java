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
 * Packet
 *
 * Author  : Joakim Eriksson
 * Created :  mar 2009
 * Updated : $Date:$
 *           $Revision:$
 */

package se.sics.mspsim.net;

import java.util.HashMap;

public class Packet {

  public static final String LL_SOURCE = "link.source";
  public static final String LL_DESTINATION = "link.destination";
  
  protected HashMap<String, Object> attributes = new HashMap<String, Object>();

  /* this is the packet data array */
  protected byte[] packetData;
  /* current position of packet data cursor */
  int currentPos = 0;

  public void setBytes(byte[] data) {
    packetData = data;
    currentPos = 0;
  }
  
  public void setBytes(byte[] data, int startPos, int len) {
    byte[] payload = new byte[len];
    System.arraycopy(data, startPos, payload, 0, len);
    packetData = payload;
    /* reset cursor in this case !!! */
    currentPos = 0;
  }
  
  public void appendBytes(byte[] data) {
    if (packetData == null) {
      packetData = data;
    } else {
      setPacketData(packetData, data);
    }
  }
  
  public void prependBytes(byte[] data) {
    if (packetData == null) {
      packetData = data;
    } else {
      setPacketData(data, packetData);
    }
  }
  
  private void setPacketData(byte[] data1, byte[] data2) {
    byte[] newData = new byte[data1.length + data2.length];
    System.arraycopy(data1, 0, newData, 0, data1.length);
    System.arraycopy(data2, 0, newData, data1.length, data2.length);
    packetData = newData;
  }
  
  public byte[] getBytes() {
    return packetData;
  }

  /* total packet length */
  public int getTotalLength() {
    return packetData.length;
  }
  
  /* called when headers are parsed to get current payload lenght 
   * only useful when parsing messages... */
  public int getPayloadLength() {
    return packetData.length - currentPos;
  }
  
  public void setAttribute(String name, Object object) {
    attributes.put(name, object);
  }

  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  public byte[] getLinkSource() {
    return (byte[]) attributes.get(LL_SOURCE);
  }

  public byte[] getLinkDestination() {
    return (byte[]) attributes.get(LL_DESTINATION);
  }

  public int get32(int pos) {
    pos = currentPos + pos;
    if (packetData.length >= pos + 3) {
    return ((packetData[pos] & 0xff) << 24) |
    ((packetData[pos + 1] & 0xff) << 16) |
    ((packetData[pos + 2] & 0xff) << 8) |
    (packetData[pos + 3] & 0xff);
    }
    return 0;
  }

  public static int get32(byte[] data, int pos) {
    if (data.length >= pos + 3) {
    return ((data[pos] & 0xff) << 24) |
    ((data[pos + 1] & 0xff) << 16) |
    ((data[pos + 2] & 0xff) << 8) |
    (data[pos + 3] & 0xff);
    }
    return 0;
  }

  public static int get16(byte[] data, int pos) {
    if (data.length >= pos + 1) {
    return ((data[pos] & 0xff) << 8) |
    ((data[pos + 1] & 0xff) << 0);
    }
    return 0;
  }

  
  public int get16(int pos) {
    pos = currentPos + pos;
    if (packetData.length > pos + 1)
      return ((packetData[pos] & 0xff) << 8) | packetData[pos + 1] & 0xff;
    return 0;
  }

  public byte getData(int pos) {
    return packetData[currentPos + pos];
  }
  
  public void incPos(int delta) {
    currentPos += delta;
  }

  public byte[] getPayload() {
    // payload is from pos to end...
    byte[] payload = new byte[packetData.length - currentPos];
    System.arraycopy(packetData, currentPos, payload, 0, payload.length);
    return payload;
  }
  
  public int getAttributeAsInt(String attr) {
    Object val = attributes.get(attr);
    if (val instanceof Integer) return ((Integer)val).intValue();
    if (val instanceof String) return Integer.parseInt((String) val);
    return -1;
  }

  /* copies bytes from currentPos + pos to the given array */
  public void copy(int pos, byte[] dst, int dstPos, int len) {
    // TODO Auto-generated method stub
    int tPos = pos + currentPos;
    System.arraycopy(packetData, tPos, dst, dstPos, len);
  }

  public void setData(int pos, byte val) {
    packetData[currentPos + pos] = val;
  }
}