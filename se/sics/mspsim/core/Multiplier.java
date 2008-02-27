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
    int val = memory[address];
    if (word) {
      val |= memory[(address + 1) & 0xffff] << 8;
    }
    return val;
  }

  public void write(int address, int data, boolean word, long cycles) {
    if (DEBUG) {
      System.out.println("Multiplier: write to: " + Utils.hex16(address) +
			 " data = " + data + " word = " + word);
    }
    memory[address] = data & 0xff;
    if (word) {
      memory[address + 1] = (data >> 8) & 0xff;
    }

    if (MSP430Constants.DEBUGGING_LEVEL > 0) {
      System.out.println("Write to HW Multiplier: " +
			 Integer.toString(address, 16) +
			 " = " + data);
    }

    switch(address) {
    case OP2:
      int o2 = memory[OP2] + (memory[OP2 + 1] << 8);
      // This should be picked based on which op was written previously!!!
      int o1 = memory[MPYS] + (memory[MPYS + 1] << 8);
      int res = o1 * o2;
      int hiRes = (res >> 16) & 0xffff;
      int loRes = res & 0xffff;
      memory[RESHI] = hiRes & 0xff;
      memory[RESHI + 1] = (hiRes >> 8) & 0xff;
      memory[RESLO] = loRes & 0xff;
      memory[RESLO + 1] = (loRes >> 8) & 0xff;
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
