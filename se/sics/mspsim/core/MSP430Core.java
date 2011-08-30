/**
 * Copyright (c) 2007, 2008, 2009, Swedish Institute of Computer Science.
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
 * MSP430Core
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.core;
import java.io.PrintStream;
import java.util.ArrayList;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.MapEntry;
import se.sics.mspsim.util.MapTable;
import se.sics.mspsim.util.Utils;

/**
 * The CPU of the MSP430
 */
public class MSP430Core extends Chip implements MSP430Constants {

  public static final int RETURN = 0x4130;

  public static final boolean debugInterrupts = false;

  public static final boolean EXCEPTION_ON_BAD_OPERATION = true;

  // Try it out with 64 k memory
  public final int MAX_MEM;
  public final int MAX_MEM_IO;
  public static final int PORTS = 6;
  
  // 16 registers of which some are "special" - PC, SP, etc.
  public int[] reg = new int[16];

  public CPUMonitor globalMonitor;
  
  public CPUMonitor[] regWriteMonitors = new CPUMonitor[16];
  public CPUMonitor[] regReadMonitors = new CPUMonitor[16];
  
  // For breakpoints, etc... how should memory monitors be implemented?
  // Maybe monitors should have a "next" pointer...? or just have a [][]?
  public CPUMonitor[] breakPoints;
  // true => breakpoints can occur!
  boolean breakpointActive = true;

  public int memory[];
  public long cycles = 0;
  public long cpuCycles = 0;
  MapTable map;
  public final boolean MSP430XArch;
  public final MSP430Config config;

  // Most HW needs only notify write and clocking, others need also read...
  // For notify write...
  public IOUnit[] memOut;
  // For notify read... -> which will happen before actual read!
  public IOUnit[] memIn;

  private ArrayList<IOUnit> ioUnits;
  private SFR sfr;
  private Watchdog watchdog;

  // From the possible interrupt sources - to be able to indicate is serviced.
  // NOTE: 64 since more modern MSP430's have more than 16 vectors (5xxx has 64).
  private InterruptHandler interruptSource[] = new InterruptHandler[64];
  private final int MAX_INTERRUPT;
  
  protected int interruptMax = -1;
  // Op/instruction represents the last executed OP / instruction
  private int op;
  public int instruction;
  private int extWord;
  int servicedInterrupt = -1;
  InterruptHandler servicedInterruptUnit = null;

  protected boolean interruptsEnabled = false;
  protected boolean cpuOff = false;

  // Not private since they are needed (for fast access...)
  protected int dcoFrq = 2500000;
  int aclkFrq = 32768;
  int smclkFrq = dcoFrq;

  long lastCyclesTime = 0;
  long lastVTime = 0;
  long currentTime = 0;
  long lastMicrosDelta;
  double currentDCOFactor = 1.0;
  
  // Clk A can be "captured" by timers - needs to be handled close to CPU...?
//  private int clkACaptureMode = CLKCAPTURE_NONE;
  // Other clocks too...
  long nextEventCycles;
  private EventQueue vTimeEventQueue = new EventQueue();
  private long nextVTimeEventCycles;

  private EventQueue cycleEventQueue = new EventQueue();
  private long nextCycleEventCycles;
  
  private ArrayList<Chip> chips = new ArrayList<Chip>();

  ComponentRegistry registry;
  Profiler profiler;
  private Flash flash;

  boolean isFlashBusy;
  
  public void setIO(int adr, IOUnit io, boolean word) {
      memOut[adr] = io;
      memIn[adr] = io;
      if (word) {
          memOut[adr + 1] = io;
          memIn[adr + 1] = io;
      }
  }

  public void setIORange(int adr, int size, IOUnit io) {
      for (int i = 0; i < size; i++) {
          memOut[adr + i] = io;
          memIn[adr + i] = io;        
      }
  }
  
  public MSP430Core(int type, ComponentRegistry registry, MSP430Config config) {
    super("MSP430", "MSP430 Core", null);
    MAX_INTERRUPT = config.maxInterruptVector;
    MAX_MEM_IO = config.maxMemIO;
    MAX_MEM = config.maxMem;
    memOut = new IOUnit[MAX_MEM_IO];
    memIn = new IOUnit[MAX_MEM_IO];
    MSP430XArch = config.MSP430XArch;

    memory = new int[MAX_MEM];
    breakPoints = new CPUMonitor[MAX_MEM];

    System.out.println("Set up MSP430 Core with " + MAX_MEM + " bytes memory");
    
    /* this is for detecting writes/read to/from non-existing IO */
    IOUnit voidIO = new IOUnit(id, memory, 0) {
        public void interruptServiced(int vector) {
        }
        public void write(int address, int value, boolean word, long cycles) {
            logw("*** IOUnit write to non-existent IO at $" + Utils.hex16(address));
        }
        public int read(int address, boolean word, long cycles) {
            logw("*** IOUnit read from non-existent IO at $" + Utils.hex16(address));
            return 0;
        }
    };

    /* fill with void IO */
    for (int i = 0; i < MAX_MEM_IO; i++) {
        memOut[i] = voidIO;
        memIn[i] = voidIO;
    }
    
    this.registry = registry;
    this.config = config;
    // The CPU need to register itself as chip
    addChip(this);

    // Ignore type for now...
    setModeNames(MODE_NAMES);
    // IOUnits should likely be placed in a hashtable?
    // Maybe for debugging purposes...
    ioUnits = new ArrayList<IOUnit>();

    flash = new Flash(this, memory,
        new FlashRange(config.mainFlashStart, config.mainFlashStart + config.mainFlashSize, 512, 64),
        new FlashRange(config.infoMemStart, config.infoMemStart + config.infoMemSize, 128, 64),
        config.flashControllerOffset);
    for (int i = 0; i < 8; i++) {
      memOut[i + config.flashControllerOffset] = flash;
      memIn[i + config.flashControllerOffset] = flash;
    }
 
    /* Setup special function registers */
    sfr = new SFR(this, memory);
    for (int i = 0, n = 0x10; i < n; i++) {
      memOut[i + config.sfrOffset] = sfr;
      memIn[i + config.sfrOffset] = sfr;
    }

    // first step towards making core configurable
    Timer[] timers = new Timer[config.timerConfig.length];
    
    for (int i = 0; i < config.timerConfig.length; i++) {
        Timer t = new Timer(this, memory, config.timerConfig[i]);
        for (int a = 0, n = 0x20; a < n; a++) {
            memOut[config.timerConfig[i].offset + a] = t;
            memIn[config.timerConfig[i].offset + a] = t;
        }
        memIn[config.timerConfig[i].timerIVAddr] = t;
        memOut[config.timerConfig[i].timerIVAddr] = t;
        
        timers[i] = t;
    }

    BasicClockModule bcs = new BasicClockModule(this, memory, 0, timers);
    for (int i = 0x56, n = 0x59; i < n; i++) {
      memOut[i] = bcs;
      memIn[i] = bcs;
    }
    
    
    // SFR and Basic clock system.
    ioUnits.add(sfr);
    ioUnits.add(bcs);

    config.setup(this, ioUnits);

    /* timers after ports ? */
    for (int i = 0; i < timers.length; i++) {
        ioUnits.add(timers[i]);
    }

    watchdog = new Watchdog(this);
    memOut[config.watchdogOffset] = watchdog;
    memIn[config.watchdogOffset] = watchdog;

    ioUnits.add(watchdog);
  }

  public Profiler getProfiler() {
    return profiler;
  }

  public void setProfiler(Profiler prof) {
    registry.registerComponent("profiler", prof);
    profiler = prof;
    profiler.setCPU(this);
  }

  public void setGlobalMonitor(CPUMonitor mon) {
      globalMonitor = mon;
  }

  public ComponentRegistry getRegistry() {
    return registry;
  }

  public SFR getSFR() {
    return sfr;
  }

  public void addChip(Chip chip) {
    chips.add(chip);
    chip.setEmulationLogger(logger);
  }

  public Chip getChip(String name) {
    for(Chip chip : chips) {
      if (name.equalsIgnoreCase(chip.getID()) || name.equalsIgnoreCase(chip.getName())) {
        return chip;
      }
    }
    return null;
  }

  public Chip getChip(Class<? extends Chip> type) {
    for(Chip chip : chips) {
      if (type.isInstance(chip)) {
        return chip;
      }
    }
    return null;
  }

  public Loggable[] getLoggables() {
      Loggable[] ls = new Loggable[ioUnits.size() + chips.size()];
      for (int i = 0; i < ioUnits.size(); i++) {
          ls[i] = ioUnits.get(i);
      }
      for (int i = 0; i < chips.size(); i++) {
          ls[i + ioUnits.size()] = chips.get(i);
      }
      return ls;
  }

  public Loggable getLoggable(String name) {
      Loggable l = getChip(name);
      if (l == null) {
          l = getIOUnit(name);
      }
      return l;
  }
  
  public Chip[] getChips() {
    return chips.toArray(new Chip[chips.size()]);
  }
  
  public void setBreakPoint(int address, CPUMonitor mon) {
    breakPoints[address] = mon;
  }

  public boolean hasBreakPoint(int address) {
    return breakPoints[address] != null;
  }
  
  public void clearBreakPoint(int address) {
    breakPoints[address] = null;
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
      if (debugInterrupts) {
          if (((value & GIE) == GIE) != interruptsEnabled) {
              System.out.println("InterruptEnabled changed: " + !interruptsEnabled);
          }
      }
      boolean oldIE = interruptsEnabled;
      interruptsEnabled = ((value & GIE) == GIE);

//      if (debugInterrupts) System.out.println("Wrote to InterruptEnabled: " + interruptsEnabled + " was: " + oldIE);
      
      if (oldIE == false && interruptsEnabled && servicedInterrupt >= 0) {
//          System.out.println("*** Interrupts enabled while in interrupt : " +
//                  servicedInterrupt + " PC: $" + getAddressAsString(reg[PC]));
          /* must handle pending immediately */
          handlePendingInterrupts();
      }
      
      cpuOff = ((value & CPUOFF) == CPUOFF);
      if (cpuOff != oldCpuOff) {
// 	System.out.println("LPM CPUOff: " + cpuOff + " cycles: " + cycles);
      }
      if (cpuOff) {
        boolean scg0 = (value & SCG0) == SCG0;
        boolean scg1 = (value & SCG1) == SCG1;
        boolean oscoff = (value & OSCOFF) == OSCOFF;
        if (oscoff && scg1 && scg0) {
          setMode(MODE_LPM4);
        } else if (scg1 && scg0){
          setMode(MODE_LPM3);
        } else if (scg1) {
          setMode(MODE_LPM2);
        } else if (scg0) {
          setMode(MODE_LPM1);
        } else {
          setMode(MODE_LPM0); 
        }
      } else {
        setMode(MODE_ACTIVE);
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
    // update last virtual time before updating DCOfactor
    lastVTime = getTime();
    lastCyclesTime = cycles;
    lastMicrosDelta = 0;
    
    currentDCOFactor = 1.0 * BasicClockModule.MAX_DCO_FRQ / frequency;

//    System.out.println("*** DCO: MAX:" + BasicClockModule.MAX_DCO_FRQ +
//    " current: " + frequency + " DCO_FAC = " + currentDCOFactor);
    if (DEBUG)
      log("Set smclkFrq: " + smclkFrq);
    dcoReset();
  }

  /* called after dcoReset */
  protected void dcoReset() {
  }
  
  // returns global time counted in max speed of DCOs (~5Mhz)
  public long getTime() {
    long diff = cycles - lastCyclesTime;
    return lastVTime + (long) (diff * currentDCOFactor);
  }

  // Converts a virtual time to a cycles time according to the current
  // cycle speed
  private long convertVTime(long vTime) {
    long tmpTime = lastCyclesTime + (long) ((vTime - lastVTime) / currentDCOFactor);
//    System.out.println("ConvertVTime: vTime=" + vTime + " => " + tmpTime);
    return tmpTime;
  }
  
  // get elapsed time in seconds
  public double getTimeMillis() {
    return 1000.0 * getTime() / BasicClockModule.MAX_DCO_FRQ;
  }
  
  private void executeEvents() {
    if (cycles >= nextVTimeEventCycles) {
      if (vTimeEventQueue.eventCount == 0) {
        nextVTimeEventCycles = cycles + 10000;
      } else {
        TimeEvent te = vTimeEventQueue.popFirst();
        long now = getTime();
//        if (now > te.time) {
//          System.out.println("VTimeEvent got delayed by: " + (now - te.time) + " at " +
//              cycles + " target Time: " + te.time + " class: " + te.getClass().getName());
//        }
        te.execute(now);
        if (vTimeEventQueue.eventCount > 0) {
          nextVTimeEventCycles = convertVTime(vTimeEventQueue.nextTime);
        } else {
          nextVTimeEventCycles = cycles + 10000;          
        }
      }
    }
    
    if (cycles >= nextCycleEventCycles) {
      if (cycleEventQueue.eventCount == 0) {
        nextCycleEventCycles = cycles + 10000;
      } else {
        TimeEvent te = cycleEventQueue.popFirst();
        te.execute(cycles);
        if (cycleEventQueue.eventCount > 0) {
          nextCycleEventCycles = cycleEventQueue.nextTime;
        } else {
          nextCycleEventCycles = cycles + 10000;          
        }
      }
    }
    
    // Pick the one with shortest time in the future.
    nextEventCycles = nextCycleEventCycles < nextVTimeEventCycles ? 
        nextCycleEventCycles : nextVTimeEventCycles;
  }
  
  /**
   * Schedules a new Time event using the cycles counter
   * @param event
   * @param time
   */
  public void scheduleCycleEvent(TimeEvent event, long cycles) {
    long currentNext = cycleEventQueue.nextTime;
    cycleEventQueue.addEvent(event, cycles);
    if (currentNext != cycleEventQueue.nextTime) {
      nextCycleEventCycles = cycleEventQueue.nextTime;
      if (nextEventCycles > nextCycleEventCycles) {
        nextEventCycles = nextCycleEventCycles;
      }
    }
  }

  
  /**
   * Schedules a new Time event using the virtual time clock
   * @param event
   * @param time
   */
  public void scheduleTimeEvent(TimeEvent event, long time) {
    long currentNext = vTimeEventQueue.nextTime;
    vTimeEventQueue.addEvent(event, time);
    if (currentNext != vTimeEventQueue.nextTime) {
      // This is only valid when not having a cycle event queue also...
      // if we have it needs to be checked also!
      nextVTimeEventCycles = convertVTime(vTimeEventQueue.nextTime);
      if (nextEventCycles > nextVTimeEventCycles) {
        nextEventCycles = nextVTimeEventCycles;
      }
      /* Warn if someone schedules a time backwards in time... */
      if (cycles > nextVTimeEventCycles) {
        logger.warning(this, "Scheduling time event backwards in time!!!");
        throw new IllegalStateException("Cycles are passed desired future time...");
      }
    }
  }
  
  
  /**
   * Schedules a new Time event msec milliseconds in the future
   * @param event
   * @param time
   */
  public long scheduleTimeEventMillis(TimeEvent event, double msec) {
    long time = (long) (getTime() + msec / 1000 * BasicClockModule.MAX_DCO_FRQ);
//    System.out.println("Scheduling at: " + time + " (" + msec + ") getTime: " + getTime());
    scheduleTimeEvent(event, time);
    return time;
  }

  public void printEventQueues(PrintStream out) {
      out.println("Current cycles: " + cycles + "  virtual time:" + getTime());
      out.println("Cycle event queue: (next time: " + nextCycleEventCycles + ")");
      cycleEventQueue.print(out);
      out.println("Virtual time event queue: (next time: " + nextVTimeEventCycles + ")");
      vTimeEventQueue.print(out);
  }
 
  // Should also return active units...
  public IOUnit getIOUnit(String name) {
    for (IOUnit ioUnit : ioUnits) {
      if (name.equalsIgnoreCase(ioUnit.getID()) ||
          name.equalsIgnoreCase(ioUnit.getName())) {
	return ioUnit;
      }
    }
    return null;
  }

  private void resetIOUnits() {
    for (int i = 0, n = ioUnits.size(); i < n; i++) {
      ioUnits.get(i).reset(RESET_POR);
    }
  }
  
  private void internalReset() {
    for (int i = 0, n = 64; i < n; i++) {
      interruptSource[i] = null;
    }
    servicedInterruptUnit = null;
    servicedInterrupt = -1;
    interruptMax = -1;
    writeRegister(SR, 0);
   
    cycleEventQueue.removeAll();
    vTimeEventQueue.removeAll();

    for (Chip chip : chips) {
      chip.notifyReset();
    }
    // Needs to be last since these can add events...
    resetIOUnits();
  
    if (profiler != null) {
        profiler.resetProfile();
    }
  }

  public void setWarningMode(EmulationLogger.WarningMode mode) {
    if (logger != null) {
      logger.setWarningMode(mode);
    }
  }
  
  public void reset() {
    flagInterrupt(MAX_INTERRUPT, null, true);
  }

  // Indicate that we have an interrupt now!
  // We should only get same IOUnit for same interrupt level
  public void flagInterrupt(int interrupt, InterruptHandler source,
      boolean triggerIR) {
    if (triggerIR) {
      interruptSource[interrupt] = source;

      if (debugInterrupts) {
        if (source != null) {
          System.out.println("### Interrupt " + interrupt  + " flagged ON by " + source.getName() + " prio: " + interrupt);
        } else {
          System.out.println("### Interrupt " + interrupt + " flagged ON by <null>");
        }
      }

      // MAX priority is executed first - update max if this is higher!
      if (interrupt > interruptMax) {
	interruptMax = interrupt;
      }
      if (interruptMax == MAX_INTERRUPT) {
          // This can not be masked at all!
          interruptsEnabled = true;
      }
    } else {
      if (interruptSource[interrupt] == source) {
	if (debugInterrupts) {
	  System.out.println("### Interrupt flagged OFF by " + source.getName() + " prio: " + interrupt);
	}
	interruptSource[interrupt] = null;
	reevaluateInterrupts();
      }
    }
  }

  private void reevaluateInterrupts() {
    interruptMax = -1;
    for (int i = 0; i < interruptSource.length; i++) {
      if (interruptSource[i] != null)
        interruptMax = i;
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
    
    reevaluateInterrupts();
    
    servicedInterrupt = -1;
    servicedInterruptUnit = null;
  }
  
  // Read method that handles read from IO units!
  public int read(int address, int mode) throws EmulationException {
      int val = 0;
      if (address > MAX_MEM) {
          printWarning(ADDRESS_OUT_OF_BOUNDS_READ, address);
          address %= MAX_MEM;
      }
      boolean word = mode != MODE_BYTE;
      // Only word reads at 0x1fe which is highest address...
      if (address < MAX_MEM_IO) {
          val = memIn[address].read(address, word, cycles);
          if (mode == MODE_WORD20) {
              val |= memIn[address + 2].read(address, word, cycles) << 16;
          }
      } else {
          if (isFlashBusy && flash.addressInFlash(address)) {
              flash.notifyRead(address);
          }

          val = memory[address] & 0xff;
          if (mode > MODE_BYTE) {
              val |= (memory[address + 1] << 8);
              if ((address & 1) != 0) {
                  printWarning(MISALIGNED_READ, address);
              }
              if (mode == MODE_WORD20) {
                  /* will the read really get data from the full word? CHECK THIS */
                  val |= (memory[address + 2] << 16) | (memory[address + 3] << 24);
                  val &= 0xfffff;
              } else {
                  val &= 0xffff;
              }
          }
      }
      if (breakPoints[address] != null) {
          breakPoints[address].cpuAction(CPUMonitor.MEMORY_READ, address, val);
      }
      /* is a null check as fast as a boolean check ?*/
      if (globalMonitor != null) {
          globalMonitor.cpuAction(CPUMonitor.MEMORY_READ, address, val);
      }
      return val;
  }
  
  public void write(int dstAddress, int dst, int mode) throws EmulationException {
    // TODO: optimize memory usage by tagging memory's higher bits.
    // will also affect below flash write stuff!!!
      if (dstAddress > MAX_MEM) {
          printWarning(ADDRESS_OUT_OF_BOUNDS_WRITE, dstAddress);
          dstAddress %= MAX_MEM;
      }
      
      if (breakPoints[dstAddress] != null) {
      breakPoints[dstAddress].cpuAction(CPUMonitor.MEMORY_WRITE, dstAddress, dst);
    }
    boolean word = mode != MODE_BYTE;

    // Only word writes at 0x1fe which is highest address...
    if (dstAddress < MAX_MEM_IO) {
      if (!word) dst &= 0xff;
      memOut[dstAddress].write(dstAddress, dst & 0xffff, mode != MODE_BYTE, cycles);
      if (mode > MODE_WORD) {
          memOut[dstAddress].write(dstAddress + 2, dst >> 16, mode != MODE_BYTE, cycles);
      }
      // check for Flash
    } else if (flash.addressInFlash(dstAddress)) {
      flash.flashWrite(dstAddress, dst & 0xffff, word);
      if (mode > MODE_WORD) {
          flash.flashWrite(dstAddress + 2, dst >> 16, word);
      }
    } else {
      // assume RAM
      memory[dstAddress] = dst & 0xff;
      if (word) {
        memory[dstAddress + 1] = (dst >> 8) & 0xff;
        if ((dstAddress & 1) != 0) {
          printWarning(MISALIGNED_WRITE, dstAddress);
        }
        if (mode > MODE_WORD) {
            memory[dstAddress + 2] = (dst >> 16) & 0xff; /* should be 0x0f ?? */
            memory[dstAddress + 3] = (dst >> 24) & 0xff; /* will be only zeroes*/
        }
      }
    }
    /* is a null check as fast as a boolean check */
    if (globalMonitor != null) {
        globalMonitor.cpuAction(CPUMonitor.MEMORY_WRITE, dstAddress, dst);
    }

  }

  void profileCall(int dst, int pc) {
      MapEntry function = map.getEntry(dst);
      if (function == null) {
          function = getFunction(map, dst);
      }
      profiler.profileCall(function, cpuCycles, pc);
  }
  
  void printWarning(int type, int address) throws EmulationException {
    String message = null;
    switch(type) {
    case MISALIGNED_READ:
      message = "**** Illegal read - misaligned word from $" +
      getAddressAsString(address) + " at $" + getAddressAsString(reg[PC]);
      break;
    case MISALIGNED_WRITE:
      message = "**** Illegal write - misaligned word to $" +
      getAddressAsString(address) + " at $" + getAddressAsString(reg[PC]);
      break;
    case ADDRESS_OUT_OF_BOUNDS_READ:
        message = "**** Illegal read - out of bounds from $" +
        getAddressAsString(address) + " at $" + getAddressAsString(reg[PC]);
        break;
    case ADDRESS_OUT_OF_BOUNDS_WRITE:
        message = "**** Illegal write -  out of bounds from $" +
        getAddressAsString(address) + " at $" + getAddressAsString(reg[PC]);
        
        break;
    }
    if (logger != null && message != null) {
      logger.warning(this, message);
    }
  }

  public void generateTrace(PrintStream out) {
    /* Override if a stack trace or other additional warning info should
     * be printed */ 
  }

  private int serviceInterrupt(int pc) {
    int pcBefore = pc;
    int spBefore = readRegister(SP);
    int sp = spBefore;
    int sr = readRegister(SR);
    
    if (profiler != null) {
      profiler.profileInterrupt(interruptMax, cycles);
    }
        
    if (flash.blocksCPU()) {
      /* TODO: how should this error/warning be handled ?? */
      throw new IllegalStateException(
          "Got interrupt while flash controller blocks CPU. CPU CRASHED.");
    }
    
    // Only store stuff on irq except reset... - not sure if this is correct...
    // TODO: Check what to do if reset is called!
    if (interruptMax < MAX_INTERRUPT) {
      // Push PC and SR to stack
      // store on stack - always move 2 steps (W) even if B.
      writeRegister(SP, sp = spBefore - 2);
      write(sp, pc, MODE_WORD);

      writeRegister(SP, sp = sp - 2);
      write(sp, (sr & 0x0fff) | ((pc & 0xf0000) >> 4), MODE_WORD);
    }
    // Clear SR
    writeRegister(SR, 0); // sr & ~CPUOFF & ~SCG1 & ~OSCOFF);

    // Jump to the address specified in the interrupt vector
    writeRegister(PC, pc = read(0xfffe - (MAX_INTERRUPT - interruptMax) * 2, MODE_WORD));

    servicedInterrupt = interruptMax;
    servicedInterruptUnit = interruptSource[servicedInterrupt];

    // Flag off this interrupt - for now - as soon as RETI is
    // executed things might change!
    reevaluateInterrupts();
    
    if (servicedInterrupt == MAX_INTERRUPT) {
        if (debugInterrupts) System.out.println("**** Servicing RESET! => $" + getAddressAsString(pc));
        internalReset();
    }
    
    
    // Interrupts take 6 cycles!
    cycles += 6;

    if (debugInterrupts) {
      System.out.println("### Executing interrupt: " +
			 servicedInterrupt + " at "
			 + pcBefore + " to " + pc +
			 " SP before: " + spBefore +
			 " Vector: " + Utils.hex16(0xfffe - (MAX_INTERRUPT - servicedInterrupt) * 2));
    }
    
    // And call the serviced routine (which can cause another interrupt)
    if (servicedInterruptUnit != null) {
      if (debugInterrupts) {
        System.out.println("### Calling serviced interrupt on: " +
                           servicedInterruptUnit.getName());
      }
      servicedInterruptUnit.interruptServiced(servicedInterrupt);
    }
    return pc;
  }

  /* returns true if any instruction was emulated - false if CpuOff */
  public int emulateOP(long maxCycles) throws EmulationException {
    //System.out.println("CYCLES BEFORE: " + cycles);
    int pc = readRegister(PC);
    long startCycles = cycles;
    
    // -------------------------------------------------------------------
    // Interrupt processing [after the last instruction was executed]
    // -------------------------------------------------------------------
    if (interruptsEnabled && servicedInterrupt == -1 && interruptMax >= 0) {
      pc = serviceInterrupt(pc);
    }

    /* Did not execute any instructions */
    if (cpuOff || flash.blocksCPU()) {
      //       System.out.println("Jumping: " + (nextIOTickCycles - cycles));
      // nextEventCycles must exist, otherwise CPU can not wake up!?

      // If CPU is not active we must run the events here!!!
      // this can trigger interrupts that wake the CPU
      // -------------------------------------------------------------------
      // Event processing - note: This can trigger IRQs!
      // -------------------------------------------------------------------
      /* This can flag an interrupt! */
      while (cycles >= nextEventCycles) {
        executeEvents();
      }

      if (interruptsEnabled && interruptMax > 0) {
          /* can not allow for jumping to nextEventCycles since that would jump too far */
          return -1;
      }

      if (maxCycles >= 0 && maxCycles < nextEventCycles) {
        // Should it just freeze or take on extra cycle step if cycles > max?
        cycles = cycles < maxCycles ? maxCycles : cycles;
      } else {
        cycles = nextEventCycles;
      }
      return -1;
    }

    // This is quite costly... should probably be made more
    // efficiently
    if (breakPoints[pc] != null) {
      if (breakpointActive) {
	breakPoints[pc].cpuAction(CPUMonitor.EXECUTE, pc, 0);
	breakpointActive = false;
	return -1;
      }
      // Execute this instruction - this is second call...
      breakpointActive = true;
    }
    if (globalMonitor != null) {
        globalMonitor.cpuAction(CPUMonitor.EXECUTE, pc, 0);
    }

    int pcBefore = pc;
    instruction = read(pc, MODE_WORD);
    int ext3_0 = 0;
    /* check for extension words */
    if ((instruction & 0xf800) == 0x1800) {
        extWord = instruction;
        ext3_0 = instruction & 0xf; /* bit 3 - 0 - either repeat count or dest 19-16 */
        pc += 2;
        instruction = read(pc, MODE_WORD);
//        System.out.println("*** Extension word!!! " + Utils.hex16(extWord) +
//                "  read the instruction too: " + Utils.hex16(instruction) + " at " + Utils.hex16(pc - 2));
    } else {
        extWord = 0;
    }
    
    op = instruction >> 12;
    int sp = 0;
    int sr = 0;
    int rval = 0; /* register value */
    int repeats = 1; /* msp430X can repeat some instructions in some cases */
    boolean zeroCarry = false; /* msp430X can zero carry in repeats */
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
    case 0:
        // MSP430X - additional instructions
        op = instruction & 0xf0f0;
//        System.out.println("Executing MSP430X instruction op:" + Utils.hex16(op) +
//                " ins:" + Utils.hex16(instruction) + " PC = $" + getAddressAsString(pc - 2));
        int src = 0;
        /* data is either bit 19-16 or src register */
        int srcData = (instruction & 0x0f00) >> 8;
        int dstData = (instruction & 0x000f);
        boolean rrword = true;
        switch(op) {
        // 20 bit register write
        case MOVA_IMM2REG:
            src = read(pc, MODE_WORD);
            writeRegister(PC, pc += 2);
            dst = src + (srcData << 16);
//            System.out.println("*** Writing $" + getAddressAsString(dst) + " to reg: " + dstData);
            writeRegister(dstData, dst);
            updateStatus = false;
            break;
        case MOVA_ABS2REG:
            src = read(pc, MODE_WORD);
            writeRegister(PC, pc += 2);
            dst = src + (srcData << 16);
            //System.out.println(Utils.hex20(pc) + " MOVA &ABS Reading from $" + getAddressAsString(dst) + " to reg: " + dstData);
            dst = read(dst, MODE_WORD20);
            //System.out.println("   => $" + getAddressAsString(dst));
            writeRegister(dstData, dst);
            updateStatus = false;
            break;
        case MOVA_IND_AUTOINC:
            if (profiler != null && instruction == 0x0110) {
                profiler.profileReturn(cpuCycles);
            }
            writeRegister(PC, pc);
            /* read from address in register */
            src = readRegister(srcData);
//            System.out.println("Reading $" + getAddressAsString(src) +
//                    " from register: " + srcData);
            dst = read(src, MODE_WORD20);
//            System.out.println("Reading from mem: $" + getAddressAsString(dst));
            writeRegister(srcData, src + 4);
//            System.out.println("*** Writing $" + getAddressAsString(dst) + " to reg: " + dstData);
            writeRegister(dstData, dst);
            updateStatus = false;
            break;
//        case CMPA_IMM:
//            break;
//        case CMPA_REG:
//            break;
//      case ADDA_IMM - TODO - make both ADDA use the same code.
        case ADDA_REG:
            src = readRegister(srcData);
            dst = readRegister(dstData);
            
            int tmp = (src ^ dst) & 0x80000;
            
            dst = src + dst;
            int nxtCarry = (dst & 0x100000) > 0 ? CARRY : 0; /* bit 20 */
            dst &= 0xfffff;
                        
            writeRegister(dstData, dst);
            sr = readRegister(SR);
            sr = sr & ~(NEGATIVE | OVERFLOW | CARRY | ZERO);
            
            // If tmp == 0 and currenly not the same sign for src & dst
            if (tmp == 0 && ((src ^ dst) & 0x80000) != 0) {
                sr |= OVERFLOW;
                //        System.out.println("OVERFLOW - ADD/SUB " + Utils.hex16(src)
                //                           + " + " + Utils.hex16(tmpDst));
            }
            
            sr = sr | nxtCarry | (dst == 0 ? ZERO : 0) | ((dst & 0x80000) > 0 ? NEGATIVE : 0);
            writeRegister(SR, sr);
            updateStatus = false;
            break;
        case RRXX_ADDR:
            rrword = false;
        case RRXX_WORD:
            int count = 1 + (instruction >> 10)& 0x03;
            dst = readRegister(dstData);
            nxtCarry = 0;
            if (rrword) {
                dst = dst & 0xffff;
            }
            switch(instruction & RRMASK) {
            /* if word zero anything above */
            case RRCM:
                System.out.println("*** RRCM!!! not implemented");
                throw new EmulationException("**** RRCM!! not implemented");
//                break;
            case RRAM:
//                System.out.println("RRAM executing");
                /* roll in MSB from above */
                /* 1 11 111 1111 needs to get in if MSB is 1 */
                if ((dst & (rrword ? 0x8000 : 0x80000)) > 0) {
                    /* add some 1 bits above MSB if MSB is 1 */
                    dst = dst | (rrword ? 0xf8000 : 0xf80000);
                }
                dst = dst >> (count - 1);
                nxtCarry = (dst & 1) > 0 ? CARRY : 0;
                dst = dst >> 1;
                break;
            case RLAM:
                /* just roll in "zeroes" from left */
                dst = dst << (count - 1);
                nxtCarry = (dst & (rrword ? 0x8000 : 0x80000)) > 0 ? CARRY : 0;
                dst = dst << 1;
                break;
            case RRUM:
                /* just roll in "zeroes" from right */
                dst = dst >> (count - 1);
                nxtCarry = (dst & 1) > 0 ? CARRY : 0;
                dst = dst >> 1;
                break;
            }
            /* clear overflow - set carry according to above OP */
            writeRegister(SR, (readRegister(SR) & ~(CARRY | OVERFLOW)) | nxtCarry);
            dst = dst & (rrword ? 0xffff : 0xfffff);
            writeRegister(dstData, dst);
            break;
        default:
            System.out.println("MSP430X instructions not yet supported: " +
                    Utils.hex16(instruction));
            throw new EmulationException("MSP430X instructions not yet supported...");
        }
        
        break;
    case 1:
    {
      // -------------------------------------------------------------------
      //  Single operand instructions
      // -------------------------------------------------------------------

      // Register
      dstRegister = instruction & 0xf;
      
      /* check if this is a MSP430X CALLA instruction */
      if ((op = instruction & CALLA_MASK) > RETI) {
          pc = readRegister(PC);
          dst = -1; /* will be -1 if not a call! */
          /* do not update status after these instructions!!! */
          updateStatus = false;
          switch(op) {
          case CALLA_IMM:
              dst = (dstRegister << 16) | read(pc, MODE_WORD);
              pc += 2;
              cycles += 4;
              break;
          case CALLA_ABS:
              /* read the address of where the address to call is */
              dst = (dstRegister << 16) | read(pc, MODE_WORD);
              dst = read(dst, MODE_WORD20);
              pc += 2;
              cycles += 4;
              break;
          default:
              int type = MODE_WORD;
              int size = 2;
              sp = readRegister(SP);
              /* check for PUSHM... POPM... */
              switch(op & 0x1f00) {
              case PUSHM_A:
                  type = MODE_WORD20;
                  size = 4;
              case PUSHM_W:
                  int n = 1 + ((instruction >> 4) & 0x0f);
                  int regNo = instruction & 0x0f;

//                  System.out.println("PUSHM " + (type == MODE_WORD20 ? "A" : "W") +
//                          " n: " + n + " " + regNo + " at " + Utils.hex16(pcBefore));

                  /* decrease stack pointer and write n times */
                  for(int i = 0; i < n; i++) {
                      sp -= size;
                      write(sp, this.reg[regNo--], type);
//                      System.out.println("Saved reg: " + (regNo + 1) + " was " + reg[regNo + 1]);

                      /* what happens if regNo is wrapped ??? */
                      if (regNo < 0) regNo = 15;
                  }
                  writeRegister(SP, sp);
                  break;
              case POPM_A:
                  type = MODE_WORD20;
                  size = 4;
              case POPM_W:
                  n = 1 + ((instruction >> 4) & 0x0f);
                  regNo = instruction & 0x0f;
//                  System.out.println("POPM W " + (type == MODE_WORD20 ? "A" : "W") + " n: " +
//                          n + " " + regNo + " at " + Utils.hex16(pcBefore));

                  /* read and increase stack pointer n times */
                  for(int i = 0; i < n; i++) {
                      this.reg[regNo++] = read(sp, type);
//                      System.out.println("Restored reg: " + (regNo - 1) + " to " + reg[regNo - 1]);
                      sp += size;
                      /* what happens if regNo is wrapped ??? */
                      if (regNo > 15) regNo = 0;
                  }

                  writeRegister(SP, sp);
                  break;
                  default:
                  System.out.println("CALLA/PUSH/POP: mode not implemented");
                  throw new EmulationException("CALLA: mode not implemented "
                          + Utils.hex16(instruction) + " => " + Utils.hex16(op));
              }
          }
          // store current PC on stack. (current PC points to next instr.)
          /* store 20 bits on stack (costs two words) */
          if (dst != -1) {
              sp = readRegister(SP) - 2;
              write(sp, (pc >> 16) & 0xf, MODE_WORD);
              sp = sp - 2;
              write(sp, pc & 0xffff, MODE_WORD);
              writeRegister(SP, sp);
              writeRegister(PC, dst);
              
              if (profiler != null) {
                  profileCall(dst, pc);
              }
          }
      } else {
          // Address mode of destination...
          int ad = (instruction >> 4) & 3;
          int nxtCarry = 0;
          op = instruction & 0xff80;
          if (op == PUSH || op == CALL) {
              // The PUSH and CALL operations increase the SP before 
              // address resolution!
              // store on stack - always move 2 steps (W) even if B./
              sp = readRegister(SP) - 2;
              writeRegister(SP, sp);
          }

          if ((dstRegister == CG1 && ad > AM_INDEX) || dstRegister == CG2) {
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
                  // TODO: needs to handle if SR is used!
                  rval = readRegisterCG(dstRegister, ad);

                  /* Support for MSP430X and below / above 64 KB */
                  /* if register is pointing to <64KB then it needs to be truncated to below 64 */
                  if (rval < 0xffff) {
                      dstAddress = (rval + read(pc, MODE_WORD)) & 0xffff;
                  } else {
                      dstAddress = read(pc, MODE_WORD);
                      if ((dstAddress & 0x8000) > 0) {
                          dstAddress |= 0xf0000;
                      }
                      dstAddress += rval;
                      dstAddress &= 0xfffff;
                  }

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
                  // Bugfix suggested by Matt Thompson
              case AM_IND_AUTOINC:
                  if(dstRegister == PC) {
                      dstAddress = readRegister(PC);
                      pc += 2;
                      writeRegister(PC, pc);
                  } else {
                      dstAddress = readRegister(dstRegister);
                      writeRegister(dstRegister, dstAddress + (word ? 2 : 1));
                  }
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
              /* set the repeat here! */
              if ((extWord & EXTWORD_REPEAT) > 0) {
                  repeats = 1 + readRegister(ext3_0) & 0xf;
              } else {
                  repeats = 1 + ext3_0;                  
              }
              zeroCarry = (extWord & EXTWORD_ZC) > 0;

//              if (repeats > 1) {
//                  System.out.println("*** Repeat " + repeats + " ZeroCarry: " + zeroCarry);
//              }
          } else {
              dst = read(dstAddress, word ? MODE_WORD : MODE_BYTE);
          }
          
          /* TODO: test add the loop here! */
          while(repeats-- > 0) {
              sr = readRegister(SR);
              /* always clear carry before repeat */
              if (repeats >= 0) {
                  if (zeroCarry) {
                      sr = sr & ~CARRY;
                      //System.out.println("ZC => Cleared carry...");
                  }
                  //System.out.println("*** Repeat: " + repeats);
              }
              switch(op) {
              case RRC:
                  nxtCarry = (dst & 1) > 0 ? CARRY : 0;
                  dst = dst >> 1;
                  if (word) {
                      dst |= (sr & CARRY) > 0 ? 0x8000 : 0;
                  } else {
                      dst |= (sr & CARRY) > 0 ? 0x80 : 0;
                  }
                  // Indicate write to memory!!
                  write = true;
                  // Set the next carry!
                  writeRegister(SR, (sr & ~(CARRY | OVERFLOW)) | nxtCarry);
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
                  writeRegister(SR, (sr & ~(CARRY | OVERFLOW)) | nxtCarry);
                  break;
              case SXT:
                  // Extend Sign (bit 8-15 => same as bit 7)
                  dst = (dst & 0x80) > 0 ? dst | 0xff00 : dst & 0x7f;
                  write = true;
                  sr = sr & ~(CARRY | OVERFLOW);
                  if (dst != 0) {
                      sr |= CARRY;
                  }
                  writeRegister(SR, sr);
                  break;
              case PUSH:
                  if (word) {
                      // Put lo & hi on stack!
                      //	  memory[sp] = dst & 0xff;
                      //	  memory[sp + 1] = dst >> 8;
                      write(sp, dst, MODE_WORD);
                  } else {
                      // Byte => only lo byte
                      //	  memory[sp] = dst & 0xff;
                      //	  memory[sp + 1] = 0;
                      write(sp, dst & 0xff, MODE_WORD);
                  }
                  /* if REG or INDIRECT AUTOINC then add 2 cycles, otherwise 1 */
                  cycles += (ad == AM_REG || ad == AM_IND_AUTOINC) ? 2 : 1;
                  write = false;
                  updateStatus = false;
                  break;
              case CALL:
                  // store current PC on stack. (current PC points to next instr.)
                  pc = readRegister(PC);
                  //	memory[sp] = pc & 0xff;
                  //	memory[sp + 1] = pc >> 8;
                  write(sp, pc, MODE_WORD);
                  writeRegister(PC, dst);

                  /* Additional cycles: REG => 3, AM_IND_AUTO => 2, other => 1 */
                  cycles += (ad == AM_REG) ? 3 : (ad == AM_IND_AUTOINC) ? 2 : 1;

                  /* profiler will be called during calls */
                  if (profiler != null) {
                      profileCall(dst, pc);
                  }

                  write = false;
                  updateStatus = false;
                  break;
              case RETI:
                  // Put Top of stack to Status DstRegister (TOS -> SR)
                  servicedInterrupt = -1; /* needed before write to SR!!! */
                  sp = readRegister(SP);
                  sr = read(sp, MODE_WORD);
                  writeRegister(SR, sr & 0x0fff);
                  sp = sp + 2;
                  //	writeRegister(SR, memory[sp++] + (memory[sp++] << 8));
                  // TOS -> PC
                  //	writeRegister(PC, memory[sp++] + (memory[sp++] << 8));
                  writeRegister(PC, read(sp, MODE_WORD) | (sr & 0xf000) << 4);
                  sp = sp + 2;
                  writeRegister(SP, sp);
                  write = false;
                  updateStatus = false;

                  cycles += 4;

                  if (debugInterrupts) {
                      System.out.println("### RETI at " + pc + " => " + reg[PC] +
                              " SP after: " + reg[SP]);
                  }        
                  if (profiler != null) {
                      profiler.profileRETI(cycles);
                  }

                  // This assumes that all interrupts will get back using RETI!
                  handlePendingInterrupts();

                  break;
              default:
                  System.out.println("Error: Not implemented instruction:" +
                          Utils.hex16(instruction));
              }
              if (repeats > 0) {
                  if (!word) {
                      dst &= 0xff;
                  } else {
                      dst &= 0xffff;
                  }
              }
          }
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

      // All jump takes two cycles
      cycles += 2;
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
        logw("Not implemented instruction: #" + Utils.binary16(instruction));
      }
      // Perform the Jump
      if (jump) {
        writeRegister(PC, pc + jmpOffset);
      }
      updateStatus = false;
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
      src = 0;

      // Some CGs should be handled as registry reads only...
      if ((srcRegister == CG1 && as > AM_INDEX) || srcRegister == CG2) {
        src = CREG_VALUES[srcRegister - 2][as];
        if (!word) {
          src &= 0xff;
        }
        cycles += dstRegMode ? 1 : 4;
      } else {
	switch(as) {
	  // Operand in register!
	case AM_REG:
	  // CG handled above!
	  src = readRegister(srcRegister);
	  if (!word) {
	    src &= 0xff;
	  }
	  cycles += dstRegMode ? 1 : 4;
	  /* add cycle if destination register = PC */
          if (dstRegister == PC) cycles++;
          
          if (dstRegMode) {
              /* possible to have repeat, etc... */
              /* TODO: decode the # also */
              if ((extWord & EXTWORD_REPEAT) > 0) {
                  repeats = 1 + readRegister(ext3_0) & 0xf;
              } else {
                  repeats = 1 + ext3_0;                  
              }
              zeroCarry = (extWord & EXTWORD_ZC) > 0;
          }
          
	  break;
	case AM_INDEX:
	  // Indexed if reg != PC & CG1/CG2 - will PC be incremented?
	  srcAddress = readRegisterCG(srcRegister, as) + read(pc, MODE_WORD);
//	    memory[pc] + (memory[pc + 1] << 8);
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
	    /* PC is always handled as word */
	    srcAddress = readRegister(PC);
	    pc += 2;
	    incRegister(PC, 2);
            cycles += dstRegMode ? 2 : 5;
	  } else {
	    srcAddress = readRegister(srcRegister);
	    incRegister(srcRegister, word ? 2 : 1);
	    cycles += dstRegMode ? 2 : 5;
	  }
	  /* If destination register is PC another cycle is consumed */
	  if (dstRegister == PC) {
	    cycles++;
	  }
	  break;
	}
      }

      // Perform the read of destination!
      if (dstRegMode) {
        if (op != MOV) {
          dst = readRegister(dstRegister);
          if (!word) {
            dst &= 0xff;
          }
        }
      } else {
        // PC Could have changed above!
        pc = readRegister(PC);
        if (dstRegister == 2) {
          /* absolute mode */
          dstAddress = read(pc, MODE_WORD); //memory[pc] + (memory[pc + 1] << 8);
        } else {
          // CG here - probably not!???
          rval = readRegister(dstRegister);
          /* Support for MSP430X and below / above 64 KB */
          /* if register is pointing to <64KB then it needs to be truncated to below 64 */
          if (rval < 0xffff) {
              dstAddress = (rval + read(pc, MODE_WORD)) & 0xffff;
          } else {
              dstAddress = read(pc, MODE_WORD);
              if ((dstAddress & 0x8000) > 0) {
                  dstAddress |= 0xf0000;
              }
              dstAddress += rval;
              dstAddress &= 0xfffff;
          }
        }

        if (op != MOV)
          dst = read(dstAddress, word ? MODE_WORD : MODE_BYTE);
        pc += 2;
        incRegister(PC, 2);
      }

      // **** Perform the read...
      if (srcAddress != -1) {

//        if (srcAddress  > 0xffff) {
//            System.out.println("SrcAddress is: " + Utils.hex20(srcAddress));
//        }
//	srcAddress = srcAddress & 0xffff;

	src = read(srcAddress, word ? MODE_WORD : MODE_BYTE);

	// 	  if (debug) {
	// 	    System.out.println("Reading from " + getAddressAsString(srcAddress) +
	// 			       " => " + src);
	// 	  }
      }

      /* TODO: test add the loop here! */
      while(repeats-- > 0) {
          sr = readRegister(SR);
          if (repeats >= 0) {
              if (zeroCarry) {
                  sr = sr & ~CARRY;
                  //System.out.println("ZC => Cleared carry...");
              }
              //System.out.println("*** Repeat: " + repeats);
          }

          int tmp = 0;
          int tmpAdd = 0;
          switch (op) {
          case MOV: // MOV
              dst = src;
              write = true;
              updateStatus = false;

              if (instruction == RETURN && profiler != null) {
                  profiler.profileReturn(cpuCycles);
              }

              break;
              // FIX THIS!!! - make SUB a separate operation so that
              // it is clear that overflow flag is correct...
          case SUB:
              // Carry always 1 with SUB
              tmpAdd = 1;
          case SUBC:
              // Both sub and subc does one complement (not) + 1 (or carry)
              src = (src ^ 0xffff) & 0xffff;
          case ADDC: // ADDC
              if (op == ADDC || op == SUBC)
                  tmpAdd = ((sr & CARRY) > 0) ? 1 : 0;
          case ADD: // ADD
              // Tmp gives zero if same sign! if sign is different after -> overf.
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
              sr = (sr & ~(CARRY | OVERFLOW)) | (dst >= src ? CARRY : 0);

              tmp = (dst - src);

              if (((src ^ tmp) & b) == 0 && (((src ^ dst) & b) != 0)) {
                  sr |= OVERFLOW;
              }
              writeRegister(SR, sr);
              // Must set dst to the result to set the rest of the status register
              dst = tmp;
              break;
          case DADD: // DADD
              if (DEBUG)
                  log("DADD: Decimal add executed - result error!!!");
              // Decimal add... this is wrong... each nibble is 0-9...
              // So this has to be reimplemented...
              dst = dst + src + ((sr & CARRY) > 0 ? 1 : 0);
              write = true;
              break;
          case BIT: // BIT
              dst = src & dst;
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
              sr = sr & ~(CARRY | OVERFLOW);
              if ((src & (word ? 0x8000 : 0x80)) != 0 && (dst & (word ? 0x8000 : 0x80)) != 0) {
                  sr |= OVERFLOW;
              }
              dst = src ^ dst;
              if (dst != 0) {
                  sr |= CARRY;
              }
              write = true;
              writeRegister(SR, sr);
              break;
          case AND: // AND
              sr = sr & ~(CARRY | OVERFLOW);
              dst = src & dst;
              if (dst != 0) {
                  sr |= CARRY;
              }
              write = true;
              writeRegister(SR, sr);
              break;
          default:
              logw("DoubleOperand not implemented: op = " + op + " at " + pc);
              if (EXCEPTION_ON_BAD_OPERATION) {
                  EmulationException ex = new EmulationException("Bad operation: " + op + " at " + pc);
                  ex.initCause(new Throwable("" + pc));
                  throw ex;
              }
          } /* after switch(op) */
          /* If we have the same register as dst and src then copy here to get input
           * in next loop
           */
          if(repeats > 0 && srcRegister == dstRegister) {
              src = dst;
              if (!word) {
                  src &= 0xff;
              } else {
                  src &= 0xffff;
              }
          }
      }
    }
    
    /* Processing after each instruction */
    if (word) {
      dst &= 0xffff;
    } else {
      dst &= 0xff;
    }
    if (write) {
      if (dstRegMode) {
	writeRegister(dstRegister, dst);
      } else {
	dstAddress &= 0xffff;
	write(dstAddress, dst, word ? MODE_WORD : MODE_BYTE);
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

    //System.out.println("CYCLES AFTER: " + cycles);

    // -------------------------------------------------------------------
    // Event processing (when CPU is awake)
    // -------------------------------------------------------------------
    while (cycles >= nextEventCycles) {
      executeEvents();
    }
    
    cpuCycles += cycles - startCycles;
    
    /* return the address that was executed */
    return pcBefore;
  }
  
  public int getModeMax() {
    return MODE_MAX;
  }

  MapEntry getFunction(MapTable map, int address) {
    MapEntry function = new MapEntry(MapEntry.TYPE.function, address, 0,
        "fkn at $" + getAddressAsString(address), null, true);
    map.setEntry(function);
    return function;
  }

  public int getPC() {
    return reg[PC];
  }

  public String getAddressAsString(int addr) {
      return config.getAddressAsString(addr);
  }

  public int getConfiguration(int parameter) {
      return 0;
  }

  public String info() {
      StringBuilder buf = new StringBuilder();
      buf.append(" Mode: " + getModeName(getMode())
              + "  ACLK: " + aclkFrq + " Hz  SMCLK: " + smclkFrq + " Hz\n"
              + " Cycles: " + cycles + "  CPU Cycles: " + cpuCycles
              + "  Time: " + (long)getTimeMillis() + " msec\n");
      buf.append(" Interrupt enabled: " + interruptsEnabled +  " HighestInterrupt: " + interruptMax);
      for (int i = 0; i < MAX_INTERRUPT; i++) {
          if (read(0xfffe - i * 2, MODE_WORD) != 0xffff) {
              buf.append(" Vector " + (MAX_INTERRUPT - i) + " at $"
                      + Utils.hex16(0xfffe - i * 2) + " -> $"
                      + Utils.hex16(read(0xfffe - i * 2, MODE_WORD)) + "\n");
          }
      }
      return buf.toString();
  }
}
