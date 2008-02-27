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
 * AD12
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.core;

public class ADC12 extends IOUnit {

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

  private int adc12ctl0 = 0;
  
  private int shTime0 = 4;
  private int shTime1 = 4;
  private boolean adc12On = false;
  private boolean enableConversion;
  private boolean startConversion;
  
  public ADC12(MSP430Core cpu) {
    super(cpu.memory, 0);
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
      
      System.out.println(getName() + ": Set SHTime0: " + shTime0 + " SHTime1: " + shTime1 + " ENC:" +
          enableConversion + " Start: " + startConversion + " ADC12ON: " + adc12On);
      break;
    }
  }

  // read a value from the IO unit
  public int read(int address, boolean word, long cycles) {
    return 0;
  }

  public String getName() {
    return "AD12";
  }

  public void interruptServiced(int vector) {
  }

  public long ioTick(long cycles) {
    return cycles + 1000000;
  }

  public int getModeMax() {
    return 0;
  }
}
