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
 * Timer
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
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
 * ==> Reads might be another problem. If a loop is just checking the
 * counter it will be reading same value for a long time. Needs to "capture"
 * reads to Timers by some simple means...
 */
public class Timer extends IOUnit {

  public static final boolean DEBUG = false;//true;

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

  public static final int CC_I = 0x08;
  public static final int CC_IFG = 0x01; // Bit 0
  public static final int CC_IE = 0x10;  // Bit 4
  public static final int CC_TRIGGER_INT = CC_IE | CC_IFG;

  public static final int CM_NONE = 0;
  public static final int CM_RISING = 1;
  public static final int CM_FALLING = 2;
  public static final int CM_BOTH = 3;

  
  // Number of cycles passed since current counter value was set
  // useful for setting expected compare and capture times to correct time.
  // valid for timer A
  private final int timerOverflow;
  private long nextTimerTrigger = 0;
  
  // this is used to create "tick" since last reset of the timer.
  // it will contain the full number of ticks since that reset and
  // is used to calculate the real counter value
  private long counterStart = 0;
  private long counterAcc;

  // Counter stores the current timer counter register (TR)
  private int counter = 0;
  private int counterPassed = 0;

  // Input map for timer A
  public static final int[] TIMER_Ax149 = new int[] {
    SRC_PORT + 0x10, SRC_ACLK, SRC_SMCLK, SRC_PORT + 0x21, // Timer
    SRC_PORT + 0x11, SRC_PORT + 0x22, SRC_GND, SRC_VCC,    // Cap 0
    SRC_PORT + 0x12, SRC_CAOUT, SRC_GND, SRC_VCC,          // Cap 1
    SRC_PORT + 0x13, SRC_ACLK, SRC_GND, SRC_VCC            // Cap 2
  };

  // Input map for timer B (configurable in later versions for other MSP430 versions)
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

  private final String name;
  private final int tiv;
  private int inputDivider = 1;

  // If clocked by anything other than the SubMainClock at full
  // speed this needs to be calculated for correct handling.
  // Should be something like inputDivider * SMCLK_SPEED / CLK_SRC_SPEED;
  private double cyclesMultiplicator = 1;

  private int clockSource;
  private int clockSpeed;
  private int mode;
  
  // The IO registers
  private int tctl;
  private int[] tcctl = new int[7];
  private int[] tccr = new int[7];

  // Support variables Max 7 compare regs for now (timer b)
  private final int noCompare;
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

  private final int ccr1Vector;
  private final int ccr0Vector;
  private final MSP430Core core;

  private TimeEvent timerTrigger = new TimeEvent(0) {
    public void execute(long t) {
//      System.out.println(getName() + " **** executing update timers at " + t + " cycles=" + core.cycles);
      updateTimers(core.cycles);
    }
  };
  
  private int lastTIV;

  private final int[] srcMap;

  private long triggerTime;
  
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
      name = "Timer A";
      tiv = TAIV;
      timerOverflow = 0x0a;
      ccr0Vector = TACCR0_VECTOR;
      ccr1Vector = TACCR1_VECTOR;
    } else {
      name = "Timer B";
      tiv = TBIV;
      timerOverflow = 0x0e;
      ccr0Vector = TBCCR0_VECTOR;
      ccr1Vector = TBCCR1_VECTOR;
    }

    reset(0);
  }

  public void reset(int type) {
    for (int i = 0, n = expCompare.length; i < n; i++) {
      expCompare[i] = -1;
      expCaptureTime[i] = -1;
      expCapInterval[i] = 0;
      outMode[i] = 0;
      capMode[i] = 0;
      inputSel[i] = 0;
      inputSrc[i] = 0;
      captureOn[i] = false;
    }
    for (int i = 0; i < tcctl.length; i++) {
      tcctl[i] = 0;
      tccr[i] = 0;
    }
    tctl = 0;
    lastTIV = 0;
    interruptEnable = false;
    interruptPending = false;
    counter = 0;
    counterPassed = 0;
    counterStart = 0;
    counterAcc = 0;
    clockSource = 0;
    cyclesMultiplicator = 1;
    mode = STOP;
    nextTimerTrigger = 0;
    inputDivider = 1;
  }

  // Should handle read of byte also (currently ignores that...)
  public int read(int address, boolean word, long cycles) {
    if (address == TAIV || address == TBIV) {
      // should clear registers for cause of interrupt (highest value)?
      // but what if a higher value have been triggered since this was
      // triggered??? -> does that matter???
      // But this mess the TIV up too early......
      // Must DELAY the reset of interrupt flags until next read...?
      int val = lastTIV;
      resetTIV(cycles);
      return val;
    }
    int val = 0;
    int index = address - offset;
    switch(index) {
    case TR:
      val = updateCounter(cycles);
//      System.out.println(getName() + " TR read => " + val);
      break;
    case TCTL:
      val = tctl;
      if (interruptPending) {
        val |= 1;
      } else {
        val &= 0xfffe;
      }
      if (DEBUG) {
        System.out.println(getName() + " Read: " +
            " CTL: inDiv:" + inputDivider +
            " src: " + getSourceName(clockSource) +
            " IEn:" + interruptEnable + " IFG: " +
            interruptPending + " mode: " + mode);
      }
      break;
    case TCCTL0:
    case TCCTL1:
    case TCCTL2:
    case TCCTL3:
    case TCCTL4:
    case TCCTL5:
    case TCCTL6:
      int i = (index - TCCTL0) / 2;
      updateTCCTL(i, cycles);
      val = tcctl[i];
      break;
    case TCCR0:
    case TCCR1:
    case TCCR2:
    case TCCR3:
    case TCCR4:
    case TCCR5:
    case TCCR6:
      i = (index - TCCR0) / 2;
      val = tccr[i];
      break;
    default:
      System.out.println("Not supported read, returning zero!!!");
    }
    
    if (DEBUG && false) {
      System.out.println(getName() + ": Read " + getName(address) + "(" + Utils.hex16(address) + ") => " +
          Utils.hex16(val) + " (" + val + ")");
    }

    // It reads the interrupt flag for capture...
    return val & 0xffff;
  }

  /* here we need to update things such as CCI / Capture/Compare Input value
   * and other dynamic values
   */
  private void updateTCCTL(int cctl, long cycles) {
    // TODO Auto-generated method stub
    // update the CCI depending on speed of clocks...
    boolean input = false;
    /* if ACLK we can calculate edge... */
    if (inputSrc[cctl] == SRC_ACLK) {
      /* needs the TimerA clock speed here... */
      int aTicks = clockSpeed / core.aclkFrq;
      updateCounter(cycles);
      if (counter % aTicks > aTicks / 2) {
        input = true;
      }
    }
    tcctl[cctl] = (tcctl[cctl] & ~CC_I) | (input ? CC_I : 0);    
  }

  private void resetTIV(long cycles) {
    if (lastTIV == timerOverflow) {
      interruptPending = false;
      lastTIV = 0;
      if (DEBUG) {
        System.out.println(getName() + " Clearing TIV - overflow ");
      }
      triggerInterrupts(cycles);
    }
    if (lastTIV / 2 < noCompare) {
      if (DEBUG) {
	System.out.println(getName() + " Clearing IFG for CCR" + (lastTIV/2));
      }
      // Clear interrupt flags!
      tcctl[lastTIV / 2] &= ~CC_IFG;
      triggerInterrupts(cycles);
    }
  }

  public void write(int address, int data, boolean word, long cycles) {
    // This does not handle word/byte difference yet... assumes it gets
    // all 16 bits when called!!!

    if (address == TAIV || address == TBIV) {
      // should clear registers for cause of interrupt (highest value)?
      // but what if a higher value have been triggered since this was
      // triggered??? -> does that matter???
      // But this mess the TIV up too early......
      // Must DELAY the reset of interrupt flags until next read...?
      resetTIV(cycles);
    }


    int iAddress = address - offset;

    switch (iAddress) {
    case TR:
      setCounter(data, cycles);
      break;
    case TCTL:
      if (DEBUG) {
        System.out.println(getName() + " wrote to TCTL: " + Utils.hex16(data));
      }
      inputDivider = 1 << ((data >> 6) & 3);
      clockSource = srcMap[(data >> 8) & 3];

      updateCyclesMultiplicator();

      if ((data & TCLR) != 0) {
	counter = 0;
	resetCounter(cycles);
	
	updateCaptures(-1, cycles);
      }

      int newMode = (data >> 4) & 3;
      if (mode == STOP && newMode != STOP) {
        // Set the initial counter to the value that counter should have after
        // recalculation
        resetCounter(cycles);
        
        // Wait until full wrap before setting the IRQ flag!
        nextTimerTrigger = (long) (cycles + cyclesMultiplicator * ((0xffff - counter) & 0xffff));
        if (DEBUG) {
          System.out.println(getName() + " Starting timer!");
        }
        recalculateCompares(cycles);
      }
      mode = newMode;
      
      interruptEnable = (data & 0x02) > 0;

      if (DEBUG) {
	System.out.println(getName() + " Write: " +
			   " CTL: inDiv:" + inputDivider +
			   " src: " + getSourceName(clockSource) +
			   " IEn:" + interruptEnable + " IFG: " +
			   interruptPending + " mode: " + mode +
			   ((data & TCLR) != 0 ? " CLR" : ""));
      }

      // Write to the tctl.
      tctl = data;
      // Clear clear bit
      tctl &= ~0x04;

      // Clear interrupt pending if so requested...
      if ((data & 0x01) == 0) {
        interruptPending = false;
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
      boolean oldCapture = captureOn[index];
      captureOn[index] = (data & 0x100) > 0;
      sync[index] = (data & 0x800) > 0;
      inputSel[index] = (data >> 12) & 3;
      int src = inputSrc[index] = srcMap[4 + index * 4 + inputSel[index]];
      capMode[index] = (data >> 14) & 3;

      /* capture a port state? */
      if (!oldCapture && captureOn[index] && (src & SRC_PORT) != 0) {
        int port = (src & 0xff) >> 4;
        int pin = src & 0x0f;
        IOPort ioPort = core.getIOPort(port);
        System.out.println(getName() + " Assigning Port: " + port + " pin: " + pin +
            " for capture");
        ioPort.setTimerCapture(this, pin);
      }
      
      updateCounter(cycles);
      
      triggerInterrupts(cycles);

      if (DEBUG) {
	System.out.println(getName() + " Write: CCTL" +
			   index + ": => " + Utils.hex16(data) +
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
      updateCounter(cycles);
      if (index == 0) {
        // Reset the counter to bring it down to a smaller value...
        // Check if up or updwn and reset if counter too high...
        if (counter > data && (mode == UPDWN || mode == UP)) {
          counter = 0;
        }
        resetCounter(cycles);
      }
      tccr[index] = data;

      int diff = data - counter;
      if (diff < 0) {
        // Ok we need to wrap!
        diff += 0x10000;
      }
      if (DEBUG) {
	System.out.println(getName() +
			   " Write: Setting compare " + index + " to " +
			   Utils.hex16(data) + " TR: " +
			   Utils.hex16(counter) + " diff: " + Utils.hex16(diff));
      }
      // Use the counterPassed information to compensate the expected capture/compare time!!!
      expCaptureTime[index] = cycles + (long)(cyclesMultiplicator * diff + 1) - counterPassed;
      if (DEBUG && counterPassed > 0) {
        System.out.println(getName() + " Comp: " + counterPassed + " cycl: " + cycles + " TR: " +
            counter + " CCR" + index + " = " + data + " diff = " + diff + " cycMul: " + cyclesMultiplicator + " expCyc: " +
            expCaptureTime[index]);
      }
      counterPassed = 0;
      if (DEBUG) {
	System.out.println(getName() + " Cycles: " + cycles + " expCap[" + index + "]: " + expCaptureTime[index] + " ctr:" + counter +
			   " data: " + data + " ~" +
			   (100 * (cyclesMultiplicator * diff * 1L) / 2500000) / 100.0 + " sec" +
			   "at cycles: " + expCaptureTime[index]);
      }
      calculateNextEventTime(cycles);
    }
  }
  void updateCyclesMultiplicator() {
    cyclesMultiplicator = inputDivider;
    if (clockSource == SRC_ACLK) {
      cyclesMultiplicator = (cyclesMultiplicator * core.smclkFrq) /
      core.aclkFrq;
      if (DEBUG) {
        System.out.println(getName() + " setting multiplicator to: " + cyclesMultiplicator);
      }
    }
    clockSpeed = (int) (core.smclkFrq / cyclesMultiplicator);
  }
  
  void resetCounter(long cycles) {
    counterStart = cycles - counterPassed;
    // set counterACC to the last returned value (which is the same
    // as bigCounter except that it is "moduloed" to a smaller value
    counterAcc = counter;
    updateCyclesMultiplicator();
    if (DEBUG) 
      System.out.println(getName() + " Counter reset at " + cycles +  " cycMul: " + cyclesMultiplicator);
  }
  
  private void setCounter(int newCtr, long cycles) {
    counter = newCtr;
    resetCounter(cycles);
  }

  private void updateCaptures(int index, long cycles) {
    int hi = noCompare;
    if (index != -1) {
      hi = index + 1;
    }

    // A hack to handle the SMCLK synchronization
    for (int i = 0, n = hi; i < n; i++) {
      int divisor = 1;
      int frqClk = 1;
      /* used to set next capture independent of counter when another clock is source
       * for the capture register!
       */
      boolean clkSource = false;

      if (clockSource == SRC_SMCLK) {
	frqClk = core.smclkFrq / inputDivider;
      } else if (clockSource == SRC_ACLK) {
	frqClk = core.aclkFrq / inputDivider;
      }
      
      // Handle the captures...
      if (captureOn[i]) {
        if (inputSrc[i] == SRC_ACLK) {
          divisor = core.aclkFrq;
          clkSource = true;
        }

	if (DEBUG) {
	  System.out.println(getName() + " expCapInterval[" + i + "] frq = " +
			     frqClk + " div = " + divisor + " SMCLK_FRQ: " + core.smclkFrq);
	}

	// This is used to calculate expected time before next capture of
	// clock-edge to occur - including what value the compare reg. will get
	expCapInterval[i] = frqClk / divisor;
	// This is not 100% correct - depending on clock mode I guess...
	if (clkSource) {
	  /* assume that this was capture recently */
//	  System.out.println(">>> ACLK! fixing with expCompare!!!");
	  expCompare[i] = (tccr[i] + expCapInterval[i]) & 0xffff;
	} else {
	  expCompare[i] = (counter + expCapInterval[i]) & 0xffff;
	}
	// This could be formulated in something other than cycles...
	// ...??? should be multiplied with clockspeed diff also?
	expCaptureTime[i] = cycles + (long)(expCapInterval[i] * cyclesMultiplicator);
	if (DEBUG) {
	  System.out.println(getName() +
			     " Expected compare " + i +
			     " => " + expCompare[i] + "  Diff: " + expCapInterval[i]);
	  System.out.println(getName() +
			     " Expected cap time: " + expCaptureTime[i] + " cycMult: " + cyclesMultiplicator);
	  System.out.println("Capture: " + captureOn[i]);
	}
      }
    }
    calculateNextEventTime(cycles);
  }

  private int updateCounter(long cycles) {
    if (mode == STOP) return counter;
    
    // Needs to be non-integer since smclk Frq can be lower
    // than aclk
    /* this should be cached and changed whenever clockSource change!!! */
    double divider = 1;
    if (clockSource == SRC_ACLK) {
      // Should later be divided with DCO clock?
      divider = 1.0 * core.smclkFrq / core.aclkFrq;
    }
    divider = divider * inputDivider;
    
    // These calculations assume that we have a big counter that counts from
    // last reset and upwards (without any roundoff errors).
    // tick - represent the counted value since last "reset" of some kind
    // counterAcc - represent the value of the counter at the last reset.
    long cycctr = cycles - counterStart;
    double tick = cycctr / divider;
    counterPassed = (int) (divider * (tick - (long) (tick)));
    long bigCounter = (long) (tick + counterAcc);
    
    switch (mode) {
    case CONTIN:
      counter = (int) (bigCounter & 0xffff);
      break;
    case UP:
      if (tccr[0] == 0) {
	counter = 0;
      } else {
	counter = (int) (bigCounter % tccr[0]);
      }
      break;
    case UPDWN:
      if (tccr[0] == 0) {
	counter = 0;
      } else {
	counter = (int) (bigCounter % (tccr[0] * 2));
	if (counter > tccr[0]) {
	  // Should back down to start again!
	  counter = 2 * tccr[0] - counter;
	}
      }
    }
//    System.out.println("CounterStart: " + counterStart + " C:" + cycles + " bigCounter: " + bigCounter +
//        " counter" + counter);

    if (DEBUG) {
      System.out.println(getName() + ": Updating counter cycctr: " + cycctr +
          " divider: " + divider + " mode:" + mode + " => " + counter);
    }
   return counter;
  }

  public long ioTick(long cycles) {
    System.out.println(getName() + " UNEXPECTED CALL TO IOTICK ****");
    return 100000 + cycles;
  }

  // Only called by the interrupt handler
  private void updateTimers(long cycles) {
    if (mode == STOP) {
      if (DEBUG) {
        System.out.println("No timer running -> no interrupt can be caused -> no scheduling...");
      }
      return;
    }
    
    updateCounter(cycles);
    
    if (cycles >= nextTimerTrigger) {
      interruptPending = true;
      // This should be updated whenever clockspeed changes...
      nextTimerTrigger = (long) (nextTimerTrigger + 0x10000 * cyclesMultiplicator);
    }
    
    // This will not work very good...
    // But the timer does not need to be updated this often...
    // Do we need to update the counter here???
    // System.out.println("Checking capture register [ioTick]: " + cycles);
    for (int i = 0, n = noCompare; i < n; i++) { 
      if (expCaptureTime[i] != -1 && cycles >= expCaptureTime[i]) {
        if (DEBUG) {
          System.out.println(getName() + (captureOn[i] ? " CAPTURE: " : " COMPARE: ") + i +
                             " Cycles: " + cycles + " expCap: " +
                             expCaptureTime[i] +
                             " => ExpCR: " + Utils.hex16(expCompare[i]) +
                             " TR: " + counter + " CCR" + i + ": " + tccr[i] + " pass: " +
                             counterPassed);
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
            (long) (0x10000 * cyclesMultiplicator);
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
    triggerInterrupts(cycles);
    calculateNextEventTime(cycles);
  }
  
  private void recalculateCompares(long cycles) {
    for (int i = 0; i < expCaptureTime.length; i++) {
      if (expCaptureTime[i] != 0) {
        int diff = tccr[i] - counter;
        if (diff < 0) {
          // Wrap...
          diff += 0x10000;
        }
        expCaptureTime[i] = cycles + (long) (diff * cyclesMultiplicator);
      }
    }
  }
  
  private void calculateNextEventTime(long cycles) {
    if (mode == STOP) {
      // If nothing is "running" there is no point scheduling...
      return;
    }
    long time = nextTimerTrigger;
//    int smallest = -1;
    for (int i = 0; i < noCompare; i++) {
      long ct = expCaptureTime[i];
      if (ct > 0 && ct < time) {
        time = ct;
//        smallest = i;
      }
    }
    
    if (time == 0) {
      time = cycles + 1000;
    }
    
    if (timerTrigger.scheduledIn == null) {
//      System.out.println(getName() + " new trigger (nothing sch) ..." + time + " re:" +
//             smallest + " => " + (smallest > 0 ? expCaptureTime[smallest] + " > " + expCompare[smallest]: 
//             nextTimerTrigger) + " C:"+ cycles);
      core.scheduleCycleEvent(timerTrigger, time);
    } else if (timerTrigger.time > time) {
//      System.out.println(getName() + " new trigger (new time)..." + time + " C:"+ cycles);
      core.scheduleCycleEvent(timerTrigger, time);
    }
  }
  
  // Can be called to generate any interrupt...
  // TODO: check if it is ok that this also sets the flags to false or if that must
  // be done by software (e.g. firmware)
  public void triggerInterrupts(long cycles) {
    // First check if any capture register is generating an interrupt...
    boolean trigger = false;
    
    int tIndex = 0;
    for (int i = 0, n = noCompare; i < n; i++) {
      // check for both IFG and IE
      boolean newTrigger = (tcctl[i] & CC_TRIGGER_INT) == CC_TRIGGER_INT;
      trigger = trigger | newTrigger;

      // This only triggers interrupts - reading TIV clears!??!
      if (i == 0) {
        // Execute the interrupt vector... the high-pri one...
        if (DEBUG) {
          System.out.println(getName() +"  >>>> Trigger IRQ for CCR0: " + tccr[0] +
              " TAR: " + counter + " cycles: " + cycles + " expCap: " + expCaptureTime[0]);
        }
        if (DEBUG && counter < tccr[0] && trigger) {
          System.out.print("***** WARNING!!! CTR Err ");
          System.out.println(getName() +"  >>>> Trigger IRQ for CCR0: " + tccr[0] +
              " TAR: " + counter + " cycles: " + cycles + " expCap: " + expCaptureTime[0] +
              " counterPassed: " + counterPassed);
        }
        core.flagInterrupt(ccr0Vector, this, trigger);
        // Trigger this!
        // This is handled by its own vector!!!
        if (trigger) {
          lastTIV = 0;
          triggerTime = cycles;
          return;
        }
      } else {
        // Which would have the highest pri? Will/should this trigger more
        // than one interrupt at a time?!!?!?!
        // If so, which TIV would be the correct one?
        if (newTrigger) {
          if (DEBUG) {
            System.out.println(getName() + " >>>> Triggering IRQ for CCR" + i +
                " at cycles:" + cycles + " CCR" + i + ": " + tccr[i] + " TAR: " +
                counter);
          }
          tIndex = i;
          // Lower numbers are higher priority
          break;
        }
      }
    }
    
    if (trigger) {
      // Or if other CCR execute the normal one with correct TAIV
      lastTIV = memory[tiv] = tIndex * 2;
      triggerTime = cycles;
      if (DEBUG) System.out.println(getName() +
          " >>>> Triggering IRQ for CCR" + tIndex +
          " at cycles:" + cycles + " CCR" + tIndex + ": " + tccr[tIndex] + " TAR: " +
          counter);
      if (DEBUG && counter < tccr[tIndex]) {
        System.out.print("***** WARNING!!!  CTR Err ");
        System.out.println(getName() +
            " >>>> Triggering IRQ for CCR" + tIndex +
            " at cycles:" + cycles + " CCR" + tIndex + ": " + tccr[tIndex] + " TAR: " +
            counter + " expCap: " + expCaptureTime[tIndex] + " counterPassed: " + counterPassed);
      }
    }

    if (!trigger) {
      if (interruptEnable && interruptPending) {
        trigger = true;
        lastTIV = memory[tiv] = timerOverflow;
      }
    }
    
//    System.out.println(getName() +">>>> Trigger IRQ for CCR:" + tIndex);
    core.flagInterrupt(ccr1Vector, this, trigger);
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
	return "Port " + ((source & 0xf0) >> 4) + "." +
	  (source & 0xf);
      }
    }
    return "";
  }

  public String getName() {
    return name;
  }

  /**
   * capture - perform a capture if the timer CCRx is configured for captures
   * Note: capture may only be called when value has changed
   * @param ccrIndex - the capture register
   * @param source - the capture source (0/1)
   */
  public void capture(int ccrIndex, int source, int value) {
    if (ccrIndex < noCompare && captureOn[ccrIndex] && inputSel[ccrIndex] == source) {
      /* This is obviously a capture! */
      boolean rise = (capMode[ccrIndex] & CM_RISING) != 0;
      boolean fall = (capMode[ccrIndex] & CM_FALLING) != 0;
      if ((value == IOPort.PIN_HI && rise) ||
          (value == IOPort.PIN_LOW && fall)) {
//      System.out.println("*** Capture on CCR_" + ccrIndex + " " + " value: " +
//            value);
        // Set the interrupt flag...
      tcctl[ccrIndex] |= CC_IFG;
      triggerInterrupts(core.cycles);
      }
    }
  }
  
  
  // The interrupt has been serviced...
  // Some flags should be cleared (the highest priority flags)?
  public void interruptServiced(int vector) {
    if (vector == ccr0Vector) {
      // Reset the interrupt trigger in "core".
      core.flagInterrupt(ccr0Vector, this, false);
      // Remove the flag also...
      tcctl[0] &= ~CC_IFG;
    }
    if (MSP430Core.debugInterrupts) {
      System.out.println(getName() + " >>>> interrupt Serviced " + lastTIV + 
          " at cycles: " + core.cycles + " servicing delay: " + (core.cycles - triggerTime));
    }
    triggerInterrupts(core.cycles);
  }

  public int getModeMax() {
    return 0;
  }
  
  public String getName(int address) {
    int reg = address - offset;
    if (reg == 0) return "TCTL";
    if (reg < 0x10) return "TCTL" + (reg - 2) / 2;
    if (reg == 0x10) return "TR";
    if (reg < 0x20) return "TCCR" + (reg - 0x12) / 2;
    return " UNDEF(" + Utils.hex16(address) + ")";
  }
  
}
