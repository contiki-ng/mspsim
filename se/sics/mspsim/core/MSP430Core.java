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
 * $Id: MSP430Core.java,v 1.4 2007/10/22 18:03:41 joakime Exp $
 *
 * -----------------------------------------------------------------
 *
 * MSP430Core
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/22 18:03:41 $
 *           $Revision: 1.4 $
 */

package se.sics.mspsim.core;
import se.sics.mspsim.util.Utils;

/**
 * The CPU of the MSP430
 */
public class MSP430Core implements MSP430Constants {

  public static final boolean DEBUG = false;
  public static final boolean debugInterrupts = false;

  // Try it out with 64 k memory
  public static final int MAX_MEM = 64*1024;
  public static final int INTERNAL_IO_SIZE = 5;
  public static final int PORTS = 6;

  // 16 registers of which some are "special" - PC, SP, etc.
  public int[] reg = new int[16];

  public CPUMonitor[] regWriteMonitors = new CPUMonitor[16];
  public CPUMonitor[] regReadMonitors = new CPUMonitor[16];

  // For breakpoints, etc... how should memory monitors be implemented?
  // Maybe monitors should have a "next" pointer...? or just have a [][]?
  public CPUMonitor[] breakPoints = new CPUMonitor[MAX_MEM];
  // true => breakpoints can occur!
  boolean breakpointActive = true;

  public int memory[] = new int[MAX_MEM];
  public long cycles = 0;
  public long cpuCycles = 0;

  // Most HW needs only notify write and clocking, others need also read...
  // For notify write...
  public IOUnit[] memOut = new IOUnit[MAX_MEM];
  // For notify read... -> which will happen before actual read!
  public IOUnit[] memIn = new IOUnit[MAX_MEM];

  private IOUnit[] ioUnits;
  private IOUnit[] passiveIOUnits;
  private SFR sfr;
  private long[] ioCycles;
  private long nextIOTickCycles;
  private int nextIOTickIndex;

  private int lastIOUnitPos = INTERNAL_IO_SIZE;

  // From the possible interrupt sources - to be able to indicate iserviced.
  private IOUnit interruptSource[] = new IOUnit[16];

  private int interruptMax = -1;
  // Op/instruction represents the last executed OP / instruction
  int op;
  int instruction;
  int servicedInterrupt = -1;
  IOUnit servicedInterruptUnit = null;

  boolean interruptsEnabled = false;
  boolean cpuOff = false;

  // Not private since they are needed (for fast access...)
  int dcoFrq = 2500000;
  int aclkFrq = 32768;
  int smclkFrq = dcoFrq;

  // Clk A can be "captured" by timers - needs to be handled close to CPU...?
  private int clkACaptureMode = CLKCAPTURE_NONE;
  // Other clocks too...

  public MSP430Core(int type) {
    // Ignore type for now...

    // Internal Active IOUnits
    ioUnits = new IOUnit[INTERNAL_IO_SIZE + 10];
    ioCycles = new long[INTERNAL_IO_SIZE + 10];

    // Passive IOUnits (no tick) - do we need to remember them???
    // Maybe for debugging purposes...
    passiveIOUnits = new IOUnit[PORTS];

    Timer ta = new Timer(this, Timer.TIMER_Ax149, memory, 0x160);
    Timer tb = new Timer(this, Timer.TIMER_Bx149, memory, 0x180);
    for (int i = 0, n = 0x20; i < n; i++) {
      memOut[0x160 + i] = ta;
      memOut[0x180 + i] = tb;
      memIn[0x160 + i] = ta;
      memIn[0x180 + i] = tb;
    }

    sfr = new SFR(this, memory);
    for (int i = 0, n = 0x10; i < n; i++) {
      memOut[i] = sfr;
      memIn[i] = sfr;
    }

    memIn[Timer.TAIV] = ta;
    memIn[Timer.TBIV] = tb;

    BasicClockModule bcs = new BasicClockModule(this, memory, 0);
    for (int i = 0x56, n = 0x59; i < n; i++) {
      memOut[i] = bcs;
    }

    Multiplier mp = new Multiplier(this, memory, 0);
    // Only cares of writes!
    for (int i = 0x130, n = 0x13f; i < n; i++) {
      memOut[i] = mp;
    }

    ioUnits[0] = ta;
    ioUnits[1] = tb;
    ioUnits[2] = bcs;

    USART usart0 = new USART(this, memory, 0x70);
    USART usart1 = new USART(this, memory, 0x78);

    ioUnits[3] = usart0;
    ioUnits[4] = usart1;

    for (int i = 0, n = 8; i < n; i++) {
      memOut[0x70 + i] = usart0;
      memIn[0x70 + i] = usart0;

      memOut[0x78 + i] = usart1;
      memIn[0x78 + i] = usart1;
    }

    // Add port 1,2 with interrupt capability!
    passiveIOUnits[0] = new IOPort(this, "1", 4, memory, 0x20);
    passiveIOUnits[1] = new IOPort(this, "2", 1, memory, 0x28);
    for (int i = 0, n = 8; i < n; i++) {
      memOut[0x20 + i] = passiveIOUnits[0];
      memOut[0x28 + i] = passiveIOUnits[1];
    }

    // Add port 3,4 & 5,6
    for (int i = 0, n = 2; i < n; i++) {
      passiveIOUnits[i + 2] = new IOPort(this, "" + (3 + i), 0,
					 memory, 0x18 + i * 4);
      memOut[0x18 + i * 4] = passiveIOUnits[i + 2];
      memOut[0x19 + i * 4] = passiveIOUnits[i + 2];
      memOut[0x1a + i * 4] = passiveIOUnits[i + 2];
      memOut[0x1b + i * 4] = passiveIOUnits[i + 2];

      passiveIOUnits[i + 4] = new IOPort(this, "" + (5 + i), 0,
					 memory, 0x30 + i * 4);

      memOut[0x30 + i * 4] = passiveIOUnits[i + 4];
      memOut[0x31 + i * 4] = passiveIOUnits[i + 4];
      memOut[0x32 + i * 4] = passiveIOUnits[i + 4];
      memOut[0x33 + i * 4] = passiveIOUnits[i + 4];
    }

    initIOUnit();
  }


  public SFR getSFR() {
    return sfr;
  }

  public void addIOUnit(int loReadMem, int hiReadMem,
			int loWriteMem, int hiWriteMem,
			IOUnit unit, boolean active) {
    // Not implemented yet... IS it needed?
//     if (loReadMem != -1) {
//       for (int i = lo, n = hiMem; i < n; i++) {
//       }
//     }

    if (active) {
      ioUnits[lastIOUnitPos++] = unit;
    }
  }

  public void setBreakPoint(int address, CPUMonitor mon) {
    breakPoints[address] = mon;
  }

  public boolean hasBreakpoint(int address) {
    return breakPoints[address] != null;
  }

  public void setRegisterWriteMonitor(int r, CPUMonitor mon) {
    regWriteMonitors[r] = mon;
  }

  public void setRegisterReadMonitor(int r, CPUMonitor mon) {
    regReadMonitors[r] = mon;
  }

  public int[] getMemory() {
    return memory;
  }

  public void writeRegister(int r, int value) {
    // Before the write!
    if (regWriteMonitors[r] != null) {
      regWriteMonitors[r].cpuAction(CPUMonitor.REGISTER_WRITE, r, value);
    }
    reg[r] = value;
    if (r == SR) {
      boolean oldCpuOff = cpuOff;
      interruptsEnabled = ((value & GIE) == GIE);
      cpuOff = ((value & CPUOFF) == CPUOFF);
      if (cpuOff != oldCpuOff) {
// 	System.out.println("LPM CPUOff: " + cpuOff + " cycles: " + cycles);
      }
    }
  }

  public int readRegister(int r) {
    if (regReadMonitors[r] != null) {
      regReadMonitors[r].cpuAction(CPUMonitor.REGISTER_READ, r, reg[r]);
    }
    return reg[r];
  }

  public int readRegisterCG(int r, int m) {
      // CG1 + m == 0 => SR!
    if ((r == CG1 && m != 0) || r == CG2) {
      // No monitoring here... just return the CG values
      return CREG_VALUES[r - 2][m];
    }
    if (regReadMonitors[r] != null) {
      regReadMonitors[r].cpuAction(CPUMonitor.REGISTER_READ, r, reg[r]);
    }
    return reg[r];
  }

  public int incRegister(int r, int value) {
    if (regReadMonitors[r] != null) {
      regReadMonitors[r].cpuAction(CPUMonitor.REGISTER_READ, r, reg[r]);
    }
    if (regWriteMonitors[r] != null) {
      regWriteMonitors[r].cpuAction(CPUMonitor.REGISTER_WRITE, r,
				    reg[r] + value);
    }
    reg[r] += value;
    return reg[r];
  }

  public void setACLKFrq(int frequency) {
    aclkFrq = frequency;
  }

  public void setDCOFrq(int frequency, int smclkFrq) {
    dcoFrq = frequency;
    this.smclkFrq = smclkFrq;
    if (DEBUG)
      System.out.println("Set smclkFrq: " + smclkFrq);
  }

  // Should also return avtieve units...
  public IOUnit getIOUnit(String name) {
    for (int i = 0, n = passiveIOUnits.length; i < n; i++) {
      if (name.equals(passiveIOUnits[i].getName())) {
	return passiveIOUnits[i];
      }
    }

    for (int i = 0, n = ioUnits.length; i < n; i++) {
      if (name.equals(ioUnits[i].getName())) {
	return ioUnits[i];
      }
    }

    return null;
  }

  private void initIOUnit() {
    long smallestCyc = 10000000l;
    for (int i = 0, n = lastIOUnitPos; i < n; i++) {
      if ((ioCycles[i] = ioUnits[i].ioTick(0)) < smallestCyc) {
	smallestCyc = ioCycles[i];
	nextIOTickIndex = i;
      }
    }
  }

  private void resetIOUnits() {
    for (int i = 0, n = lastIOUnitPos; i < n; i++) {
      ioUnits[i].reset();
    }
    for (int i = 0, n = passiveIOUnits.length; i < n; i++) {
      passiveIOUnits[i].reset();
    }
  }

  public void reset() {
    resetIOUnits();
    reg[PC] = memory[0xfffe] +  (memory[0xffff] << 8);
    for (int i = 0, n = 16; i < n; i++) {
      interruptSource[i] = null;
    }
    servicedInterruptUnit = null;
    servicedInterrupt = -1;
    interruptMax = -1;
    cpuOff = false;
  }

  // Indicate that we have an interrupt now!
  // We should only get same IOUnit for same interrupt level
  public void flagInterrupt(int interrupt, IOUnit source, boolean triggerIR) {
    if (triggerIR) {
      interruptSource[interrupt] = source;

      if (debugInterrupts) {
	System.out.println("### Interrupt flagged ON by " + source.getName());
      }

      if (interrupt > interruptMax) {
	interruptMax = interrupt;
      }
    } else {
      if (interruptSource[interrupt] == source) {
	if (debugInterrupts) {
	  System.out.println("### Interrupt flagged OFF by " + source.getName());
	}
	interruptSource[interrupt] = null;
      }
    }
  }

  // returns the currently serviced interrupt (vector ID)
  public int getServicedInterrupt() {
    return servicedInterrupt;
  }

  // This will be called after an interrupt have been handled
  // In the main-CPU loop
  public void handlePendingInterrupts() {
    // By default no int. left to process...
    interruptMax = -1;

//     IOUnit src = interruptSource[servicedInterrupt];
    // Remove this src since it has been handled already...
//     interruptSource[servicedInterrupt] = null;
    // And call the serviced routine (which can cause another interrupt)
    if (servicedInterruptUnit != null) {
      if (debugInterrupts) {
	System.out.println("### Calling serviced interrupt on: " +
			   servicedInterruptUnit.getName());
      }
      servicedInterruptUnit.interruptServiced();
    }

    for (int i = 0, n = 16; i < n; i++) {
      if (interruptSource[i] != null)
	interruptMax = i;
    }

    servicedInterrupt = -1;
    servicedInterruptUnit = null;
  }

  private void handleIO() {
    // Call the IO unit!
    // 	System.out.println("Calling: " + ioUnits[nextIOTickIndex].getName());
    ioCycles[nextIOTickIndex] = ioUnits[nextIOTickIndex].ioTick(cycles);

    // Find the next unit to call...
    long smallestCyc = cycles + 1000000l;
    int index = 0;
    for (int i = 0, n = lastIOUnitPos; i < n; i++) {
      if (ioCycles[i] < smallestCyc) {
	smallestCyc = ioCycles[i];
	index = i;
      }
    }
    nextIOTickCycles = smallestCyc;
    nextIOTickIndex = index;
//     System.out.println("Smallest IO cycles: " + smallestCyc + " => " +
// 		       ioUnits[index].getName());
  }

  // Read method that handles read from IO units!
  public int read(int address, boolean word) {
    int val = 0;
    if (address < 0x200 && memIn[address] != null) {
      val = memIn[address].read(address, word, cycles);
    } else {
      address &= 0xffff;
      val = memory[address] & 0xff;
      if (word) {
	val |= (memory[(address + 1) & 0xffff] << 8);
      }
    }
    return val;
  }

  public void write(int dstAddress, int dst, boolean word) {
    if (memOut[dstAddress] != null) {
      if (!word) dst &= 0xff;
      memOut[dstAddress].write(dstAddress, dst, word, cycles);
    } else {
      memory[dstAddress] = dst & 0xff;
      if (word) {
	memory[dstAddress + 1] = (dst >> 8) & 0xff;
      }
    }
  }


  private int serviceInterrupt(int pc) {
    int pcBefore = pc;
    int spBefore = readRegister(SP);
    int sp = spBefore;
    int sr = readRegister(SR);
    // Push PC and SR to stack
    // store on stack - always move 2 steps (W) even if B.
    writeRegister(SP, sp = spBefore - 2);
    // Put lo & hi on stack!
    memory[sp] = pc & 0xff;
    memory[sp + 1] = (pc >> 8) & 0xff;

    writeRegister(SP, sp = sp - 2);
    // Put lo & hi on stack!
    memory[sp] = sr & 0xff;
    memory[sp + 1] = (sr >> 8) & 0xff;

    // Clear SR - except ...
    // Jump to the address specified in the interrupt vector
    writeRegister(PC, pc =
		memory[0xffe0 + interruptMax * 2] +
		(memory[0xffe0 + interruptMax * 2 + 1] << 8));

    writeRegister(SR, sr & ~CPUOFF & ~SCG1 & ~OSCOFF);

    servicedInterrupt = interruptMax;
    servicedInterruptUnit = interruptSource[servicedInterrupt];
    // Flag off this interrupt - for now - as soon as RETI is
    // executed things might change!
    interruptMax = -1;

    // Interrupts take 6 cycles!
    cycles += 6;

    if (debugInterrupts) {
      System.out.println("### Executing interrupt: " +
			 servicedInterrupt + " at "
			 + pcBefore + " to " + pc +
			 " SP before: " + spBefore);
    }
    return pc;
  }

  /* returns true if any instruction was emulated - false if CpuOff */
  public boolean emulateOP() {
    int pc = readRegister(PC);
    long startCycles = cycles;

    // -------------------------------------------------------------------
    // I/O processing
    // -------------------------------------------------------------------
    if (cycles >= nextIOTickCycles) {
      handleIO();
    }


    // -------------------------------------------------------------------
    // Interrupt processing [after the last instruction was executed]
    // -------------------------------------------------------------------
    if (interruptsEnabled && servicedInterrupt == -1 && interruptMax >= 0) {
      pc = serviceInterrupt(pc);
    }

    /* Did not execute any instructions */
    if (cpuOff) {
//       System.out.println("Jumping: " + (nextIOTickCycles - cycles));
      cycles = nextIOTickCycles;
      return false;
    }

    // This is quite costly... should probably be made more
    // efficiently (maybe in CORE where PC is read anyway?)
    if (breakPoints[pc] != null) {
      if (breakpointActive) {
	breakPoints[pc].cpuAction(CPUMonitor.BREAK, pc, 0);
	breakpointActive = false;
	return false;
      } else {
	// Execute this instruction - this is second call...
	breakpointActive = true;
      }
    }


    instruction = memory[pc] + (memory[pc + 1] << 8);
    op = instruction >> 12;
    int sp = 0;
    int sr = 0;
    boolean word = (instruction & 0x40) == 0;

    // Destination vars
    int dstRegister = 0;
    int dstAddress = -1;
    boolean dstRegMode = false;
    int dst = 0;

    boolean write = false;
    boolean updateStatus = true;

    // When is PC increased  probably immediately (e.g. here)?
    pc += 2;
    writeRegister(PC, pc);

    switch (op) {
    case 1:
      // -------------------------------------------------------------------
      //  Single operand instructions
      // -------------------------------------------------------------------
    {
      // Register
      dstRegister = instruction & 0xf;
      // Adress mode of destination...
      int ad = (instruction >> 4) & 3;
      int nxtCarry = 0;
      op = instruction & 0xff80;
      if (op == PUSH) {
	// The PUSH operation increase the SP before address resolution!
	// store on stack - always move 2 steps (W) even if B./
	sp = readRegister(SP) - 2;
	writeRegister(SP, sp);
      }

      if ((dstRegister == CG1 && ad != AM_INDEX) || dstRegister == CG2) {
	dstRegMode = true;
	cycles++;
      } else {
	switch(ad) {
	  // Operand in register!
	case AM_REG:
	  dstRegMode = true;
	  cycles++;
	  break;
	case AM_INDEX:
	  dstAddress = readRegisterCG(dstRegister, ad) +
	    memory[pc] + (memory[pc + 1] << 8);

	  // When is PC incremented - assuming immediately after "read"?
	  pc += 2;
	  writeRegister(PC, pc);
	  cycles += 4;
	  break;
	  // Indirect register
	case AM_IND_REG:
	  dstAddress = readRegister(dstRegister);
	  cycles += 3;
	  break;
	case AM_IND_AUTOINC:
	  dstAddress = readRegister(dstRegister);
	  writeRegister(dstRegister, dstAddress + (word ? 2 : 1));
	  cycles += 3;
	  break;
	}
      }


      // Perform the read
      if (dstRegMode) {
	dst = readRegisterCG(dstRegister, ad);

	if (!word) {
	  dst &= 0xff;
	}
      } else {
	dst = read(dstAddress, word);
      }

      switch(op) {
      case RRC:
	nxtCarry = (dst & 1) > 0 ? CARRY : 0;
	dst = dst >> 1;
	if (word) {
	  dst |= (readRegister(SR) & CARRY) > 0 ? 0x8000 : 0;
	} else {
	  dst |= (readRegister(SR) & CARRY) > 0 ? 0x80 : 0;
	}
	// Indicate write to memory!!
	write = true;
	// Set the next carry!
	writeRegister(SR, (readRegister(SR) & ~CARRY) | nxtCarry);
	break;
      case SWPB:
	int tmp = dst;
	dst = ((tmp >> 8) & 0xff) + ((tmp << 8) & 0xff00);
	write = true;
	break;
      case RRA:
	nxtCarry = (dst & 1) > 0 ? CARRY : 0;
	if (word) {
	  dst = (dst & 0x8000) | (dst >> 1);
	} else {
	  dst = (dst & 0x80) | (dst >> 1);
	}
	write = true;
	writeRegister(SR, (readRegister(SR) & ~CARRY) | nxtCarry);
	break;
      case SXT:
	// Extend Sign (bit 8-15 => same as bit 7)
	dst = (dst & 0x80) > 0 ? dst | 0xff00 : dst & 0x7f;
	write = true;
	break;
      case PUSH:
	if (word) {
	  // Put lo & hi on stack!
	  memory[sp] = dst & 0xff;
	  memory[sp + 1] = dst >> 8;
	} else {
	  // Byte => only lo byte
	  memory[sp] = dst & 0xff;
	  memory[sp + 1] = 0;
	}
	write = false;
	updateStatus = false;
	break;
      case CALL:
	// store current PC on stack... (current PC points to next instr.)
	sp = readRegister(SP) - 2;
	writeRegister(SP, sp);

	pc = readRegister(PC);
	memory[sp] = pc & 0xff;
	memory[sp + 1] = pc >> 8;
	writeRegister(PC, dst);

	write = false;
	updateStatus = false;
	break;
      case RETI:
	// Put Top of stack to Status DstRegister (TOS -> SR)
	sp = readRegister(SP);
	writeRegister(SR, memory[sp++] + (memory[sp++] << 8));
	// TOS -> PC
	writeRegister(PC, memory[sp++] + (memory[sp++] << 8));
	writeRegister(SP, sp);
	write = false;
	updateStatus = false;

	if (debugInterrupts) {
	  System.out.println("### RETI at " + pc + " => " + reg[PC] +
			     " SP after: " + reg[SP]);
	}

	// This assumes that all interrupts will get back using RETI!
	handlePendingInterrupts();

	break;
      default:
	System.out.println("Error: Not implemented instruction:" +
			   instruction);
      }
    }
    break;
    // Jump instructions
    case 2:
    case 3:
      // 10 bits for address for these => 0x00fc => remove 2 bits
      int jmpOffset = instruction & 0x3ff;
      jmpOffset = (jmpOffset & 0x200) == 0 ?
	2 * jmpOffset : -(2 * (0x200 - (jmpOffset & 0x1ff)));
      boolean jump = false;

      // All jump takes one extra cycle
      cycles++;
      sr = readRegister(SR);
      switch(instruction & 0xfc00) {
      case JNE:
	jump = (sr & ZERO) == 0;
	break;
      case JEQ:
	jump = (sr & ZERO) > 0;
	break;
      case JNC:
	jump = (sr & CARRY) == 0;
	break;
      case JC:
	jump = (sr & CARRY) > 0;
	break;
      case JN:
	jump = (sr & NEGATIVE) > 0;
	break;
      case JGE:
	jump = (sr & NEGATIVE) > 0 == (sr & OVERFLOW) > 0;
	break;
      case JL:
	jump = (sr & NEGATIVE) > 0 != (sr & OVERFLOW) > 0;
	break;
      case JMP:
	jump = true;
	break;
      default:
	System.out.println("Not implemented instruction: " +
			   Utils.binary16(instruction));
      }
      // Perform the Jump
      if (jump) {
	writeRegister(PC, pc + jmpOffset);
      }
      break;
    default:
      // ---------------------------------------------------------------
      // Double operand instructions!
      // ---------------------------------------------------------------
      dstRegister = instruction & 0xf;
      int srcRegister = (instruction >> 8) & 0xf;
      int as = (instruction >> 4) & 3;

      // AD: 0 => register direct, 1 => register index, e.g. X(Rn)
      dstRegMode = ((instruction >> 7) & 1) == 0;
      dstAddress = -1;
      int srcAddress = -1;
      int src = 0;

      // Some CGs should be handled as registry reads only...
      if ((srcRegister == CG1 && as != AM_INDEX) || srcRegister == CG2) {
	src = CREG_VALUES[srcRegister - 2][as];
	if (!word) {
	  src &= 0xff;
	}

      } else {
	switch(as) {
	  // Operand in register!
	case AM_REG:
	  // CG handled above!
	  src = readRegister(srcRegister);
	  if (!word) {
	    src &= 0xff;
	  }
	  break;
	case AM_INDEX:
	  // Indexed if reg != PC & CG1/CG2 - will PC be incremented?
	  srcAddress = readRegisterCG(srcRegister, as) +
	    memory[pc] + (memory[pc + 1] << 8);
	  // When is PC incremented - assuming immediately after "read"?
	  incRegister(PC, 2);
	  cycles += dstRegMode ? 3 : 6;
	  break;
	  // Indirect register
	case AM_IND_REG:
	  srcAddress = readRegister(srcRegister);
	  cycles += dstRegMode ? 2 : 5;
	  break;
	case AM_IND_AUTOINC:
	  if (srcRegister == PC) {
	    srcAddress = readRegister(PC);
	    pc += 2;
	    incRegister(PC, 2);
	    cycles += 3;
	  } else {
	    srcAddress = readRegister(srcRegister);
	    incRegister(srcRegister, word ? 2 : 1);
	    cycles += dstRegMode ? 2 : 5;
	  }
	  break;
	}
      }

      // Perform the read of destination!
      if (dstRegMode) {
	dst = readRegister(dstRegister);
	if (!word) {
	  dst &= 0xff;
	}
      } else {
	// PC Could have changed above!
	pc = readRegister(PC);
	if (dstRegister == 2) {
	  dstAddress = memory[pc] + (memory[pc + 1] << 8);
	} else {
	  // CG here???
	  dstAddress = readRegister(dstRegister)
	    + memory[pc] + (memory[pc + 1] << 8);
	}

	dst = read(dstAddress, word);
	pc += 2;
	incRegister(PC, 2);
      }

      // **** Perform the read...
      if (srcAddress != -1) {

	// Got very high address - check that?!!
	srcAddress = srcAddress & 0xffff;

	src = read(srcAddress, word);

	// 	  if (debug) {
	// 	    System.out.println("Reading from " + Utils.hex16(srcAddress) +
	// 			       " => " + src);
	// 	  }
      }

      int tmp = 0;
      int tmpAdd = 0;
      switch (op) {
      case MOV: // MOV
	dst = src;
	write = true;
	break;
	// FIX THIS!!! - make SUB a separate operation so that
	// it is clear that overflow flag is corretct...
      case SUB:
	// Carry always 1 with SUB
	tmpAdd = 1;
      case SUBC:
	// Both sub and subc does one complement (not) + 1 (or carry)
	src = (src ^ 0xffff) & 0xffff;
      case ADDC: // ADDC
	if (op == ADDC || op == SUBC)
	  tmpAdd = ((readRegister(SR) & CARRY) > 0) ? 1 : 0;
      case ADD: // ADD
	// Tmp gives zero if same sign! if sign is different after -> overf.
	sr = readRegister(SR);
	sr &= ~(OVERFLOW | CARRY);

	tmp = (src ^ dst) & (word ? 0x8000 : 0x80);
	// Includes carry if carry should be added...

	dst = dst + src + tmpAdd;

	if (dst > (word ? 0xffff : 0xff)) {
	  sr |= CARRY;
	}
	// If tmp == 0 and currenly not the same sign for src & dst
	if (tmp == 0 && ((src ^ dst) & (word ? 0x8000 : 0x80)) != 0) {
	  sr |= OVERFLOW;
	  // 	    System.out.println("OVERFLOW - ADD/SUB " + Utils.hex16(src)
	  // 			       + " + " + Utils.hex16(tmpDst));
	}

	// 	  System.out.println(Utils.hex16(dst) + " [SR=" +
	// 			     Utils.hex16(reg[SR]) + "]");
	writeRegister(SR, sr);
	write = true;
	break;
      case CMP: // CMP
	// Set CARRY if A >= B, and it's clear if A < B
	int b = word ? 0x8000 : 0x80;
	sr = readRegister(SR);
	sr = (sr & ~(CARRY | OVERFLOW)) | (dst >= src ? CARRY : 0);

	tmp = dst - src;

	if (((src ^ tmp) & b) == 0 && (((src ^ dst) & b) != 0)) {
	  sr |= OVERFLOW;
	}
	writeRegister(SR, sr);
	// Must set dst to the result to set the rest of the status register
	dst = tmp;
	break;
      case DADD: // DADD
	if (DEBUG)
	  System.out.println("DADD: Decimal add executed - result error!!!");
	// Decimal add... this is wrong... each nibble is 0-9...
	// So this has to be reimplemented...
	dst = dst + src + ((readRegister(SR) & CARRY) > 0 ? 1 : 0);
	write = true;
	break;
      case BIT: // BIT
	dst = src & dst;
	sr = readRegister(SR);
	// Clear overflow and carry!
	sr = sr & ~(CARRY | OVERFLOW);
	// Set carry if result is non-zero!
	if (dst != 0) {
	  sr |= CARRY;
	}
	writeRegister(SR, sr);
	break;
      case BIC: // BIC
	// No status reg change
	// 	  System.out.println("BIC: =>" + Utils.hex16(dstAddress) + " => "
	// 			     + Utils.hex16(dst) + " AS: " + as +
	// 			     " sReg: " + srcRegister + " => " + src +
	// 			     " dReg: " + dstRegister + " => " + dst);
	dst = (~src) & dst;

	write = true;
	updateStatus = false;
	break;
      case BIS: // BIS
	dst = src | dst;
	write = true;
	updateStatus = false;
	break;
      case XOR: // XOR
	dst = src ^ dst;
	write = true;
	break;
      case AND: // AND
	dst = src & dst;
	write = true;
	break;
      default:
	System.out.println("DoubleOperand not implemented: " + op +
			   " at " + pc);
      }
    }
    dst &= 0xffff;

    if (write) {
      if (dstRegMode) {
	writeRegister(dstRegister, dst);
      } else {
	dstAddress &= 0xffff;
	write(dstAddress, dst, word);
      }
    }
    if (updateStatus) {
      // Update the Zero and Negative status!
      // Carry and overflow must be set separately!
      sr = readRegister(SR);
      sr = (sr & ~(ZERO | NEGATIVE)) |
	((dst == 0) ? ZERO : 0) |
	(word ? ((dst & 0x8000) > 0 ? NEGATIVE : 0) :
	 ((dst & 0x80) > 0 ? NEGATIVE : 0));
      writeRegister(SR, sr);
    }

    cpuCycles += cycles - startCycles;
    return true;
  }
}
