/*
 * Copyright (c) 2009, Friedrich-Alexander University Erlangen, Germany
 * 
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
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
 * This file is part of mspsim.
 *
 */
/**
 * @author Klaus Stengel <siklsten@informatik.stud.uni-erlangen.de>
 */
package se.sics.mspsim.core;

import se.sics.mspsim.util.Utils;

public class Flash extends IOUnit {
  private static final boolean DEBUG = true;
  
  private static final int FCTL1 = 0x0128;
  private static final int FCTL2 = 0x012a;
  private static final int FCTL3 = 0x012c;

  private static final int FRKEY =   0x9600;
  private static final int FWKEY =   0xA500;
  private static final int KEYMASK = 0xff00;
  private static final int CMDMASK = 0x00ff;

  private static final int BLKWRT = 0x80;
  private static final int WRT =    0x40;
  
  private static final int ERASE_SHIFT = 1;
  private static final int ERASE_MASK = 0x06;
  
  private enum EraseMode {
    ERASE_NONE,
    ERASE_SEGMENT,
    ERASE_MAIN,
    ERASE_ALL
  };
  
  private enum WriteMode {
    WRITE_NONE,
    WRITE_SINGLE,
    WRITE_BLOCK,
    WRITE_BLOCK_FINISH
  }
  
  private static final int EMEX =    0x20;
  private static final int LOCK =    0x10;
  private static final int WAIT =    0x08;
  private static final int ACCVIFG = 0x04;
  private static final int KEYV =    0x02;
  private static final int BUSY =    0x01;

  private static final int FSSEL_SHIFT = 6;
  private static final int FSSEL_MASK = 0xc0;

  private static final int RESET_VECTOR = 15;
  private static final int NMI_VECTOR = 14;
  private static final int ACCVIE = 1 << 5;
  
  private enum ClockSource {
    ACLK,
    MCLK,
    SMCLK
  };
  
  private static final int MASS_ERASE_TIME = 5297;
  private static final int SEGMENT_ERASE_TIME = 4819;
  
  private static final int WRITE_TIME = 35;

  private static final int BLOCKWRITE_FIRST_TIME = 30;
  private static final int BLOCKWRITE_TIME = 21;
  private static final int BLOCKWRITE_END_TIME = 6;

  private static final int FN_MASK = 0x3f;

  private MSP430Core cpu;
  private SFR sfr;
  private FlashRange main_range;
  private FlashRange info_range;
  private int[] memory;
  
  private int mode;      /* FCTL1 */
  private int clockcfg;  /* FCTL2 */
  private int statusreg; /* FCTL3 */

  private boolean locked;
  private boolean busy;
  private boolean wait;
  private boolean blocked_cpu;
  private EraseMode current_erase;
  
  private WriteMode current_write_mode;
  private int blockwrite_count;
  
  private TimeEvent end_process = new TimeEvent(0) {
    public void execute(long t) {
      blocked_cpu = false;
      
      switch(current_erase) {
      case ERASE_NONE:
        break;
        
      case ERASE_SEGMENT:
      case ERASE_MAIN:
      case ERASE_ALL:
	mode = 0;
	current_erase = EraseMode.ERASE_NONE;
	busy = false;
	break;
      }
      
      switch(current_write_mode) {
      case WRITE_NONE:
	break;
	
      case WRITE_SINGLE:
	mode = 0;
	busy = false;
	current_write_mode = WriteMode.WRITE_NONE;
	break;
	
      case WRITE_BLOCK:
	blockwrite_count++;
	if (blockwrite_count == 64) {
	  // FIXME: What happens if we try to write more than 64 bytes
	  // on real hardware???
	  System.out.printf("Last access in block mode. Forced exit?");
	  current_write_mode = WriteMode.WRITE_BLOCK_FINISH;
	}
/*	if (DEBUG) {
	  System.out.println("Write cycle complete, flagged WAIT.");
	} */
	wait = true;
	break;
	
      case WRITE_BLOCK_FINISH:
	if (DEBUG) {
	  System.out.println("Programming voltage dropped, " +
	  		"write mode disabled.");
	}
	current_write_mode = WriteMode.WRITE_NONE;
	busy = false;
	wait = true;
	mode = 0;
	break;
      }
    }
  };
  
  public Flash(MSP430Core cpu, int[] memory, FlashRange main_range,
      FlashRange info_range) {
    super(memory, FCTL1);
    this.cpu = cpu;
    this.memory = memory;
    this.main_range = main_range;
    this.info_range = info_range;
    this.sfr = cpu.getSFR();
    locked = true;
    
    reset(MSP430.RESET_POR);
  }

  public boolean blocksCPU() {
    return blocked_cpu;
  }
  
  public String getName() {
    return "Flash";
  }
  
  public void interruptServiced(int vector) {
    cpu.flagInterrupt(vector, this, false);
  }
  
  public boolean addressInFlash(int address) {
    if (main_range.isInRange(address)) {
      return true;
    }
    if (info_range.isInRange(address)) {
      return true;
    }
    
    return false;
  }
  
  private int getFlashClockDiv() {
    return (clockcfg & FN_MASK) + 1;
  }
  
  private void waitFlashProcess(int time) {
    int instr_addr = cpu.readRegister(MSP430.PC);
    int freqdiv = getFlashClockDiv();
    int myfreq;
    double finish_msec;
    
    busy = true;
    if (addressInFlash(instr_addr)) {
      blocked_cpu = true;
    }
    
    switch(getClockSource()) {
    case ACLK:
      myfreq = cpu.aclkFrq / freqdiv;
      finish_msec = ((double)time * freqdiv * 1000) / cpu.aclkFrq;
      if (DEBUG)
	System.out.println("Flash: Using ACLK source with f=" + myfreq 
	    + " Hz\nFlasg: Time required=" + finish_msec + " ms");
      cpu.scheduleTimeEventMillis(end_process, finish_msec);
      break;
      
    case SMCLK:
      myfreq = cpu.smclkFrq / freqdiv;
      finish_msec = ((double)time * freqdiv * 1000) / cpu.smclkFrq;
      /* if (DEBUG)
	System.out.println("Flash: Using SMCLK source with f=" + myfreq 
	    + " Hz\nFlash: Time required=" + finish_msec + " ms"); */
      cpu.scheduleTimeEventMillis(end_process, finish_msec);
      break;

      
    case MCLK:
      if (DEBUG)
	System.out.println("Flash: Using MCLK source with div=" + freqdiv);
      cpu.scheduleCycleEvent(end_process, (long)time * freqdiv);
      break;
    }
  }
  
  public boolean needsTick() {
    return false;
  }
  
  public void flashWrite(int address, int data, boolean word) {
    int wait_time = -1;
    
    if (locked) {
      if (DEBUG) {
	System.out.println("Write to flash blocked because of LOCK flag.");
      }
      return;
    }
    
    if (busy || wait == false) {
      if (!((mode & BLKWRT) != 0 && wait)) {
	triggerAccessViolation("Flash write prohbited while BUSY=1 or WAIT=0");
	return;
      }
    }
    
    switch(current_erase) {
    case ERASE_SEGMENT:
      int a_area_start[] = new int[1];
      int a_area_end[] = new int[1];
      getSegmentRange(address, a_area_start, a_area_end);
      int area_start = a_area_start[0];
      int area_end = a_area_end[0];
      
      if (DEBUG) {
	System.out.println("Segment erase @" + Utils.hex16(address) + 
	    ": erasing area " + Utils.hex16(area_start) + "-" +
	    Utils.hex16(area_end));
      }
      for (int i = area_start; i < area_end; i++) {
	memory[i] = 0xff;
      }
      waitFlashProcess(SEGMENT_ERASE_TIME);
      return;
      
    case ERASE_MAIN:
      if (! main_range.isInRange(address)) {
	return;
      }
      for (int i = main_range.start; i < main_range.end; i++) {
	memory[i] = 0xff;
      }
      waitFlashProcess(MASS_ERASE_TIME);
      return;
      
    case ERASE_ALL:
      for (int i = main_range.start; i < main_range.end; i++) {
	memory[i] = 0xff;
      }
      for (int i = info_range.start; i < main_range.end; i++) {
	memory[i] = 0xff;
      }
      waitFlashProcess(MASS_ERASE_TIME);
      return;
    }
    
    switch(current_write_mode) {
    case WRITE_BLOCK:
      wait = false;
      // TODO: Register target block and verify all writes stay in the same
      // block. What does the real hardware on random writes?!?
      if (blockwrite_count == 0) {
	wait_time = BLOCKWRITE_FIRST_TIME;
	if (DEBUG) {
	  System.out.println("Flash write in block mode started @"
	      + Utils.hex16(address));
	}
	if (addressInFlash(cpu.readRegister(MSP430.PC))) {
	  System.out.println("Oops. Block write access only allowed when" +
	  		" executing from RAM.");
	}
      } else {
	wait_time = BLOCKWRITE_TIME;
      }
      break;
      
    case WRITE_SINGLE:
      wait_time = WRITE_TIME;
      break;
    }
    
    /* Flash memory allows clearing bits only */
    memory[address] &= data & 0xff;
    if (word) {
	memory[address + 1] &= (data >> 8) & 0xff;
    }
    
    if (wait_time < 0) {
      throw new RuntimeException("Wait time not properly initialized");
    }
    waitFlashProcess(wait_time);
  }
  
  public void notifyRead(int address) {
    if (busy) {
      triggerAccessViolation("Flash read not allowed while BUSY flag set");
      return;
    }
    if (DEBUG) {
      if (wait == false && current_write_mode == WriteMode.WRITE_BLOCK) {
	System.out.println("Reading flash prohibited. Would read 0x3fff!!!\n" 
	    + "CPU PC=" + Utils.hex16(cpu.readRegister(MSP430.PC)) 
	    + " read address=" + Utils.hex16(address));
      }
    }
  }
  
  private FlashRange getFlashRange(int address) {
    if (main_range.isInRange(address)) {
      return main_range;
    }
    if (info_range.isInRange(address)) {
      return info_range;
    }
    return null;
  }
  
  private void getSegmentRange(int address, int[] start, int[] end) {
    FlashRange addr_type = getFlashRange(address);
    int segsize, ioffset;
    
    if (addr_type == null) {
      throw new RuntimeException("Address not in flash");
    }
    
    segsize = addr_type.segment_size;
    ioffset = address - addr_type.start;
    
    ioffset /= segsize;
    ioffset *= segsize;
    
    start[0] = addr_type.start + ioffset;
    end[0] = start[0] + segsize;
  }
  
  public int read(int address, boolean word, long cycles) {
    if (address == FCTL1) {
      return mode | FRKEY;
    }
    if (address == FCTL2) {
      return clockcfg | FRKEY;
    }
    if (address == FCTL3) {
      int retval = statusreg | FRKEY;
      
      if (busy)
	retval |= BUSY;
      
      if (locked)
	retval |= LOCK;
      
      if (wait)
	retval |= WAIT;
      
      return retval;
    }

    return 0;
  }

  private ClockSource getClockSource() {
    switch((clockcfg & FSSEL_MASK) >> FSSEL_SHIFT) {
      case 0:
      return ClockSource.ACLK;
      case 1:
      return ClockSource.MCLK;
      case 2:
      case 3:
      return ClockSource.SMCLK;
    }
    throw new RuntimeException("Bad clock source");
  }

  private boolean checkKey(int value) {
    if ((value & KEYMASK) == FWKEY)
      return true;

    System.out.println("Bad key accessing flash controller --> reset");
    statusreg |= KEYV;
    cpu.flagInterrupt(RESET_VECTOR, this, true);
    return false;
  }

  private void triggerEmergencyExit() {
    mode = 0;
    busy = false;
    wait = true;
    locked = true;
    current_erase = EraseMode.ERASE_NONE;
    current_write_mode = WriteMode.WRITE_NONE;
    
  }
  
  private EraseMode getEraseMode(int regdata) {
    int idx = (regdata & ERASE_MASK) >> ERASE_SHIFT;
    
    for(EraseMode em : EraseMode.values()) {
      if (em.ordinal() == idx)
	return em;
    }
    throw new RuntimeException("Invalid erase mode");
  }
  
  private void triggerErase(int newmode) {
    current_erase = getEraseMode(newmode);
  }
  
  private void triggerLockFlash() {
    locked = true;
  }
  
  private void triggerUnlockFlash() {
    locked = false;
  }
  
  private void triggerAccessViolation(String reason) {
    if (DEBUG)
      System.out.println("Flash access violation: " + reason +
	  "\nPC=" + Utils.hex16(cpu.readRegister(MSP430.PC)));
    
    statusreg |= ACCVIFG;
    if (sfr.isIEBitsSet(SFR.IE1, ACCVIE)) {
      cpu.flagInterrupt(NMI_VECTOR, this, true);
    }
  }
  
  private void triggerSingleWrite() {
    /*if (DEBUG) {
      System.out.println("Single write triggered");
    }*/
    current_write_mode = WriteMode.WRITE_SINGLE;
  }
  
  private void triggerBlockWrite() {
    if (DEBUG) {
      System.out.println("Block write triggered");
    }
    current_write_mode = WriteMode.WRITE_BLOCK;
    blockwrite_count = 0;
  }
  
  private void triggerEndBlockWrite() {
    if (DEBUG) {
      System.out.println("Got end of flash block write");
    }
    current_write_mode = WriteMode.WRITE_BLOCK_FINISH;
    waitFlashProcess(BLOCKWRITE_END_TIME);
  }
  
  public void write(int address, int value, boolean word, long cycles) {
    if (!word) {
      System.out.println("Invalid access type to flash controller");
      return;
    }

    if (!(address == FCTL1 || address == FCTL2 || address == FCTL3)) {
      return;
    }

    if (!checkKey(value)) {
      return;
    }

    int regdata = value & CMDMASK;
    switch (address) {
    case FCTL1:
      // access violation while erase/write in progress
      // exception: block write mode and WAIT==1
      if ((mode & ERASE_MASK) != 0 || (mode & WRT) != 0) {
	if (!((mode & BLKWRT) != 0 && wait)) {
	  triggerAccessViolation(
	      "FCTL1 write not allowed while erase/write active.");
	  return;
	}
      }

      if ((mode & ERASE_MASK) != (regdata & ERASE_MASK)) {
	if ((mode & ERASE_MASK) == 0) {
	  triggerErase(regdata);
	}
	mode &= ~ERASE_MASK;
	mode |= regdata & ERASE_MASK;
      }

      if ((mode & WRT) != (regdata & WRT)) {
	if ((regdata & WRT) != 0) {
	  if ((regdata & BLKWRT) != 0) {
	    triggerBlockWrite();
	    mode |= BLKWRT;
	  } else {
	    triggerSingleWrite();
	  }
	  mode &= ~WRT;
	  mode |= regdata & WRT;
	}
      }
      
      if ((mode & BLKWRT) != 0 && (regdata & BLKWRT) == 0) {
	triggerEndBlockWrite();
	mode &= ~BLKWRT;
      }
      break;

    case FCTL2:
      // access violation if BUSY==1
      if (busy) {
	triggerAccessViolation(
	    "Register write to FCTL2 not allowed when busy");
	return;
      }
      clockcfg = regdata;
      break;

    case FCTL3:
      if ((statusreg & EMEX) == 0 && (regdata & EMEX) == 1) {
	triggerEmergencyExit();
      }

      if (locked && (regdata & LOCK) == 0) {
	triggerUnlockFlash();
      } else {
	if (!locked && (regdata & LOCK) != 0) {
	  triggerLockFlash();
	}
      }

      if (((statusreg ^ regdata) & KEYV) != 0) {
	statusreg ^= KEYV;
      }
      if (((statusreg ^ regdata) & ACCVIFG) != 0) {
	statusreg ^= ACCVIFG;
      }

      break;
    }
  }

  public void reset(int type) {
    if (DEBUG) {
      System.out.println("Flash got reset!");
    }

    if (type == MSP430.RESET_POR)
      statusreg = 0;
    
    mode = 0;
    clockcfg = 0x42;
    busy = false;
    wait = true;
    locked = true;
    current_erase = EraseMode.ERASE_NONE;
    current_write_mode = WriteMode.WRITE_NONE;
  }
}
