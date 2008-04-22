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
 * Watchdog
 *
 * Author  : Joakim Eriksson
 * Created : 22 apr 2008
 * Updated : $Date:$
 *           $Revision:$
 */
package se.sics.mspsim.core;

import se.sics.mspsim.util.Utils;

/**
 * @author joakim
 *
 */
public class Watchdog extends IOUnit {

  private static final int WDTCTL = 0x120;

  private static final int WDTHOLD = 0x80;
  private static final int WDTCNTCL = 0x08;
  private static final int WDTSSEL = 0x04;
  private static final int WDTISx = 0x03;
  
  
  private int wdtctl;
  private boolean wdtOn = true;
  private MSP430Core cpu;

  private TimeEvent wdtTrigger = new TimeEvent(0) {
    public void execute(long t) {
//      System.out.println(getName() + " **** executing update timers at " + t + " cycles=" + core.cycles);
      triggerWDT(t);
    }
  };

  
  
  public Watchdog(MSP430Core cpu) {
    super(cpu.memory, 0x120);
    this.cpu = cpu;
  }
   
  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return "Watchdog";
  }

  @Override
  public void interruptServiced(int vector) {
    // TODO Auto-generated method stub

  }

  private void triggerWDT(long time) {
   // Here the WDT triggered!!!
    System.out.println("WDT trigger - should reset?!?!");
  }
  
  public int read(int address, boolean word, long cycles) {
    if (address == WDTCTL) return wdtctl | 0x6900;
    return 0;
  }

  @Override
  public void write(int address, int value, boolean word, long cycles) {
    if (address == WDTCTL) {
      if ((value >> 8) == 0x5a) {
        System.out.println("Wrote to WDTCTL: " + Utils.hex8(wdtctl));
        wdtctl = value & 0xff;
        // Is it on?
        wdtOn = (value & 0x80) == 0;
        if ((value & WDTCNTCL) != 0) {
          // Clear timer - reschedule the event!
        }
      } else {
        // Trigger reset!!
        System.out.println("WDTCTL: illegal write - should reset!!!! " + value);
      }
    }
  }

}
