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
 * $Id: $
 *
 * -----------------------------------------------------------------
 *
 * OperatingModeStatistics
 *
 * Author  : Joakim Eriksson
 * Created : 17 jan 2008
 * Updated : $Date:$
 *           $Revision:$
 */
package se.sics.mspsim.util;

import java.util.HashMap;
import java.util.Iterator;

import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.OperatingModeListener;

/**
 * @author Joakim
 *
 */
public class OperatingModeStatistics implements OperatingModeListener {
  
  private MSP430Core cpu;
  private HashMap<String, StatEntry> statistics = new HashMap<String, StatEntry>();

  public OperatingModeStatistics(MSP430Core cpu) {
    this.cpu = cpu;
  }
  
  public void addMonitor(Chip chip) {
    chip.addOperatingModeListener(this);
    StatEntry entry = new StatEntry(chip.getName(), chip.getModeMax());
    statistics.put(chip.getName(), entry);    
  }
  
  public void modeChanged(Chip source, int mode) {
    StatEntry entry = statistics.get(source.getName());
    if (entry != null)
      entry.updateStat(mode, cpu.cycles);
  }

  public void printStat() {    
    for (Iterator<StatEntry> iterator = statistics.values().iterator(); iterator.hasNext();) {
      StatEntry entry = iterator.next();
      entry.printStat();
    }
  }
  
  public DataSource getDataSource(String chip, int mode) {
    StatEntry se = statistics.get(chip);
    if (se != null) {
      return new StatDataSource(se, mode);
    }
    return null;
  }

  public DataSource getMultiDataSource(String chip) {
    StatEntry se = statistics.get(chip);
    if (se != null) {
      return new StatMultiDataSource(se);
    }
    return null;  
  }
  
  private class StatDataSource implements DataSource {

    private StatEntry entry;
    private int mode;
    private long lastCycles;
    private long lastValue;
    
    public StatDataSource(StatEntry entry, int mode) {
      this.entry = entry;
      this.mode = mode;
      lastCycles = cpu.cycles;
    }
    
    // returns percentage since last call...
    public int getValue() {
      long diff = cpu.cycles - lastCycles;
      if (diff == 0) return 0;
      long val = entry.getValue(mode, cpu.cycles);
      long valDiff = val - lastValue;
      lastValue = val;
      lastCycles = cpu.cycles;
      return (int) (100 * valDiff / diff);
    }
  }

  private class StatMultiDataSource implements DataSource{

    private StatEntry entry;
    private long lastCycles;
    private long[] lastValue;
    
    public StatMultiDataSource(StatEntry entry) {
      this.entry = entry;
      lastCycles = cpu.cycles;
      lastValue = new long[entry.elapsed.length];
    }
    
    // returns percentage since last call...
    public int getValue() {
      long diff = cpu.cycles - lastCycles;
      if (diff == 0) return 0;

      long valDiff = 0;
      // Assume that 0 means "off"
      for (int i = 1; i < lastValue.length; i++) {
        // Just sum them - later a multiplicator array might be useful...
        long val = entry.getValue(i, cpu.cycles);
        valDiff += (val - lastValue[i]);
        lastValue[i] = val;
      }
      lastCycles = cpu.cycles;
      return (int) (100 * valDiff / diff);
    }
  }
  
  
  private class StatEntry {
    String key;
    long startTime;
    int mode = -1;
    long[] elapsed;
    
    StatEntry(String key, int max) {
      this.key = key;
      elapsed = new long[max + 1];
    }
    
    long getValue(int mode, long cycles) {
      if (mode == this.mode) {
        return elapsed[mode] + (cycles - startTime);
      }
      return elapsed[mode];
    }
    
    void updateStat(int mode, long cycles) {
      if (this.mode != -1) {
        elapsed[this.mode] += cycles - startTime;
      }
      this.mode = mode;
      startTime = cycles;
    }
    
    void printStat() {
      System.out.println("Stat for: " + key);
      for (int i = 0; i < elapsed.length; i++) {
        System.out.println("" + (i + 1) + " = " + elapsed[i]);
      }
    }
  }
  
}
