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
 * ADC12
 *
 * Each time a sample is converted the ADC12 system will check for EOS flag
 * and if not set it just continues with the next conversion (x + 1). 
 * If EOS next conv is startMem.
 * Interrupt is triggered when the IE flag are set! 
 *
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.core;

import com.sun.org.apache.xpath.internal.operations.Div;

public class ADC12 extends IOUnit {

  private static final boolean DEBUG = true; //false;
  
  public static final int ADC12CTL0 = 0x01A0;// Reset with POR
  public static final int ADC12CTL1 = 0x01A2;// Reset with POR
  public static final int ADC12IFG = 0x01A4; //Reset with POR
  public static final int ADC12IE = 0x01A6; //Reset with POR
  public static final int ADC12IV = 0x01A8; //Reset with POR
  public static final int ADC12MEM0 = 0x0140; //Unchanged
  public static final int ADC12MEM1 = 0x0142; //Unchanged
  public static final int ADC12MEM2 = 0x0144; //Unchanged
  public static final int ADC12MEM3 = 0x0146; //Unchanged
  public static final int ADC12MEM4 = 0x0148; //Unchanged
  public static final int ADC12MEM5 = 0x014A; //Unchanged
  public static final int ADC12MEM6 = 0x014C; //Unchanged
  public static final int ADC12MEM7 = 0x014E; //Unchanged
  public static final int ADC12MEM8 = 0x0150; //Unchanged
  public static final int ADC12MEM9 = 0x0152; //Unchanged
  public static final int ADC12MEM10 = 0x0154; //Unchanged
  public static final int ADC12MEM11 = 0x0156; //Unchanged
  public static final int ADC12MEM12 = 0x0158; //Unchanged
  public static final int ADC12MEM13 = 0x015A; //Unchanged
  public static final int ADC12MEM14 = 0x015C; //Unchanged
  public static final int ADC12MEM15 = 0x015E; //Unchanged
  public static final int ADC12MCTL0 = 0x080; //Reset with POR
  public static final int ADC12MCTL1 = 0x081; //Reset with POR
  public static final int ADC12MCTL2 = 0x082; //Reset with POR
  public static final int ADC12MCTL3 = 0x083; //Reset with POR
  public static final int ADC12MCTL4 = 0x084; //Reset with POR
  public static final int ADC12MCTL5 = 0x085; //Reset with POR
  public static final int ADC12MCTL6 = 0x086; //Reset with POR
  public static final int ADC12MCTL7 = 0x087; //Reset with POR
  public static final int ADC12MCTL8 = 0x088; //Reset with POR
  public static final int ADC12MCTL9 = 0x089; //Reset with POR
  public static final int ADC12MCTL10 = 0x08A; //Reset with POR
  public static final int ADC12MCTL11 = 0x08B; //Reset with POR
  public static final int ADC12MCTL12 = 0x08C; //Reset with POR
  public static final int ADC12MCTL13 = 0x08D; //Reset with POR
  public static final int ADC12MCTL14 = 0x08E; //Reset with POR
  public static final int ADC12MCTL15 = 0x08F; //Reset with POR
  
  public static final int[] SHTBITS = new int[] {
    4, 8, 16, 32, 64, 96, 128, 192,
    256, 384, 512, 768, 1024, 1024, 1024, 1024
  };

  public static final int EOS_MASK = 0x80;
  
  
  private int adc12ctl0 = 0;
  private int adc12ctl1 = 0;
  private int[] adc12mctl = new int[16]; 
  private int[] adc12mem = new int[16]; 
  private int adc12Pos = 0;
  
  private int shTime0 = 4;
  private int shTime1 = 4;
  private boolean adc12On = false;
  private boolean enableConversion;
  private boolean startConversion;
  
  private int shSource = 0;
  private int startMem = 0;
  private int adcDiv = 1;

  private ADCInput adcInput[] = new ADCInput[8];
  
  private int conSeq;
  private int adc12ie;
  private int adc12ifg;
  private int adc12iv;
  
  private int adcSSel;
  private MSP430Core core;
  private int adc12Vector = 7;

  private TimeEvent adcTrigger = new TimeEvent(0) {
    public void execute(long t) {
//      System.out.println(getName() + " **** executing update timers at " + t + " cycles=" + core.cycles);
      convert();
    }
  };

  
  public ADC12(MSP430Core cpu) {
    super(cpu.memory, 0);
    core = cpu;
  }
  
  public void setADCInput(int adindex, ADCInput input) {
    adcInput[adindex] = input;
  }

  // write a value to the IO unit
  public void write(int address, int value, boolean word,
			     long cycles) {
    switch (address) {
    case ADC12CTL0:
      adc12ctl0 = value;
      shTime0 = SHTBITS[(value >> 8) & 0x0f];
      shTime1 = SHTBITS[(value >> 12) & 0x0f];
      adc12On = (value & 0x10) > 0;
      enableConversion = (value & 0x02) > 0;
      startConversion = (value & 0x01) > 0;
      
      if (DEBUG) System.out.println(getName() + ": Set SHTime0: " + shTime0 + " SHTime1: " + shTime1 + " ENC:" +
          enableConversion + " Start: " + startConversion + " ADC12ON: " + adc12On);
      if (adc12On && enableConversion && startConversion) {
        convert();
      }
      break;
    case ADC12CTL1:
      adc12ctl1 = value;
      startMem = (value >> 12) & 0xf;
      shSource = (value >> 10) & 0x3;
      adcDiv = ((value >> 5) & 0x7) + 1;
      conSeq = (value >> 1) & 0x03;
      adcSSel = (value >> 3) & 0x03;
      if (DEBUG) System.out.println(getName() + ": Set startMem: " + startMem + " SHSource: " + shSource +
          " ConSeq-mode:" + conSeq + " Div: " + adcDiv + " ADCSSEL: " + adcSSel);
      break;
    case ADC12IE:
      adc12ie = value;
      break;
    case ADC12IFG:
      adc12ifg = value;
      break;
    default:
      if (address >= ADC12MCTL0 && address <= ADC12MCTL15)  {
        adc12mctl[address - ADC12MCTL0] = value & 0xff;
        System.out.println("ADC12MCTL" + (address - ADC12MCTL0) + " source = " + (value & 0xf));
        if ((value & 0x80) != 0) {
          System.out.println("ADC12MCTL" + (address - ADC12MCTL0) + " EOS bit set");
        }
      }
    }
  }

  // read a value from the IO unit
  public int read(int address, boolean word, long cycles) {
    switch(address) {
    case ADC12CTL0:
      return adc12ctl0;
    case ADC12CTL1:
      return adc12ctl1;
    case ADC12IE:
      return adc12ie;
    case ADC12IFG:
      return adc12ifg;
    default:
      if (address >= ADC12MCTL0 && address <= ADC12MCTL15)  {
        return adc12mctl[address - ADC12MCTL0];
      } else if (address >= ADC12MEM0) {
        int reg = address - ADC12MEM0;
        // Clear ifg!
        adc12ifg &= ~(1 << reg);
//        System.out.println("Read ADCMEM" + (reg / 2));        
        if (adc12iv == reg + 6) {
          core.flagInterrupt(adc12Vector, this, false);
          adc12iv = 0;
//          System.out.println("** de-Trigger ADC12 IRQ for ADCMEM" + adc12Pos);
        }
        return adc12mem[reg];
      }
    }
    return 0;
  }

  public String getName() {
    return "ADC12";
  }

  private void convert() {
    // Some noice...
    ADCInput input = adcInput[adc12mctl[adc12Pos] & 0x7];
    adc12mem[adc12Pos] = input != null ? input.nextData() : 2048 + 100 - (int) Math.random() * 200;
    if ((adc12ie & (1 << adc12Pos)) > 0) {
      adc12ifg |= (1 << adc12Pos);
      // This should check if there already is an hihger iv!
      adc12iv = adc12Pos * 2 + 6;
//      System.out.println("** Trigger ADC12 IRQ for ADCMEM" + adc12Pos);
      core.flagInterrupt(adc12Vector, this, true);
    }
    // Increase
    if ((adc12mctl[adc12Pos] & EOS_MASK) == EOS_MASK) {
      adc12Pos = startMem;
    } else {
      adc12Pos = (adc12Pos + 1) & 0x0f;
    }
    int delay = adcDiv * (shTime0 + 13);
    //System.out.println("Sampling again after: " + delay + " => " + adc12Pos);
    core.scheduleTimeEvent(adcTrigger, adcTrigger.time + delay);
  }
  
  public void interruptServiced(int vector) {
  }
}
