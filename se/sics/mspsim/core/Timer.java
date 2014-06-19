/**
 * Copyright (c) 2007-2012, Swedish Institute of Computer Science.
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
 * -----------------------------------------------------------------
 *
 * Timer
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 */

package se.sics.mspsim.core;
import se.sics.mspsim.core.EmulationLogger.WarningType;
import se.sics.mspsim.core.MSP430Config.MUXConfig;
import se.sics.mspsim.util.Utils;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

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

  public static final int TBIV = 0x011e;
  public static final int TAIV = 0x012e;

//  public static final int TACCR0_VECTOR = 6;
//  // Other is on 5
//  public static final int TACCR1_VECTOR = 5;
//
//  public static final int TBCCR0_VECTOR = 13;
//  // Other is on 12
//  public static final int TBCCR1_VECTOR = 12;

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

  public static final int TEX0 = 0x20;

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

  public static final String[] modeNames = {
      "STOP", "UP", "CONT", "UPDWN"
  };

  
  private final int tiv;
  private int inputDivider1 = 1;  
  private int inputDivider2 = 1;  
  
  private int endValueIndex = 0;
  private static final int[] EndValue = new int[] {
   16, 12, 10, 8
  };

  // If clocked by anything other than the SubMainClock at full
  // speed this needs to be calculated for correct handling.
  // Should be something like inputDivider * SMCLK_SPEED / CLK_SRC_SPEED;
  private double cyclesMultiplicator = 1;

  private int clockSource;
  private int clockSpeed;
  private int mode;
  
  // The IO registers
  private int tctl;

  private boolean interruptEnable = false;
  private boolean interruptPending = false;

  private final int ccr1Vector;
  private final int ccr0Vector;

  // Support variables Max 7 compare regs for now (timer b)
  private final int noCompare;
  private final CCR ccr[];

  /* this is class represents a capture and compare register */
  private class CCR extends TimeEvent {
      int tcctl;
      int tccr;

      List<MUXConfig> PortCon;
      
      
      int expCompare;
      int expCapInterval;
      long expCaptureTime;

      int capMode;
      boolean captureOn = false;
      int inputSel;
      int inputSrc;
      long cyclesLeft = 0;
      boolean sync;
      int outMode;

      final int interruptVector;
      final int index;

      public CCR(long time, String name, int vector, int index) {
          super(time, name);
          interruptVector = vector;
          this.index = index;
          this.PortCon = new ArrayList<MUXConfig>();
      }
      
      String getName() {
          return "CCR " + index;
      }

      public void execute(long t) {
          if (mode == STOP) {
              //System.out.println("**** IGNORING EXECUTION OF CCR - timer stopped!!!");
              return;
          }
          long cycles = cpu.cycles;
          updateCounter(cycles);
          
          if(!eventReachable()){
            System.out.println("CCR Value bigger than TimerMaxValue!");
            return;            
          }

          if (expCaptureTime != -1 && cycles >= expCaptureTime) {
              
              int diff=calcDiv();

              /* sometimes the event seems to be triggered too early... */
              if (diff>0) {
                  if (DEBUG) log("**** Counter too small: " + counter + " vs " + tccr);
                  expCaptureTime = cycles + (long) (diff * cyclesMultiplicator);
                  update();
                  return;
              }
              
              
              if (DEBUG) {
                  log((captureOn ? "CAPTURE: " : "COMPARE: ") + index +
                          " Cycles: " + cycles + " expCap: " + expCaptureTime +
                          " => ExpCR: " + Utils.hex16(expCompare) +
                          " TR: " + counter + " CCR" + index + ": " + tccr + " pass: " +
                          counterPassed);
              }
              
                ccrEvent(this);
                // Set the interrupt flag...
                tcctl |= CC_IFG;

              if (captureOn) {
                  // Write the expected capture time to the register (counter could
                  // differ slightly)
                  tccr = expCompare;
                  // Update capture times... for next capture
                  expCompare = (expCompare + expCapInterval) & WrapValue();
                  expCaptureTime += expCapInterval * cyclesMultiplicator;
                  if (DEBUG) {
                      log("setting expCaptureTime to next capture: " + expCaptureTime);
                  }
              } else {
                  int steps=calcNextEvent()+diff;
                  // Update expected compare time for this compare/cap register
                  expCaptureTime = expCaptureTime + (long) (steps * cyclesMultiplicator);
                  if (DEBUG) {
                      log("setting expCaptureTime to full wrap: " + expCaptureTime);
                  }
              }
              /* schedule again! */
              update();
              triggerInterrupt(cycles);
          }
      }

      public int calcNextEvent(){
        if(mode == UPDWN){
          int CycleVal;
          long bcount=bigCount(cpu.cycles)%(2*ccr[0].tccr);
          boolean DIR_UP = (bcount <= ccr[0].tccr);           
          if(DIR_UP){
            CycleVal=2*(ccr[0].tccr-tccr);
          }else{
            CycleVal=2*(tccr);
          }
          return CycleVal;
        } else{
          return calcPeriodeTime();
        }
      }
      
      public boolean eventReachable(){
        if(mode == UPDWN){
          if(WrapValue()<ccr[0].tccr) return false;
        } else {
            if (mode == UP){
              if(WrapValue()<ccr[0].tccr) return false;
            } else { 
              if(WrapValue()<tccr) return false;        //Counter cannot count to tccr
            }
        } 
        return true;
      }      
      
      public int calcDiv(){
        int diff;

        if(mode == UPDWN){
          int bcount=(int)(bigCount(cpu.cycles)%(2*ccr[0].tccr));
          boolean DIR_UP = (bcount <= ccr[0].tccr);

          if(DIR_UP){
            diff=tccr-bcount;
          } else{
            diff=2*ccr[0].tccr-tccr-bcount;
          }
        } else {
          if(counter>=tccr) diff=tccr-counter;
          else{
            diff=tccr-counter;
            int wrap_distance;
            if (mode == UP){
              wrap_distance=ccr[0].tccr-tccr+counter;
            } else { 
              wrap_distance=WrapValue()+1-tccr+counter;
            }
            if(diff>wrap_distance) diff=-wrap_distance;
          }
        } 
        return diff;
      }      
      
      /* this method only takes care of the interrupt triggering! */
      public void triggerInterrupt(long cycles) {
          /* trigger if trigger should be... */
          if ((tcctl & CC_TRIGGER_INT) == CC_TRIGGER_INT) {
              if (index == 0) {
                  if (DEBUG) log("triggering interrupt");
                  cpu.flagInterrupt(interruptVector, Timer.this, true);
              } else if (lastTIV == 0) {
                  lastTIV = index * 2;
                  if (DEBUG) log("triggering interrupt TIV: " + lastTIV);
                  cpu.flagInterrupt(interruptVector, Timer.this, true);
              } else if (lastTIV > index * 2) {
                  /* interrupt already triggered, but set to this lower IRQ */
                  lastTIV = index * 2;
              }
          }
      }

      public void updateCaptures(long cycles) {
          int divisor = 1;
          int frqClk = 1;
          /* used to set next capture independent of counter when another clock is source
           * for the capture register!
           */
          boolean clkSource = false;

          if (clockSource == SRC_SMCLK) {
              frqClk = cpu.smclkFrq / (inputDivider1+inputDivider2);
          } else if (clockSource == SRC_ACLK) {
              frqClk = cpu.aclkFrq / (inputDivider1+inputDivider2);
          }

          // Handle the captures...
          if (captureOn) {
              if (inputSrc == SRC_ACLK) {
                  divisor = cpu.aclkFrq;
                  clkSource = true;
              }

              if (DEBUG) {
                  log("expCapInterval[" + index + "] frq = " +
                          frqClk + " div = " + divisor + " SMCLK_FRQ: " + cpu.smclkFrq);
              }

              // This is used to calculate expected time before next capture of
              // clock-edge to occur - including what value the compare reg. will get
              expCapInterval = frqClk / divisor;
              // This is not 100% correct - depending on clock mode I guess...
              if (clkSource) {
                  /* assume that this was capture recently */
                  //            System.out.println(">>> ACLK! fixing with expCompare!!!");
                  expCompare = (tccr + expCapInterval) & WrapValue();
              } else {
                  expCompare = (counter + expCapInterval) & WrapValue();
              }
              // This could be formulated in something other than cycles...
              // ...??? should be multiplied with clockspeed diff also?
              expCaptureTime = cycles + (long)(expCapInterval * cyclesMultiplicator);
              if (DEBUG) {
                  log("Expected compare " + index +
                          " => " + expCompare + "  Diff: " + expCapInterval);
                  log("Expected cap time: " + expCaptureTime + " cycMult: " + cyclesMultiplicator);
                  log("Capture: " + captureOn);
              }
              update();
          }
      }

      public void update() {
          /* schedule this capture register for update*/
          if (expCaptureTime != -1 && expCaptureTime != time) {
              if (DEBUG) log(cpu.cycles + ":" + ">> SCHEDULING " + getName() + " = " + tccr +
                      " TR: " + counter + " at: " + expCaptureTime);
              cpu.scheduleCycleEvent(this, expCaptureTime);
          }
      }
      
      public void timerStarted(long cycles) {
          if (cyclesLeft != 0) {
              expCaptureTime = cycles + cyclesLeft;
              update();
          }
      }
      
      public void timerStopped(long cycles) {
          if (expCaptureTime != -1) {
              cyclesLeft = cycles - expCaptureTime;
              if (cyclesLeft < 0) {
                  cyclesLeft = 0;
              }
          }
      }

      public String info() {
          String Listen = "";
          for (MUXConfig con : PortCon) {
            Listen += con.Port + "." + con.Pin + " ";
          }
          return "CCR" + index + ":" +
          "  CM: " + capNames[capMode] +
          "  CCIS:" + inputSel + "  Source: " +
          getSourceName(inputSrc) +
          "  Capture: " + captureOn +
          "  Listen: " + Listen +
          " IFG: " + ((tcctl & CC_IFG) > 0) + " IE: " + ((tcctl & CC_IE) > 0);
      }
      
      public DefaultMutableTreeNode getNode() {
        String Listen = "";
        for (MUXConfig con : PortCon) {
          Listen += con.Port + "." + con.Pin + " ";
        }
        DefaultMutableTreeNode CCRx = new DefaultMutableTreeNode("CCR" + index + ":");
        
        CCRx.add(new DefaultMutableTreeNode("CM: " + capNames[capMode]));
        CCRx.add(new DefaultMutableTreeNode("CCIS:" + inputSel));
        CCRx.add(new DefaultMutableTreeNode("Source: " + getSourceName(inputSrc)));
        CCRx.add(new DefaultMutableTreeNode("Capture: " + captureOn));
        CCRx.add(new DefaultMutableTreeNode("Listen: " + Listen));
        CCRx.add(new DefaultMutableTreeNode("IFG: " + ((tcctl & CC_IFG))));
        CCRx.add(new DefaultMutableTreeNode("IE: " + ((tcctl & CC_IE) > 0)));
        return CCRx; 
    }      

      public void togglePortSel() {
        for (MUXConfig con : PortCon) {
          IOPort out = cpu.getIOUnit(IOPort.class, "P" + con.Port);
          boolean Value = out.readPortSel(con.Sel,con.Pin);
          out.writePortSel(con.Sel,con.Pin, !Value);
        }
      }

      public void writePortSel(boolean Value) {
        for (MUXConfig con : PortCon) {
          IOPort out = cpu.getIOUnit(IOPort.class, "P" + con.Port);
          out.writePortSel(con.Sel,con.Pin, Value);
        }
      }      
  }

  private TimeEvent counterTrigger = new TimeEvent(0, "Timer Counter Trigger") {
      public void execute(long t) {
          if(mode==STOP){
            return;
          }        
          interruptPending = true;
          // This should be updated whenever clockspeed changes...
          nextTimerTrigger = (long) (nextTimerTrigger + calcPeriodeTime() * cyclesMultiplicator);
//          System.out.println("*** scheduling counter trigger..." + nextTimerTrigger + " now = " + t);
          cpu.scheduleCycleEvent(this, nextTimerTrigger);
          
          
          if (lastTIV == 0 && interruptEnable) {
              lastTIV = memory[tiv] = timerOverflow;
              cpu.flagInterrupt(ccr1Vector, Timer.this, true);
          } else {
//              System.out.println("*** Did not trigger interrupt: " + interruptEnable);
          }
      }
  };
  
  private int lastTIV;

  private final int[] srcMap;

  private long triggerTime;
  
  private MSP430Config.TimerType typ;
  
  /**
   * Creates a new <code>Timer</code> instance.
   *
   */

  public Timer(MSP430Core cpu, int[] memory, MSP430Config.TimerConfig config) {
    super(config.name, config.name, cpu, memory, config.offset);
    this.srcMap = config.srcMap;
    // noCompare = (srcMap.length / 4) - 1;
    noCompare = config.ccrCount;
    if (srcMap == TIMER_Ax149) {
      timerOverflow = 0x0a;
    } else {
      timerOverflow = 0x0e;
    }
    tiv = config.timerIVAddr;
    ccr0Vector = config.ccr0Vector;
    ccr1Vector = config.ccrXVector;

    counterTrigger.name += ' ' + config.name;

    ccr = new CCR[noCompare];
    for (int i = 0; i < noCompare; i++) {
        ccr[i] = new CCR(0, "CCR" + i + " " + config.name, i == 0 ? ccr0Vector : ccr1Vector, i);
    }

    if (config.muxConfig != null)
      for (int i = 0; i < config.muxConfig.length; i++) {
        int j = config.muxConfig[i].CCUnitIndex;
        ccr[j].PortCon.add(config.muxConfig[i]);
        // System.out.println(ccr[j].info());
    }
    
    this.typ=config.typ;
    
    reset(0);
  }
  
  public int calcPeriodeTime(){
    int CycleVal;
    if (mode == UP){
      CycleVal=ccr[0].tccr+1;
    } else if (mode == CONTIN){
      CycleVal=WrapValue()+1;           
    } else {
      CycleVal=2*(ccr[0].tccr)+1; 
    }
    return CycleVal;
  }
  
  public void reset(int type) {

      /* reset the capture and compare registers */
      for (int i = 0, n = noCompare; i < n; i++) {
          CCR reg = ccr[i];
          reg.expCompare = -1;
          reg.expCaptureTime = -1;
          reg.expCapInterval = 0;
          reg.outMode = 0;
          reg.capMode = 0;
          reg.inputSel = 0;
          reg.inputSrc = 0;
          reg.captureOn = false;
          reg.tcctl = 0;
          reg.tccr = 0;
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
      inputDivider1 = 1;
      inputDivider2 = 1;
      endValueIndex = 0;
  }
  
  public int WrapValue(){
    return ((1<<EndValue[endValueIndex])-1);       
  }

  // Should handle read of byte also (currently ignores that...)
  public int read(int address, boolean word, long cycles) {

//      if (DEBUG) log("read from: $" + Utils.hex(address, 4));

      if (address == tiv) {
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
        log("Read: " +
            " CTL: inDiv:" + inputDivider1 + " " + inputDivider2 +
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
      val = ccr[i].tcctl;
      break;
    case TCCR0:
    case TCCR1:
    case TCCR2:
    case TCCR3:
    case TCCR4:
    case TCCR5:
    case TCCR6:
      i = (index - TCCR0) / 2;
      if (i >= noCompare) {
          throw new EmulationException(getName() + " Reading from CCR register that is not available " + i);
      }
      val = ccr[i].tccr;
      break;
    case TEX0:
      val=inputDivider2-1;
      break;
    default:
      logw(WarningType.VOID_IO_READ, "Not supported read, returning zero!!! addr: " + index + " addr: $" + Utils.hex(address, 4));
    }
    
    if (DEBUG) {
      log("Read " + getName(address) + "($" + Utils.hex(address, 4) + ") => $" +
          Utils.hex16(val) + " (" + val + ")");
    }

    // It reads the interrupt flag for capture...
    return val & 0xffff;
  }

  /* here we need to update things such as CCI / Capture/Compare Input value
   * and other dynamic values
   */
  private void updateTCCTL(int cctl, long cycles) {
      if (cctl >= noCompare) {
          throw new EmulationException(getName() + " Trying to write to non-existent CCTL register: " + cctl);
      }
    // update the CCI depending on speed of clocks...
    boolean input = false;
    /* if ACLK we can calculate edge... */
    if (ccr[cctl].inputSrc == SRC_ACLK) {
      /* needs the TimerA clock speed here... */
      int aTicks = clockSpeed / cpu.aclkFrq;
      updateCounter(cycles);
      
      /* only calculate this if clock runs faster then ACLK - otherwise it
       * this will be dividing by zero... 
       */
      if (aTicks > 0 && counter % aTicks > aTicks / 2) {
        input = true;
      }
    }
    ccr[cctl].tcctl = (ccr[cctl].tcctl & ~CC_I) | (input ? CC_I : 0);    
  }

  private void resetTIV(long cycles) {
    if (lastTIV == timerOverflow) {
      interruptPending = false;
      if (DEBUG) {
        log("Clearing TIV - overflow ");
      }
    } else if (lastTIV / 2 < noCompare) {
      if (DEBUG) {
	log(cpu.cycles + ": Clearing IFG for CCR" + (lastTIV/2));
      }
      // Clear interrupt flags!
      ccr[lastTIV / 2].tcctl &= ~CC_IFG;
    }

    /* flag this interrupt off */
    cpu.flagInterrupt(ccr1Vector, this, false);
    lastTIV = 0;

    /* reevaluate interrupts for the ccr1 vector - possibly flag on again... */
    for (int i = 1; i < noCompare; i++) {
        ccr[i].triggerInterrupt(cycles);
    }
    /* if the timer overflow interrupt is triggering - lowest priority => signal! */
    if (lastTIV == 0 && interruptEnable & interruptPending) {
        lastTIV = timerOverflow;
        cpu.flagInterrupt(ccr1Vector, this, true);
    }
  }

  public void write(int address, int data, boolean word, long cycles) {
    // This does not handle word/byte difference yet... assumes it gets
    // all 16 bits when called!!!

    if (address == tiv) {
      // should clear registers for cause of interrupt (highest value)?
      // but what if a higher value have been triggered since this was
      // triggered??? -> does that matter???
      // But this mess the TIV up too early......
      // Must DELAY the reset of interrupt flags until next read...?
      resetTIV(cycles);
    }

    int iAddress = address - offset;

    if (DEBUG) log("write to: $" + Utils.hex(address, 4) +
            " => " + iAddress + " = " + data);


    switch (iAddress) {
    case TR:
      setCounter(data, cycles);
      break;
    case TCTL:
      if (DEBUG) {
        log("wrote to TCTL: " + Utils.hex16(data));
      }
      inputDivider1 = 1 << ((data >> 6) & 3);
      clockSource = srcMap[(data >> 8) & 3];
      
      if(typ==MSP430Config.TimerType.TimerEndEdit)
        endValueIndex = ((data >> 11) & 3);
      
      setCounter(counter&WrapValue(),cycles);
      
      if ((data & TCLR) != 0) {
	setCounter(0,cycles);
	
	for (int i = 0; i < noCompare; i++) {
            ccr[i].updateCaptures(cycles);
        }
      }

      int oldMode = mode;
      mode = (data >> 4) & 3;
      
      if (oldMode == STOP && mode != STOP) {
        // Set the initial counter to the value that counter should have after
        // recalculation
        resetCounter(cycles);
        
        // Wait until full wrap before setting the IRQ flag!
        nextTimerTrigger = (long) (cycles + cyclesMultiplicator * ((calcPeriodeTime() - counter) & WrapValue()));
        if (DEBUG) {
          log("Starting timer!");
        }

        for (int i = 0; i < noCompare; i++) {
            ccr[i].timerStarted(cycles);
        }
        
        if (DEBUG) log(cpu.cycles + ": Timer started: " + counter + "  CCR1:" + ccr[1].expCaptureTime);
        
      }
      if (oldMode != STOP && mode == STOP) {
          /* call update counter to remember how many cycles that passed before this stop... */
          updateCounter(cycles);
          for (int i = 0; i < noCompare; i++) {
              ccr[i].timerStopped(cycles);
          }
          if (DEBUG) log(cpu.cycles + ": Timer stopped: " + counter + "  CCR1:" + ccr[1].expCaptureTime);
      }
      
      interruptEnable = (data & 0x02) > 0;

      if (DEBUG) {
	log("Write:  CTL: inDiv:" + inputDivider1 + " " + inputDivider2 +
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
      
      if (!interruptEnable){
        resetTIV(cycles);
      }
      
      //    updateCaptures(-1, cycles);
      for (int i = 0; i < noCompare; i++) {
          ccr[i].updateCaptures(cycles);
      }
      
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
      CCR reg = ccr[index];
      reg.tcctl = data;
      reg.outMode = (data >> 5)& 7;
      boolean oldCapture = reg.captureOn;
      reg.captureOn = (data & 0x100) > 0;
      reg.sync = (data & 0x800) > 0;
      reg.inputSel = (data >> 12) & 3;
      int src = reg.inputSrc = srcMap[4 + index * 4 + reg.inputSel];
      reg.capMode = (data >> 14) & 3;

      /* capture a port state? */
      if (!oldCapture && reg.captureOn && (src & SRC_PORT) != 0) {
        int port = (src & 0xff) >> 4;
        int pin = src & 0x0f;
        IOPort ioPort = cpu.getIOUnit(IOPort.class, "P" + port);
        if (DEBUG) log("Assigning Port: " + port + " pin: " + pin +
            " for capture");
        ioPort.setTimerCapture(this, pin);
      }
      
      updateCounter(cycles);
      
      if (DEBUG) {
	log(getName() + "Write: CCTL" + index + ": => " + Utils.hex16(data) +
	        " CM: " + capNames[reg.capMode] +
	        " CCIS:" + reg.inputSel + " name: " +
	        getSourceName(reg.inputSrc) +
	        " Capture: " + reg.captureOn +
	        " IE: " + ((data & CC_IE) != 0));
      }

      reg.updateCaptures(cycles);
      startIt(index,cycles);
//      updateCaptures(index, cycles);
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
          setCounter(0,cycles);
        }
      }
      if (ccr[index] == null)
          logw(WarningType.VOID_IO_WRITE, "Timer write to " + Utils.hex16(address));
      ccr[index].tccr = data;

      startIt(index,cycles);
      //calculateNextEventTime(cycles);
      break;
    case TEX0:
      inputDivider2 = (data&7)+1;      
      updateCyclesMultiplicator();      
      break;
    }
  }

  void startIt(int index,long cycles){
    int tccr=ccr[index].tccr;
    int diff = ccr[index].calcDiv();
    if (diff <= 0) {
      diff=ccr[index].calcNextEvent()+diff;
    }
    if (DEBUG) {
      log("Write: Setting compare " + index + " to "
          + Utils.hex16(tccr) + " TR: " + Utils.hex16(counter)
          + " diff: " + Utils.hex16(diff));
    }
    // Use the counterPassed information to compensate the expected
    // capture/compare time!!!
    ccr[index].expCaptureTime = cycles
        + (long) (cyclesMultiplicator * diff + 1) - counterPassed;
    if (DEBUG && counterPassed > 0) {
      log("Comp: " + counterPassed + " cycl: " + cycles + " TR: "
          + counter + " CCR" + index + " = " + tccr + " diff = "
          + diff + " cycMul: " + cyclesMultiplicator
          + " expCyc: " + ccr[index].expCaptureTime);
    }
    counterPassed = 0;
    if (DEBUG) {
      log("Cycles: " + cycles + " expCap[" + index + "]: "
          + ccr[index].expCaptureTime + " ctr:" + counter
          + " data: " + tccr + " ~"
          + (100 * (cyclesMultiplicator * diff * 1L) / 2500000)
          / 100.0 + " sec" + "at cycles: "
          + ccr[index].expCaptureTime);
    }
    ccr[index].update();
  }  
  
  void updateCyclesMultiplicator() {
    cyclesMultiplicator = inputDivider1*inputDivider2;
    if (clockSource == SRC_ACLK) {
      cyclesMultiplicator = (cyclesMultiplicator * cpu.smclkFrq) /
      cpu.aclkFrq;
      if (DEBUG) {
        log("setting multiplicator to: " + cyclesMultiplicator);
      }
    }
    clockSpeed = (int) (cpu.smclkFrq / cyclesMultiplicator);
  }
  
  void resetCounter(long cycles) {
      double divider = 1.0;
      if (clockSource == SRC_ACLK) {
          // Should later be divided with DCO clock?
          divider = 1.0 * cpu.smclkFrq / cpu.aclkFrq;
      }
      divider = divider * inputDivider1 * inputDivider2;
        
      // These calculations assume that we have a big counter that counts from
      // last reset and upwards (without any roundoff errors).
      // tick - represent the counted value since last "reset" of some kind
      // counterAcc - represent the value of the counter at the last reset.
      long cycctr = cycles - counterStart;
      double tick = cycctr / divider;
      counterPassed = (int) (divider * (tick - (long) (tick)));
      
    counterStart = cycles - counterPassed;
    // set counterACC to the last returned value (which is the same
    // as bigCounter except that it is "moduloed" to a smaller value
    counterAcc = counter;
    updateCyclesMultiplicator();
    if (DEBUG) { 
      log("Counter reset at " + cycles +  " cycMul: " + cyclesMultiplicator);
    }

    cpu.scheduleCycleEvent(counterTrigger, cycles + (long)((WrapValue() + 1 - counter) * cyclesMultiplicator));
//    System.out.println("(re)Scheduling counter trigger..." + counterTrigger.time + " now = " + cycles + " ctr: " + counter);

  }

  private void setCounter2(int newCtr, long cycles) {
    counter = newCtr;
  }
  
  private void setCounter(int newCtr, long cycles) {
    counter = newCtr;
    resetCounter(cycles);
  }
  
  private long bigCount(long cycles){
    // Needs to be non-integer since smclk Frq can be lower
    // than aclk
    /* this should be cached and changed whenever clockSource change!!! */
    double divider = 1;
    if (clockSource == SRC_ACLK) {
      // Should later be divided with DCO clock?
      divider = 1.0 * cpu.smclkFrq / cpu.aclkFrq;
    }
    divider = divider * inputDivider1 * inputDivider2;

    // These calculations assume that we have a big counter that counts from
    // last reset and upwards (without any roundoff errors).
    // tick - represent the counted value since last "reset" of some kind
    // counterAcc - represent the value of the counter at the last reset.
    long cycctr = cycles - counterStart;
    double tick = cycctr / divider;
    counterPassed = (int) (divider * (tick - (long) (tick)));

    if (DEBUG) {
      log("Updating counter cycctr: " + cycctr + " divider: " + divider
          + " mode:" + mode + " => " + counter);
    } 
    return (long) (tick + counterAcc);    
  }

  private int updateCounter(long cycles) {
    if (mode == STOP) return counter;
    
    long bigCounter=bigCount(cycles);

    
    switch (mode) {
    case CONTIN:
      setCounter2((int) (bigCounter & WrapValue()),cycles);
      break;
    case UP:
      if (ccr[0].tccr == 0) {
        setCounter2(0,cycles);
      } else {
        setCounter2((int) (bigCounter % ccr[0].tccr),cycles);
      }
      break;
    case UPDWN:
      if (ccr[0].tccr == 0) {
        setCounter2(0,cycles);
      } else {
        setCounter2((int) (bigCounter % (ccr[0].tccr * 2)),cycles);
	if (counter > ccr[0].tccr) {
	  // Should back down to start again!
	  setCounter2(2 * ccr[0].tccr - counter,cycles);
	}
      }
    }

   return counter;
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

  /**
   * capture - perform a capture if the timer CCRx is configured for captures
   * Note: capture may only be called when value has changed
   * @param ccrIndex - the capture register
   * @param source - the capture source (0/1)
   */
  public void capture(int ccrIndex, int source, IOPort.PinState value) {
      CCR reg = ccr[ccrIndex];
      if (ccrIndex < noCompare && reg.captureOn && reg.inputSel == source) {
          /* This is obviously a capture! */
          boolean rise = (reg.capMode & CM_RISING) != 0;
          boolean fall = (reg.capMode & CM_FALLING) != 0;
          if ((value == IOPort.PinState.HI && rise) ||
                  (value == IOPort.PinState.LOW && fall)) {
              //      System.out.println("*** Capture on CCR_" + ccrIndex + " " + " value: " +
              //            value);
              // update counter values and compare register
              updateCounter(cpu.cycles);
              reg.tccr = counter;
              
              // Set the interrupt flag...
              reg.tcctl |= CC_IFG;
              reg.triggerInterrupt(cpu.cycles);
              /* triggerInterrupts(cpu.cycles); */
          }
    }
  }
  
  // The interrupt has been serviced...
  // Some flags should be cleared (the highest priority flags)?
  public void interruptServiced(int vector) {
    if (vector == ccr0Vector) {
      // Reset the interrupt trigger in "core".
      cpu.flagInterrupt(ccr0Vector, this, false);
      // Remove the flag also - but only for the dedicated vector (CCR0)
      ccr[0].tcctl &= ~CC_IFG;
    }
    if (MSP430Core.debugInterrupts) {
      System.out.println(getName() + " >>>> interrupt Serviced " + lastTIV + 
          " at cycles: " + cpu.cycles + " servicing delay: " + (cpu.cycles - triggerTime));
    }
    /* old method is replaced */
    /* triggerInterrupts(cpu.cycles); */
  }

  public int getModeMax() {
    return 0;
  }

  private String getName(int address) {
    int reg = address - offset;
    if (reg == 0) return "TCTL";
    if (reg < 0x10) return "TCTL" + (reg - 2) / 2;
    if (reg == 0x10) return "TR";
    if (reg < 0x20) return "TCCR" + (reg - 0x12) / 2;
    if (reg == 0x20) return "TEXO";
    return " UNDEF(" + Utils.hex(address, 4) + ")";
  }

  @Override
  public String info() {
      StringBuilder sb = new StringBuilder();
      sb.append("  Source: " + getSourceName(clockSource) + "  Speed: " + clockSpeed
              + " Hz  inDiv: " + inputDivider1 + " " + inputDivider2 + "  Multiplier: " + cyclesMultiplicator + '\n'
              + "  Mode: " + modeNames[mode] + "  IEn: " + interruptEnable
              + "  IFG: " + interruptPending + "  TR: " + updateCounter(cpu.cycles) + '\n');
      for (CCR reg : ccr) {
          sb.append("  ").append(reg.info()).append('\n');
      }
      return sb.toString();
  }
  
  public DefaultMutableTreeNode getNode(){
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(getName());
    node.add(new DefaultMutableTreeNode ("Source: " + getSourceName(clockSource)));                                                                                      //
    node.add(new DefaultMutableTreeNode ("Speed: " + clockSpeed + " Hz"));                                                                                          //
    node.add(new DefaultMutableTreeNode ("inDiv1: " + inputDivider1));                                                                                          //
    node.add(new DefaultMutableTreeNode ("inDiv2: " + inputDivider2));                                                                                          //
    node.add(new DefaultMutableTreeNode ("Multiplier: " + cyclesMultiplicator));                                                                                     //
    node.add(new DefaultMutableTreeNode ("Mode: " + modeNames[mode]));                                                                                           //
    node.add(new DefaultMutableTreeNode ("IEn: " + interruptEnable));                                                                                            //
    node.add(new DefaultMutableTreeNode ("IFG: " + interruptPending));                                                                                            //
    node.add(new DefaultMutableTreeNode ("TR: " + updateCounter(cpu.cycles)));                                                                                             //
    
    for (CCR reg : ccr) {
      node.add(reg.getNode());
    }
    return node;
  }   

  public void ccrEvent(CCR ccrX) {

    if (ccrX.index == 0) {
      for (int i = 0; i < noCompare; i++) {
        if (ccr[i].PortCon.size() > 0) {
          if ((mode == UP) | (mode == CONTIN)) {
            switch (ccr[i].outMode) {
            case 2:
            case 3:
              ccr[i].writePortSel(false);
              break;
            case 6:
            case 7:
              ccr[i].writePortSel(true);
              break;
            case 4:
              if(i==0) ccr[i].togglePortSel();
            }
          } else if (mode == UPDWN) {
            switch (ccr[i].outMode) {
            case 3:
              ccr[i].writePortSel(false);
              break;
            case 7:
              ccr[i].writePortSel(true);
              break;
            }
          }
        }
      }
    } else {
      if ((mode == UP) | (mode == CONTIN)) {
        switch (ccrX.outMode) {
        case 1:
        case 2:
        case 3:
          ccrX.writePortSel(true);
          break;
        case 4:
          ccrX.togglePortSel();
          break;
        case 5:
        case 6:
        case 7:
          ccrX.writePortSel(false);
          break;
        }
      } else if (mode == UPDWN) {
        long bcount=bigCount(cpu.cycles)%(2*ccr[0].tccr);
        boolean DIR_UP = (bcount < ccr[0].tccr);
        if (DIR_UP) {
          switch (ccrX.outMode) {
          case 1:
          case 4:
          case 6:
            ccrX.writePortSel(true);
            break;
          case 2:
          case 5:
            ccrX.writePortSel(false);
            break;
          }
        } else {
          switch (ccrX.outMode) {
          case 2:
          case 3:
            ccrX.writePortSel(true);
            break;
          case 4:
          case 6:
          case 7:
            ccrX.writePortSel(false);
            break;
          }
        }
      }
    }
  }
}
