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
 * $Id: IOUnit.java,v 1.3 2007/10/21 21:17:34 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * IOUnit
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.core;

public abstract class IOUnit {

  int[] memory;
  int offset;

  public IOUnit(int[] memory, int offset) {
    this.memory = memory;
    this.offset = offset;
  }

  public void reset() {
  }

  public boolean needsTick() {
    return true;
  }

  // Default implementation assums notify write and read on all
  // addresses (should be optimized for each unit)
  public boolean needsWrite(int address) {
    return true;
  }

  public boolean needsRead(int address) {
    return true;
  }

  // Should return the cycle it wants the next tick...
  public long ioTick(long cycles) {
    return cycles + 1000000;
  }

  // write
  // write a value to the IO unit
  public abstract void write(int address, int value, boolean word,
			     long cycles);

  // read
  // read a value from the IO unit
  public abstract int read(int address, boolean word, long cycles);

  public abstract String getName();

  // We should add "Interrupt serviced..." to indicate that its latest
  // Interrupt was serviced...
  public abstract void interruptServiced();

  // Utility function for converting 16 bits data to correct return
  // value depending on address alignment and word/byte mode
  public static int return16(int address, int data, boolean word) {
    if (word) return data;
    // First byte => low byte
    if ((address & 1) == 0) return data & 0xff;
    // Second byte => hi byte
    return (data >> 8) & 0xff;
  }
}
