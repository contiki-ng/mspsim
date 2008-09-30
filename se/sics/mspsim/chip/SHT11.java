/**
 * Copyright (c) 2007, 2008, Swedish Institute of Computer Science.
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
 * SHT11 
 *
 * Author  : Joakim Eriksson, joakime@sics.se
 * Created : Sept 16 2008
 * Updated : $Date:  $
 *           $Revision: $
 */

package se.sics.mspsim.chip;

import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.TimeEvent;
import se.sics.mspsim.util.Utils;

public class SHT11 extends Chip {

  private static final int IDLE = 0;
  private static final int COMMAND = 1;
  private static final int ACK_CMD = 2;
  private static final int MEASURE = 3;
  private static final int WRITE_BYTE = 4;
  private static final int ACK_WRITE = 5;

  private final int CMD_MEASURE_TEMP = 0x03;
  private final int CMD_MEASURE_HUM = 0x05;
  
  private final boolean DEBUG = false;

  private final static char[] INIT_COMMAND = "CdcCDc".toCharArray();
  private int initPos = 0;
  
  
  /* Serial data pins */
  IOPort sclkPort;
  int sclkPin;
  IOPort sdataPort;
  int sdataPin;
  
  int state = IDLE;
  
  boolean clockHi = false;
  boolean dataHi = false;
  private int readData = 0;
  private int bitCnt = 0;
  private int temp = 3960 + 2400;
  private int humid = 0x1040;
  private int output[] = new int[3];
  private int writePos = 0;
  private int writeLen = 0;
  private int writeData = 0;
  
  private MSP430Core cpu;
  
  private int rev8bits(int v) {
    int r = 0;
    int s = 7;

    for (v >>= 1; v > 0; v >>= 1) {
      r <<= 1;
      r |= v & 1;
      s--;
    }
    r <<= s;                  /* Shift when v's highest bits are zero */
    return r;
  }
  
  private int crc8Add(int acc, int data) {
    int i;
    acc ^= data;
    for(i = 0; i < 8; i++) {
      if((acc & 0x80) != 0) {
        acc = (acc << 1) ^ 0x31;
      } else {
        acc <<= 1;
      }
    }
    return acc & 0xff;
  }
  
  
  private TimeEvent measureEvent = new TimeEvent(0) {
    public void execute(long t) {
      if (readData == CMD_MEASURE_TEMP) {
        output[0] = temp >> 8;
        output[1] = temp & 0xff;
      } else if (readData == CMD_MEASURE_HUM) {
        output[0] = humid >> 8;
        output[1] = humid & 0xff;
      } else {
        /* Something bad has happened */
        return;
      }
      int crc = 0;
      crc = crc8Add(crc, readData);
      crc = crc8Add(crc, output[0]);
      crc = crc8Add(crc, output[1]);
      System.out.println("CRC: " + crc + " rcrc: " + rev8bits(crc));
      output[2] = rev8bits(crc);
      
      /* finished measuring - signal with LOW! */
      sdataPort.setPinState(sdataPin, IOPort.PIN_LOW);
      state = WRITE_BYTE;
      writeData = output[0];
      writePos = 0;
      writeLen = 3;
    }};
    
  public SHT11(MSP430Core core) {
    cpu = core;
  }
    
  public void setDataPort(IOPort port, int bit) {
    sdataPort = port;
    sdataPin = bit;
  }
  
  public void reset(int type) {    
    clockHi = true;
    dataHi = true;
    initPos = 0;
    bitCnt = 0;
    readData = 0;
    writePos = 0;
    writeData = 0;
    state = IDLE;
    // Always set pin to high when not doing anything...
    sdataPort.setPinState(sdataPin, IOPort.PIN_HI);
  }
  
  public void clockPin(boolean high) {
    if (clockHi == high) return;

    char c = high ? 'C' : 'c';
    if (DEBUG) System.out.println(getName() + ": clock pin " + c);
    switch (state) {
    case IDLE:
      if (checkInit(c)) {
        state = COMMAND;
      }
      break;
    case COMMAND:
      if (c == 'c') {
        readData = (readData << 1) | (dataHi ? 1 : 0);
        bitCnt++;
        if (bitCnt == 8) {
          System.out.println("SHT11: read: " + Utils.hex8(readData));
          bitCnt = 0;
          state = ACK_CMD;
          sdataPort.setPinState(sdataPin, IOPort.PIN_LOW);
        }
      }
      break;
    case ACK_CMD:
      if (c == 'c') {
        sdataPort.setPinState(sdataPin, IOPort.PIN_HI);
        if (readData == CMD_MEASURE_HUM || readData == CMD_MEASURE_TEMP) {
          state = MEASURE;
          /* schedule measurement for 20 millis */
          cpu.scheduleTimeEventMillis(measureEvent, 20);
        }
      }
      break;
    case MEASURE:
      break;
    case WRITE_BYTE:
      if (c == 'C') {
        boolean hi = (writeData & 0x80) != 0;
        sdataPort.setPinState(sdataPin, hi ? IOPort.PIN_HI : IOPort.PIN_LOW);
        bitCnt++;
        writeData = writeData << 1;
        if (bitCnt == 8) {
          // All bits are written!
          state = ACK_WRITE;
          System.out.println("Wrote byte: " + output[writePos]);
          writePos++;
        }
      }
      break;
    case ACK_WRITE:
      if (c == 'C' && dataHi) {
        System.out.println("*** NO ACK???");
        reset(0);
      }
      break;
    }
    clockHi = high;
  }
  
  public void dataPin(boolean high) {
    if (dataHi == high) return;
    char c = high ? 'D' : 'd';
    if (DEBUG) System.out.println(getName() + ": data pin  " + c);
    switch (state) {
    case IDLE:
      if (checkInit(c)) {
        state = COMMAND;
      }
      break;
    case ACK_WRITE:
      if (c == 'D') { // if D goes back high - then we are done here!!!
        System.out.println("ACK for byte complete...");
        if (writePos < writeLen) {
          state = WRITE_BYTE;
          writeData = output[writePos];
          bitCnt = 0;
        } else {
          reset(0);
        }
      }
      break;
    }
    dataHi = high;
  }

  private boolean checkInit(char c) {
    if (INIT_COMMAND[initPos] == c) {
      initPos++;
      if (initPos == INIT_COMMAND.length) {
        initPos = 0;
        if (DEBUG) {
          System.out.println("SHT11: COMMAND signature detected!!!");
        }
        return true;
      }
    } else {
      initPos = 0;
      // If this is a correct first char => ok!
      if (c == INIT_COMMAND[0]) {
        initPos = 1;
      }
    }
    return false;
  }
  
  
  public int getModeMax() {
    return 0;
  }

  public String getName() {
    return "SHT11";
  }

}
