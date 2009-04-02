/**
 * Copyright (c) 2007, Swedish Institute of Computer Science.
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
 * $Id: DS2411.java 177 2008-03-11 15:32:12Z nifi $
 *
 * -----------------------------------------------------------------
 *
 * DS2411 - MAC Address chip
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2008-03-11 16:32:12 +0100 (ti, 11 mar 2008) $
 *           $Revision: 177 $
 */
package se.sics.mspsim.chip;


import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.TimeEvent;
import se.sics.mspsim.util.Utils;

public class DS2411 extends Chip {

  private static final boolean DEBUG = false;
  
  private enum STATE {
    IDLE, WAIT_FOR_RESET, RESETTING, SIGNAL_READY, READY, WAIT_SENDING, SENDING
  }
  
  private static final int CMD_READ_ROM = 0x33;
  private static final int CMD_SEACH_ROM = 0xf0;  
  
  MSP430Core cpu;
  private IOPort sdataPort;
  private int sdataPin;
  private STATE state = STATE.IDLE;
  private boolean lastPin;
  
  private int pos = 0;
  private int readByte = 0;
  private int writeByte = 0;
  private int writeLen = 0;
  private int writePos = 0;
  /* max 10 bytes to write back */
  private int[] writeBuf = new int[10];
  private int[] macID = new int[]{1, 2, 3, 4, 5, 6};
  
  private TimeEvent stateEvent = new TimeEvent(0) {
    public void execute(long t) {
      switch (state) {
      case WAIT_FOR_RESET:
        if (!lastPin) {
          state = STATE.RESETTING;
          log("Reseting...");
        }
        break;
      case SIGNAL_READY:
        /* ready! release bus */
        sdataPort.setPinState(sdataPin, IOPort.PIN_HI);
        state = STATE.READY;
        log("Ready!");
        readByte = 0;
        pos = 0;
        break;
      case READY:
        log("Reading: " + (lastPin ? 1 : 0));
        readByte = readByte + (lastPin ? (1 << pos) : 0);
        pos++;
        if (pos == 8) {
          log("Command: " + Utils.hex8(readByte));
          handleCommand(readByte);
          state = STATE.WAIT_SENDING;
          pos = 0;
          writePos = 0;
          writeByte = writeBuf[writePos];              
        }
        break;
      }
    }
  };
  
  public DS2411(MSP430Core cpu) {
    this.cpu = cpu;
    if (DEBUG) {
      setLogStream(System.out);
    }
  }

  private int crcAdd(int acc, int data) {
    int i;
    acc ^= data;
    for (i = 0; i < 8; i++) {
      if ((acc & 1) == 1)
        acc = (acc >> 1) ^ 0x8c;
      else
        acc >>= 1;
    }
    return acc;
  }

  private int crc8(int[] buf, int len) {
    int acc = 0;
    for (int i = 0; i < len; i++) {
      acc = crcAdd(acc, buf[i]);
    }
    return acc;
  }
  
  protected void handleCommand(int cmd) {
    if (cmd == CMD_READ_ROM) {
      /* 48 bits = 6 bytes */
      writeBuf[0] = 0x01; /* family */
      writeBuf[1] = macID[0];
      writeBuf[2] = macID[1];
      writeBuf[3] = macID[2];
      writeBuf[4] = macID[3];
      writeBuf[5] = macID[4];
      writeBuf[6] = macID[5];
      writeBuf[7] = crc8(writeBuf, 7); /* the crc */
      writeLen = 1 + 6 + 1;
    }
  }

  public int getModeMax() {
    return 0;
  }

  public String getName() {
    return "DS2411";
  }

  public void setDataPort(IOPort port, int bit) {
    sdataPort = port;
    sdataPin = bit;
  }

  /* Communication pin to the DS2411 */
  /* TODO: we should have a separate reset event so that a low for a long time will
   * cause a reset in any state...
   */
  public void dataPin(boolean high) {
    log(" Data pin high: " + high + " " + cpu.cycles);
    if (lastPin == high) return;
    lastPin = high;
    switch(state) {
    case IDLE:
      sdataPort.setPinState(sdataPin, IOPort.PIN_HI);      
      if (!high) {
        state = STATE.WAIT_FOR_RESET;
        /* reset if low for at least 480uS - we check after 400uS and resets
         * then */
        log("Wait for reset...");
        cpu.scheduleTimeEventMillis(stateEvent, 0.400);
      }
      break;
    case RESETTING:
      if (high) {
        state = STATE.SIGNAL_READY;
        log("Signal ready");
        /* reset done - signal with LOW for a while! */
        sdataPort.setPinState(sdataPin, IOPort.PIN_LOW);
        cpu.scheduleTimeEventMillis(stateEvent, 0.480);
        pos = 0;
      }
      break;
    case READY:
      /* we should read a byte during the READY - 60us - 120us time slot*/
      if (!high) {
        /* schedule a read after 40us */
        cpu.scheduleTimeEventMillis(stateEvent, 0.040);
      }
      break;
    case WAIT_SENDING:
      if (!high) {
        state = STATE.SENDING;
      }
      break;
    case SENDING:
      if (high) {
        if (pos == 0) log("should write next byte: " + writeByte);

        /* went high => we should send another bit */
        sdataPort.setPinState(sdataPin,
            ((writeByte & (1 << pos)) > 0) ? IOPort.PIN_HI : IOPort.PIN_LOW);
        log(" wrote bit: " + (((writeByte & (1 << pos)) > 0) ? 1 : 0));
        pos++;
        if (pos == 8) {
          writePos++;
          if (writePos == writeLen) {
            log("write is over => IDLE!!!!");
            state = STATE.IDLE;
          } else {
            pos = 0;
            writeByte = writeBuf[writePos];
          }
        }
      }
    }
  }

  public void setMACID(int i, int j, int k, int l, int m, int n) {
    macID[0] = i;
    macID[1] = j;
    macID[2] = k;
    macID[3] = l;
    macID[4] = m;
    macID[5] = n;    
  }
}
