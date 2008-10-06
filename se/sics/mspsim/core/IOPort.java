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
 * $Id$
 *
 * -----------------------------------------------------------------
 *
 * IOPort
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */

package  se.sics.mspsim.core;
import se.sics.mspsim.util.Utils;

public class IOPort extends IOUnit {

  public static final int PIN_LOW = 0;
  public static final int PIN_HI = 1;

  public static final boolean DEBUG = false;

  public static final String[] iNames = {
    "P_IN","P_OUT", "P_DIR", "P_IFG", "P_IES", "P_IE", "P_SEL" };
  public static final String[] names = {
    "P_IN", "P_OUT", "P_DIR", "P_SEL" };

  private String name;
  private int interrupt;
  private int interruptFlag;
  private int interruptEnable;
  private MSP430Core cpu;

  // External pin state!
  private int pinState[] = new int[8];

  public static final int IN = 0;
  public static final int OUT = 1;
  public static final int DIR = 2;
  public static final int SEL = 3;
  public static final int IFG = 3;
  public static final int IES = 4;
  public static final int IE = 5;
  public static final int ISEL = 6;

  // One listener per port maximum (now at least)
  private PortListener listener;
  // represents the direction register
  private int dirReg;
  private int out;
  
  private Timer[] timerCapture = new Timer[8];

  /**
   * Creates a new <code>IOPort</code> instance.
   *
   */
  public IOPort(MSP430Core cpu, String portName,
		int interrupt, int[] memory, int offset) {
    super(memory, offset);
    name = portName;
    this.interrupt = interrupt;
    this.interruptEnable = 0;
    this.cpu = cpu;
  }

  public void setPortListener(PortListener listener) {
    this.listener = listener;
  }

  public void setTimerCapture(Timer timer, int pin) {
    System.out.println("Setting timer capture for pin: " + pin);
    timerCapture[pin] = timer;
  }
  
  public int read(int address, boolean word, long cycles) {
    if (DEBUG) {
      System.out.println("Notify read: " + address);
    }

    int val = memory[address];
    if (word) {
      val |= memory[(address + 1) & 0xffff] << 8;
    }
    return val;
  }

  public void write(int address, int data, boolean word, long cycles) {
    memory[address] = data & 0xff;
    if (word) {
      memory[address + 1] = (data >> 8) & 0xff;
    }

    // This does not handle word writes yet...
    int iAddress = address - offset;

    if (DEBUG) {
      try {
	System.out.println("Writing to " + getName() + ":" +
			   (interrupt > 0? iNames[iAddress] : names[iAddress]) +
			   "  " + Utils.hex8(address) +
			   " => " + Utils.hex8(data) + "=#" +
			   Utils.binary8(data) + " word: " + word);
      } catch (Exception e) {
	e.printStackTrace();
      }
    }

    switch (iAddress) {
    case IN:
      break;
    case OUT:
      out = data;
      if (listener != null) {
        // Any output configured pin (pin-bit = 0) should have 1 here?! 
//        if (name.equals("1"))
//          System.out.println(getName() + " write to IOPort via OUT reg: " + Utils.hex8(data));
        listener.portWrite(this, out | (~dirReg)&0xff);
      }
      break;
    case DIR:
      dirReg = data;
      if (listener != null) {
        // Any output configured pin (pin-bit = 0) should have 1 here?! 
//        if (name.equals("1"))
//          System.out.println(getName() + " write to IOPort via DIR reg: " + Utils.hex8(data));
        listener.portWrite(this, out | (~dirReg)&0xff);
      }
      break;
      // SEL & IFG is the same but behaviour differs between p1,p2 and rest...
    case SEL:
      //   case IFG:
      if (interrupt > 0) {
	// IFG - writing a zero => clear the flag!
	if (DEBUG) {
	  System.out.println(getName() + " Clearing IFlag: " + data);
	}
	interruptFlag &= data;
	memory[offset + IFG] = interruptFlag;
	cpu.flagInterrupt(interrupt, this, (interruptFlag & interruptEnable) > 0);
      } else {
	// Samel as ISEL!!!
      }
      break;
    case IES:
      break;
    case IE:
      interruptEnable = data;
      break;
    case ISEL:
    }
  }

  public String getName() {
    return "Port " + name;
  }

  public void interruptServiced(int vector) {
  }

  // for HW to set hi/low on the pins...
  public void setPinState(int pin, int state) {
    if (pinState[pin] != state) {
      pinState[pin] = state;
      int bit = 1 << pin;
      if (state == PIN_HI) {
        memory[IN + offset] |= bit;
      } else {
        memory[IN + offset] &= ~bit;
      }
      if (interrupt > 0) {
        if ((memory[offset + IES] & bit) == 0) {
          // LO/HI transition
          if (state == PIN_HI) {
            interruptFlag |= bit;
            if (DEBUG) {
              System.out.println(getName() +
                  " Flagging interrupt (HI): " + bit);
            }
          }
        } else {
          // HI/LO transition
          if (state == PIN_LOW) {
            interruptFlag |= bit;
            if (DEBUG) {
              System.out.println(getName() +
                  " Flagging interrupt (LOW): " + bit);
            }
          }
        }
        memory[offset + IFG] = interruptFlag;
        // Maybe this is not the only place where we should flag int?
        cpu.flagInterrupt(interrupt, this, (interruptFlag & interruptEnable) > 0);
      }
      
      if (timerCapture[pin] != null) {
        /* should not be pin and 0 here
         * pin might need configration and 0 can maybe also be 1? 
         */
//        System.out.println(getName() + " Notifying timer of changed pin value");
        timerCapture[pin].capture(pin, 0, state);
      }
      
    }
  }


  public void reset(int type) {
    for (int i = 0, n = 8; i < n; i++) {
      pinState[i] = PIN_LOW;
    }
    interruptFlag = 0;
    interruptEnable = 0;
    cpu.flagInterrupt(interrupt, this, (interruptFlag & interruptEnable) > 0);
  }
}
