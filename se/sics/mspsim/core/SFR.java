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
 * $Id: SFR.java,v 1.3 2007/10/21 21:17:34 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * SFR
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.core;

/**
 * SFR - emulation of special function registers
 */
public class SFR extends IOUnit {

  public static final int IE1 = 0;
  public static final int IE2 = 1;
  public static final int IFG1 = 2;
  public static final int IFG2 = 3;
  public static final int ME1 = 4;
  public static final int ME2 = 5;

  private int ie1 = 0;
  private int ie2 = 0;
  private int ifg1 = 0;
  private int ifg2 = 0;
  private int me1 = 0;
  private int me2 = 0;

  private int[] memory;
  private MSP430Core cpu;

  public SFR(MSP430Core cpu, int[] memory) {
    super(memory, 0);
    this.cpu = cpu;
    this.memory = memory;
  }

  public void reset() {
  }

  public boolean needsTick() {
    return false;
  }

  // write
  // write a value to the IO unit
  public void write(int address, int value, boolean word,
			     long cycles) {
    switch (address) {
    case IE1:
      ie1 = value;
      break;
    case IE2:
      ie2 = value;
      break;
    case IFG1:
      ifg1 = value;
      break;
    case IFG2:
      ifg2 = value;
      break;
    case ME1:
      me1 = value;
      break;
    case ME2:
      me2 = value;
      break;
    default:
      memory[address] = value;
    }
  }

  // read
  // read a value from the IO unit
  public int read(int address, boolean word, long cycles) {
    switch (address) {
    case IE1:
      return ie1;
    case IE2:
      return ie2;
    case IFG1:
      return ifg1;
    case IFG2:
      return ifg2;
    case ME1:
      return me1;
    case ME2:
      return me2;
    default:
      return memory[address];
    }
  }

  public void setBitIFG(int index, int bits) {
    if (index == 0) ifg1 |= bits;
    else ifg2 |= bits;
  }

  public void clrBitIFG(int index, int bits) {
    if (index == 0) ifg1 &= ~bits;
    else ifg2 &= ~bits;
  }

  public boolean isIEBitsSet(int index, int flags) {
    if (index == 0) return (ie1 & flags) == flags;
    else return (ie2 & flags) == flags;
  }

  public int getIFG(int index) {
    if (index == 0) return ifg1;
    else return ifg2;
  }

  public void interruptServiced() {
  }

  public String getName() {
    return "SpecialFunctionRegister, SFR";
  }
} // SFR
