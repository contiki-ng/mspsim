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
 * $Id: Multiplier.java,v 1.3 2007/10/21 21:17:34 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * Multiplier
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.core;
import se.sics.mspsim.util.Utils;

public class Multiplier extends IOUnit {

  public static final boolean DEBUG = false;

  public static final int MPY = 0x130;
  public static final int MPYS = 0x132;
  public static final int MAC = 0x134;
  public static final int MACS = 0x136;
  public static final int OP2 = 0x138;
  public static final int RESLO = 0x13a;
  public static final int RESHI = 0x13c;
  public static final int SUMEXT = 0x13e;

  private int mpy;
  private int mpys;
  private int op2;

  private int resLo;
  private int resHi;
  private int mac;
  private int macs;
  private int sumext;
  
  private int lastWriteOP;
  private int currentSum;
  
  MSP430Core core;
  /**
   * Creates a new <code>Multiplier</code> instance.
   *
   */
  public Multiplier(MSP430Core core, int memory[], int offset) {
    super(memory, offset);
    this.core = core;
  }

  public boolean needsTick() {
    return false;
  }

  public int read(int address, boolean word, long cycles) {
    switch (address) {
    case MPY:
      return mpy;
    case MPYS:
      return mpys;
    case MAC:
      return mac;
    case MACS:
      return macs;
    case OP2:
      return op2;
    case RESHI:
      if (DEBUG) System.out.println(getName() + " read res hi: " + resHi );
      return resHi;
    case RESLO:
      if (DEBUG) System.out.println(getName() + " read res lo: " + resLo );
      return resLo;
    case SUMEXT:
      if (DEBUG) System.out.println(getName() + " read sumext: " + sumext);
      return sumext;
    }
    System.out.println(getName() + " read other address:" + address);
    return 0;
  }

  public void write(int address, int data, boolean word, long cycles) {
    if (DEBUG) {
      System.out.println("Multiplier: write to: " + Utils.hex16(address) +
			 " data = " + data + " word = " + word);
    }
    if (MSP430Constants.DEBUGGING_LEVEL > 0) {
      System.out.println("Write to HW Multiplier: " +
			 Integer.toString(address, 16) +
			 " = " + data);
    }

    switch(address) {
    case MPY:
      mpy = data;
      if (DEBUG) System.out.println(getName() + " Write to MPY: " + data);
      lastWriteOP = address;
      break;
    case MPYS:
      mpys = data;
      if (DEBUG) System.out.println(getName() + " Write to MPYS: " + data);
      lastWriteOP = address;
      break;
    case MAC:
      mac = data;
      if (DEBUG) System.out.println(getName() + " Write to MAC: " + data);
      lastWriteOP = address;
      break;
    case MACS:
      macs = data;
      if (DEBUG) System.out.println(getName() + " Write to MACS: " + data);
      lastWriteOP = address;
      break;
    case RESLO:
      resLo = data;
      break;
    case RESHI:
      resHi = data;
      break;
    case OP2:
      if (DEBUG) System.out.println(getName() + " Write to OP2: " + data);
      sumext = 0;
      op2 = data;
      // This should be picked based on which op was written previously!!!
      int o1 = mpy;
      boolean signMode = false;
      boolean sum = false;
      if (lastWriteOP == MPYS) {
        o1 = mpys;
        signMode = true;
      } else if (lastWriteOP == MAC) {
        o1 = mac;
        sum = true;
      } else if (lastWriteOP == MACS) {
        o1 = macs;
        signMode = true;
        sum = true;
      }
      
      if (signMode) {
        // Assume two 16 bit mults.
        if (((o1 ^ op2) & 0x8000) > 0) {
          sumext = 0xffff;
        }
      }
      long res = o1 * op2;
      System.out.println("O1:" + o1 + " * " + op2 + " = " + res);
      if (sum) { 
        currentSum = (resHi << 16) + resLo;
        currentSum += res;
        res = currentSum;
      }
      resHi = (int) ((res >> 16) & 0xffff);
      resLo = (int) (res & 0xffff);
      if(DEBUG) System.out.println(" ===> result = " + res);
      break;
    }
  }

  public String getName() {
    return "Hardware Multiplier";
  }

  public void interruptServiced(int vector) {
  }

  public int getModeMax() {
    return 0;
  }


}
