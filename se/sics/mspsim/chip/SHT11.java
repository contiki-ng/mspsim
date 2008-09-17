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
import se.sics.mspsim.util.Utils;

public class SHT11 extends Chip {

  private static final int IDLE = 0;
  private static final int COMMAND = 1;
  private static final int ACK_CMD = 2;

  private final static char[] INIT_COMMAND = "CdcCD".toCharArray();
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
  private int readCnt = 0;
  
  
  public void setDataPort(IOPort port, int bit) {
    sdataPort = port;
    sdataPin = bit;
  }
  
  public void reset() {
    clockHi = true;
    dataHi = true;
    initPos = 0;
    readCnt = 0;
    readData = 0;
    // Always set pin to high when not doing anything...
    sdataPort.setPinState(sdataPin, IOPort.PIN_HI);
  }
  
  public void clockPin(boolean high) {
    if (clockHi == high) return;
    char c = high ? 'C' : 'c';
    System.out.println(getName() + ": clock pin " + c);
    switch (state) {
    case IDLE:
      if (checkInit(c)) {
        state = COMMAND;
      }
      break;
    case COMMAND:
      if (c == 'C') {
        readData = (readData << 1) | (dataHi ? 1 : 0);
        readCnt++;
        if (readCnt == 8) {
          System.out.println("SHT11: read: " + Utils.hex8(readData));
          readCnt = 0;
          state = ACK_CMD;
          sdataPort.setPinState(sdataPin, IOPort.PIN_LOW);
        }
      }
      break;
    case ACK_CMD:
      sdataPort.setPinState(sdataPin, IOPort.PIN_HI);
      state = IDLE;
      break;
    }
    clockHi = high;
  }
  
  public void dataPin(boolean high) {
    if (dataHi == high) return;
    char c = high ? 'D' : 'd';
    System.out.println(getName() + ": data pin  " + c);
    switch (state) {
    case IDLE:
      if (checkInit(c)) {
        state = COMMAND;
      }
    }
    dataHi = high;
  }

  private boolean checkInit(char c) {
    if (INIT_COMMAND[initPos] == c) {
      initPos++;
      if (initPos == INIT_COMMAND.length) {
        initPos = 0;
        System.out.println("SHT11: COMMAND signature detected!!!");
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
