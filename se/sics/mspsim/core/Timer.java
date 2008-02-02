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
 * $Id: Timer.java,v 1.3 2007/10/21 21:17:34 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * Timer
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.core;
import se.sics.mspsim.util.Utils;

/**
 * Timer.java
 *
 * How should ports be connected to clock capture???
 * E.g. if port 1.2 give a signal then a capture is made on T_A[2]?!
 * (if it is configured to do that).
 * => some kind of listener on the ports ???
 *
 * ===> how do we capture the internal clocks
 * TACTL2 => ACLK if configured for that
 * - same as any port - some kind of listener that we add when
 *   the configuration is in that way.?!
 * All low-level clocks should probably be ticked by the "cpu"-loop in
 * some way, but configured by the BasicClockModule, otherwise it will
 * be too time consuming (probably).
 * CLCK Capture needs to be moved into the CPU since it is "time-critical"...
 * Other captures (ports, etc) could be handled separately (i think)
 *
 * Several capturers can be "looking" at the same signal
 * and capture at different edges - how implement that efficiently?
 *
 * ___---___---___
 *
 * ==> Reads might be another problem. If a loop is just cheking the
 * counter it will be reading same value for a long time. Needs to "capture"
 * reads to Timers by some simple means...
 */
public class Timer extends IOUnit {

  public static final boolean DEBUG = false;

  public static final int TIMER_A = 0;
  public static final int TIMER_B = 1;
  private String[] name = new String[] {"A", "B"};

  public static final int TBIV = 0x011e;
  public static final int TAIV = 0x012e;

  public static final int TACCR0_VECTOR = 6;
  // Other is on 5
  public static final int TACCR1_VECTOR = 5;

  public static final int TBCCR0_VECTOR = 13;
  // Other is on 12
  public static final int TBCCR1_VECTOR = 12;

  public static final int TCTL = 0;
  public static final int TCCTL0 = 2;
  public static final int TCCTL1 = 4;
  public static final int TCCTL2 = 6;
  public static final int TCCTL3 = 8;
  public static final int TCCTL4 = 0xa;
  public static final int TCCTL5 = 0xc;
  public static final int TCCTL6 = 0xe;

  public static final int TR = 0x10;
  public static final int TCCR0 = 0x12;
  public static final int TCCR1 = 0x14;
  public static final int TCCR2 = 0x16;
  public static final int TCCR3 = 0x18;
  public static final int TCCR4 = 0x1a;
  public static final int TCCR5 = 0x1c;
  public static final int TCCR6 = 0x1e;

  public static final int STOP = 0;
  public static final int UP = 1;
  public static final int CONTIN = 2;
  public static final int UPDWN = 3;

  // Different capture modes...
  public static final int CAP_NONE = 0;
  public static final int CAP_UP = 1;
  public static final int CAP_DWN = 2;
  public static final int CAP_BOTH = 3;

  public static final int TCLR = 0x4;

  public static final int SRC_ACLK = 0;
  public static final int SRC_MCLK = 1;
  public static final int SRC_SMCLK = 2;
  public static final int SRC_PORT = 0x100;
  public static final int SRC_GND = 0x200;
  public static final int SRC_VCC = 0x201;
  public static final int SRC_CAOUT = 0x202; // Internal ??? What is this?

  public static final int CC_IFG = 0x01; // Bit 0
  public static final int CC_IE = 0x10;  // Bit 4
  public static final int CC_TRIGGER_INT = CC_IE | CC_IFG;

  private long lastCycles = 0;
  private long counterStart = 0;

  // Input map for timer A
  public static final int[] TIMER_Ax149 = new int[] {
    SRC_PORT + 0x10, SRC_ACLK, SRC_SMCLK, SRC_PORT + 0x21, // Timer
    SRC_PORT + 0x11, SRC_PORT + 0x22, SRC_GND, SRC_VCC,    // Cap 0
    SRC_PORT + 0x12, SRC_CAOUT, SRC_GND, SRC_VCC,          // Cap 1
    SRC_PORT + 0x13, SRC_ACLK, SRC_GND, SRC_VCC            // Cap 2
  };

  // Input map for timer B
  public static final int[] TIMER_Bx149 = new int[] {
    SRC_PORT + 0x47, SRC_ACLK, SRC_SMCLK, SRC_PORT + 0x47, // Timer
    SRC_PORT + 0x40, SRC_PORT + 0x40, SRC_GND, SRC_VCC,    // Cap 0
    SRC_PORT + 0x41, SRC_PORT + 0x41, SRC_GND, SRC_VCC,    // Cap 1
    SRC_PORT + 0x42, SRC_PORT + 0x42, SRC_GND, SRC_VCC,    // Cap 2
    SRC_PORT + 0x43, SRC_PORT + 0x43, SRC_GND, SRC_VCC,    // Cap 3
    SRC_PORT + 0x44, SRC_PORT + 0x44, SRC_GND, SRC_VCC,    // Cap 4
    SRC_PORT + 0x45, SRC_PORT + 0x45, SRC_GND, SRC_VCC,    // Cap 5
    SRC_PORT + 0x46, SRC_ACLK, SRC_GND, SRC_VCC            // Cap 6
  };

  public static final String[] capNames = new String[] {
    "NONE", "RISING", "FALLING", "BOTH"
  };

  private int type = 0;
  private int inputDivider = 1;

  // If clocked by anything other than the SubMainClock at full
  // speed this needs to be calculated for correct handling.
  // Should be something like inputDivider * SMCLK_SPEED / CLK_SRC_SPEED;
  private int cyclesMultiplicator = 1;

  private int clockSource;
  private int mode;

  private int counter = 0;
  private int countDirection = 1;

  // The IO registers
  private int[] tcctl = new int[7];
  private int[] tccr = new int[7];

  // Support variables Max 7 compare regs... (timer b)
  private int noCompare = 0;
  private int[] expCompare = new int[7];
  private int[] expCapInterval = new int[7];
  private long[] expCaptureTime = new long[7];

  private int[] capMode = new int[7];
  private boolean[] captureOn = new boolean[7];
  private int[] inputSel = new int[7];
  private int[] inputSrc = new int[7];
  private boolean[] sync = new boolean[7];
  private int[] outMode = new int[7];

  private boolean interruptEnable = false;
  private boolean interruptPending = false;


  private MSP430Core core;

  private int lastTIV;

  private int[] srcMap;
  /**
   * Creates a new <code>Timer</code> instance.
   *
   */
  public Timer(MSP430Core core, int[] srcMap, int[] memory, int offset) {
    super(memory, offset);
    this.srcMap = srcMap;
    this.core = core;
    noCompare = (srcMap.length / 4) - 1;
    if (DEBUG) {
      System.out.println("Timer: noComp:" + noCompare);
    }
    if (srcMap == TIMER_Ax149) {
      type = TIMER_A;
    } else {
      type = TIMER_B;
    }
    reset();
  }

  public void reset() {
    for (int i = 0, n = expCompare.length; i < n; i++) {
      expCompare[i] = -1;
      expCaptureTime[i] = -1;
    }
  }

  // Should handle read of byte also (currently ignores that...)
  public int read(int address, boolean word, long cycles) {

    int val = memory[address];
    if (word) {
      val |= memory[(address + 1) & 0xffff] << 8;
    }

    if (address == TAIV || address == TBIV) {
      // should clear registers for cause of interrupt (highest value)?
      // but what if a higher value have been triggered since this was
      // triggered??? -> does that matter???
      // But this mess the TIV up too early......
      // Must DELAY the reset of interrupt flags until next read...?
      resetTIV();
    }

    if (address != 0x166) {
      if (DEBUG && false) {
	System.out.println("Read: " + Utils.hex16(address) + " => " +
			   Utils.hex16(memory[address] +
				       (memory[address + 1] << 8)) + " (" +
			   (memory[address] + (memory[address + 1] << 8)) + ")");
      }
    }

    int index = address - offset;
    switch(index) {
    case TR:
      return updateCounter(cycles);
    case TCCTL0:
    case TCCTL1:
    case TCCTL2:
    case TCCTL3:
    case TCCTL4:
    case TCCTL5:
    case TCCTL6:
      int i = (index - TCCTL0) / 2;
      return tcctl[i];
    case TCCR0:
    case TCCR1:
    case TCCR2:
    case TCCR3:
    case TCCR4:
    case TCCR5:
    case TCCR6:
      i = (index - TCCR0) / 2;
      return tccr[i];
    }

    // It reads the interrupt flag for capture...
    return val;
  }

  private void resetTIV() {
    if (lastTIV / 2 < noCompare) {
      if (DEBUG) {
	System.out.println(getName() + " Clearing TIV/Comparex2: " + lastTIV);
      }
      // Clear interrupt flags!
      tcctl[lastTIV / 2] &= ~CC_IFG;
      triggerInterrupts();
      //      memory[offset + TCCTL0 + lastTIV] &= ~CC_IFG;
    }
  }

  public void write(int address, int data, boolean word, long cycles) {
    memory[address] = data & 0xff;
    if (word) {
      memory[address + 1] = (data >> 8) & 0xff;
    }

    // This does not handle word/byte difference yet... assumes it gets
    // all 16 bits when called!!!

    int iAddress = address - offset;

    switch (iAddress) {
    case TR:
      setCounter(data, cycles);
      break;
    case TCTL:
      if (DEBUG) {
        System.out.println(getName() + " wrote to TCTL: " + data);
      }
      inputDivider = 1 << ((data >> 6) & 3);
      clockSource = srcMap[(data >> 8) & 3];

      cyclesMultiplicator = inputDivider;
      if (clockSource == SRC_ACLK) {
	cyclesMultiplicator = (cyclesMultiplicator * core.smclkFrq) /
	  core.aclkFrq;
	if (DEBUG) {
	  System.out.println(getName() + " setting multiplicator to: " +
			     cyclesMultiplicator);
	}
      }

      mode = (data >> 4) & 3;

      if ((data & TCLR) != 0) {
	counter = 0;
	counterStart = cycles;
	// inputDivider = 1; ????
	countDirection = 1;
	// Clear this bit...
	memory[address] &= ~4;
      }

      interruptEnable = (data & 1) > 0;

      if (DEBUG) {
	System.out.println(getName() + " Write: Timer_" + name[type] +
			   " CTL: inDiv:" + inputDivider +
			   " src: " + getSourceName(clockSource) +
			   " IE:" + interruptEnable + " IP: " +
			   interruptPending + " mode: " + mode +
			   ((data & TCLR) != 0 ? " CLR" : ""));
      }

      // Write back the interrupt pending info...
      if (interruptPending) {
	memory[address] |= 1;
      } else {
	memory[address] &= 0xfe;
      }

      updateCaptures(-1, cycles);
      break;
    case TCCTL0:
    case TCCTL1:
    case TCCTL2:
    case TCCTL3:
    case TCCTL4:
    case TCCTL5:
    case TCCTL6:
      // Control register...
      int index = (iAddress - TCCTL0) / 2;
      tcctl[index] = data;
      outMode[index] = (data >> 5)& 7;
      captureOn[index] = (data & 0x100) > 0;
      sync[index] = (data & 0x800) > 0;
      inputSel[index] = (data >> 12) & 3;
      inputSrc[index] = srcMap[4 + index * 4 + inputSel[index]];
      capMode[index] = (data >> 14) & 3;

      triggerInterrupts();

      if (DEBUG) {
	System.out.println(getName() + " Write: CCTL" +
			   index + " => " + Utils.hex16(data) +
			   " CM: " + capNames[capMode[index]] +
			   " CCIS:" + inputSel[index] + " name: " +
			   getSourceName(inputSrc[index]) +
			   " Capture: " + captureOn[index] +
			   " IE: " + ((data & CC_IE) != 0));
      }
      updateCaptures(index, cycles);
      break;
      // Write to compare register!
    case TCCR0:
    case TCCR1:
    case TCCR2:
    case TCCR3:
    case TCCR4:
    case TCCR5:
    case TCCR6:
      // update of compare register
      index = (iAddress - TCCR0) / 2;
      tccr[index] = data;
      updateCounter(cycles);
      if (DEBUG) {
	System.out.println(getName() +
			   " Write: Setting compare " + index + " to " +
			   Utils.hex16(data) + " TR: " +
			   Utils.hex16(counter));
      }
      int diff = data - counter;
      if (diff < 0) {
	// Ok we need to wrap!
	diff += 0x10000;
      }
      expCaptureTime[index] = cycles + cyclesMultiplicator * diff;
      if (DEBUG) {
	System.out.println(getName() + " Cycles: " + cycles + " expCap[" + index + "]: " + expCaptureTime[index] + " ctr:" + counter +
			   " data: " + data + " ~" +
			   (100 * (cyclesMultiplicator * diff * 1L) / 2500000) / 100.0 + " sec");
      }
    }
  }

  private void setCounter(int newCtr, long cycles) {
    counter = newCtr;
    counterStart = cycles;
  }

  private void updateCaptures(int index, long cycles) {
    int low = 0;
    int hi = noCompare;
    if (index != -1) {
      low = index;
      hi = index + 1;
    }

    // A hack to handle the SMCLK synchronization
    for (int i = 0, n = hi; i < n; i++) {
      int divisor = 1;
      int frqClk = 1;

      if (clockSource == SRC_SMCLK) {
	frqClk = core.smclkFrq / inputDivider;
      } else if (clockSource == SRC_ACLK) {
	frqClk = core.aclkFrq / inputDivider;
      }

      // Handle the captures...
      if (captureOn[i]) {
	if (inputSrc[i] == SRC_ACLK) {
	  divisor = core.aclkFrq;
	}

	if (DEBUG) {
	  System.out.println(getName() + " expCapInterval[" + i + "] frq = " +
			     frqClk + " div = " + divisor);
	}

	// This is used to calculate expected time before next capture of
	// clock-edge to occur - including what value the compare reg. will get
	expCapInterval[i] = frqClk / divisor;
	// This is not 100% correct - depending on clock mode I guess...
	expCompare[i] = (counter + expCapInterval[i]) & 0xffff;
	// This could be formulated in something other than cycles...
	// ...??? should be multiplied with clockspeed diff also?
	expCaptureTime[i] = cycles + expCapInterval[i] * cyclesMultiplicator;
	if (DEBUG) {
	  System.out.println(getName() +
			     " Expected compare " + i +
			     " => " + expCompare[i]);
	  System.out.println(getName() +
			     " Expected cap time: " + expCaptureTime[i]);
	  System.out.println("Capture: " + captureOn[i]);
	}
      }
    }
  }

  private int updateCounter(long cycles) {
    // Needs to be non-integer since smclk Frq can be lower
    // than aclk
    double divider = 1;
    if (clockSource == SRC_ACLK) {
      // Should later be divided with DCO clock?
      divider = 1.0 * core.smclkFrq / core.aclkFrq;
    }
    divider = divider * inputDivider;
    long cycctr = cycles - counterStart;
    switch (mode) {
    case CONTIN:
      counter = ((int) (cycctr / divider)) & 0xffff;
      break;
    case UP:
      counter = ((int) (cycctr / divider)) % tccr[0];
      break;
    case UPDWN:
      counter = ((int) (cycctr / divider)) % (tccr[0] * 2);
      if (counter > tccr[0]) {
	// Should back down to start again!
	counter = 2 * tccr[0] - counter;
      }
    }
//    System.out.println(getName() + "Updating counter cycctr: " + cycctr + " divider: " + divider + " mode:" + mode + " => " + counter);

   return counter;
  }

  // Simplest possible - just a call each 1000 cycles (which is wrong...)
  public long ioTick(long cycles) {

    // This will not work very good...
    // But the timer does not need to be updated this often...
    // Do we need to update the counter here???
    //    System.out.println("Checking capture register [ioTick]: " + cycles);
    for (int i = 0, n = noCompare; i < n; i++) {
      //      System.out.println("Checking: " + i);
      if (expCaptureTime[i] != -1 && cycles > expCaptureTime[i]) {
	if (DEBUG) {
	  System.out.println(getName() + " CAPTURE: " + i +
			     " Cycles: " + cycles + " expCap: " +
			     expCaptureTime[i] +
			     " => ExpCR: " + Utils.hex16(expCompare[i]) +
			     " TR: " + Utils.hex16(updateCounter(cycles)));
	}
	// Set the interrupt flag...
	tcctl[i] |= CC_IFG;

	if (captureOn[i]) {
	  // Write the expected capture time to the register (counter could
	  // differ slightly)
	  tccr[i] = expCompare[i];
	  // Update capture times... for next capture
	  expCompare[i] = (expCompare[i] + expCapInterval[i]) & 0xffff;
	  expCaptureTime[i] += expCapInterval[i] * cyclesMultiplicator;
	  if (DEBUG) {
	    System.out.println(getName() +
			       " setting expCaptureTime to next capture: " +
			       expCaptureTime[i]);
	  }
	} else {
	  // Update expected compare time for this compare/cap reg.
	  // 0x10000 cycles... e.g. a full 16 bits wrap of the timer
	  expCaptureTime[i] = expCaptureTime[i] +
	    0x10000 * cyclesMultiplicator;
	  if (DEBUG) {
	    System.out.println(getName() +
			       " setting expCaptureTime to full wrap: " +
			       expCaptureTime[i]);
	  }
	}

	if (DEBUG) {
	  System.out.println("Wrote to: " +
			     Utils.hex16(offset + TCCTL0 + i * 2 + 1));
	}
      }
    }

    // Trigger interrupts that are up for triggering!
    triggerInterrupts();


//     System.out.println("Writer: timer ctr = " + Utils.hex16(counter));
    return 1000 + cycles;
  }

  // Can be called to generate any interrupt...
  public void triggerInterrupts() {
    // First check if any capture register is generating an interrupt...
    boolean trigger = false;
    int tIndex = 0;
    for (int i = 0, n = noCompare; i < n; i++) {
      boolean newTrigger = (tcctl[i] & CC_TRIGGER_INT) == CC_TRIGGER_INT;
      trigger = trigger | newTrigger;

      // This only triggers interrupts - reading TIV clears!??!
      if (i == 0) {
	// Execute the interrupt vector... the high-pri one...
	core.flagInterrupt(type == TIMER_A ? TACCR0_VECTOR : TBCCR0_VECTOR,
			   this, trigger);
	// Trigger this!
	// This is handled by its own vector!!!
	if (trigger) {
	  lastTIV = memory[type == TIMER_A ? TAIV : TBIV] = 0;
	  trigger = false;
	}
      } else {
	// Which would have the highest pri? Will/should this trigger more
	// than one interrupt at a time?!!?!?!
	// If so, which TIV would be the correct one?
	if (newTrigger) {
	  if (DEBUG) {
	    System.out.println(getName() + " triggering interrupt TIV: " +
			       (i * 2));
	  }
	  tIndex = i;
	}
      }
    }
    core.flagInterrupt(type == TIMER_A ? TACCR1_VECTOR : TBCCR1_VECTOR,
		       this, trigger);
    if (trigger) {
      // Or if other CCR execute the normal one with correct TAIV
      lastTIV = memory[type == TIMER_A ? TAIV : TBIV] = tIndex * 2;
    }
  }

  public String getSourceName(int source) {
    switch (source) {
      case SRC_ACLK:
      return "ACLK";
    case SRC_VCC:
      return "VCC";
    case SRC_GND:
      return "GND";
    case SRC_SMCLK:
      return "SMCLK";
    default:
      if ((source & SRC_PORT) == SRC_PORT) {
	return "Port " + ((source & 0x10) >> 4) + "." +
	  (source & 0xf);
      }
    }
    return "";
  }

  public String getName() {
    return "Timer " + name[type];
  }


  // The interrupt have been serviced...
  // Some flags should be cleared (the highest priority flags)?
  public void interruptServiced() {
    if (MSP430Core.debugInterrupts) {
      System.out.println("interrupt Serviced...");
    }
  }

  public int getModeMax() {
    return 0;
  }
}
