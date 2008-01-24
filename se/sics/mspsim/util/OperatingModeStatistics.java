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
  }
  
  public void modeChanged(Chip source, int mode) {
    StatEntry entry = statistics.get(source.getName());
    if (entry == null) {
      entry = new StatEntry(source.getName(), source.getModeMax());
      statistics.put(source.getName(), entry);
    }
    entry.updateStat(mode, cpu.cycles);
  }

  public void printStat() {    
    for (Iterator<StatEntry> iterator = statistics.values().iterator(); iterator.hasNext();) {
      StatEntry entry = iterator.next();
      entry.printStat();
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
