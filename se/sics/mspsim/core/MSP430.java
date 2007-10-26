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
 * $Id: MSP430.java,v 1.4 2007/10/21 22:02:22 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * MSP430
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 22:02:22 $
 *           $Revision: 1.4 $
 */

package se.sics.mspsim.core;
import java.util.Arrays;
import java.util.Hashtable;

import se.sics.mspsim.util.Utils;

public class MSP430 extends MSP430Core {

  public static final int RETURN = 0x4130;

  private int[] execCounter;

  private boolean debug = false;
  private boolean running = false;

  // Debug time - measure cycles
  private long lastCycles = 0;
  private long time;

  private long instCtr = 0;

  private DisAsm disAsm;

  private MapTable map;
  private Hashtable<String,CallEntry> profileData;
  private CallEntry[] callStack;
  private int cSP = 0;

  /**
   * Creates a new <code>MSP430</code> instance.
   *
   */
  public MSP430(int type) {
    super(type);
    disAsm = new DisAsm();
  }

  public DisAsm getDisAsm() {
    return disAsm;
  }

  public void cpuloop() {
    if (running) {
      throw new IllegalStateException("already running");
    }
    running = true;
    // ??? - power-up reset should be executed?!
    time = System.currentTimeMillis();
    run();
  }

  private void run() {
    while (running) {
      // -------------------------------------------------------------------
      // Debug information
      // -------------------------------------------------------------------
      if (debug) {
	if (servicedInterrupt >= 0) {
	  disAsm.disassemble(reg[PC], memory, reg, servicedInterrupt);
	} else {
	  disAsm.disassemble(reg[PC], memory, reg);
	}
      }

      instCtr++;
      if ((instCtr % 10000007) == 0 && !debug) {
	printCPUSpeed(reg[PC]);
      }

      if (execCounter != null) {
	execCounter[reg[PC]]++;
      }

      emulateOP();

//       if ((instruction & 0xff80) == CALL) {
// 	System.out.println("Call to PC = " + reg[PC]);
//       }

      if (map != null) {
	if ((instruction & 0xff80) == CALL) {
	  profileCall(map.getFunction(reg[PC]), cycles);
	  // 	  System.out.println("Call," + map.getFunction(reg[PC]) + "," +
	  // 			     cycles);
	} else if (instruction == RETURN) {
	  profileReturn(cycles);
	  //System.out.println("Return," + cycles);
	}
      }
    }
  }

  private void profileCall(String function, long cycles) {
//     System.out.println("Call at: " + Utils.hex16(reg[PC]));
    if (callStack[cSP] == null) {
      callStack[cSP] = new CallEntry();
    }
    if (function == null) {
      function = "fkn at $" + Utils.hex16(reg[PC]);
    }
    callStack[cSP].function = function;
    callStack[cSP++].cycles = cycles;
  }

  private void profileReturn(long cycles) {
    String fkn = callStack[--cSP].function;
//     System.out.println("Profiler: return / call stack: " + cSP + ", " + fkn);

    long elapsed = cycles - callStack[cSP].cycles;
    CallEntry ce = profileData.get(fkn);
    if (ce == null) {
      profileData.put(fkn, ce = new CallEntry());
      ce.function = fkn;
      ce.cycles = elapsed;
    } else {
      ce.cycles += elapsed;
    }
  }

  public void printProfile() {
    CallEntry[] entries =
      profileData.values().toArray(new CallEntry[0]);
    Arrays.sort(entries);
    for (int i = 0, n = entries.length; i < n; i++) {
      printFkn(entries[i].function);
      System.out.println(" " + entries[i].cycles);
    }
  }

  public void printFkn(String f) {
    System.out.print(f);
    int len = f.length();
    if (len < 40)
      len = 40 - len;
    else
      len = 0;
    for (int i = 0, n = len; i < n; i++) {
      System.out.print(" ");
    }
  }

  public long step() {
    if (running) {
      throw new IllegalStateException("step not possible when CPU is running");
    }

    // -------------------------------------------------------------------
    // Debug information
    // -------------------------------------------------------------------
    if (debug) {
      if (servicedInterrupt >= 0) {
	disAsm.disassemble(reg[PC], memory, reg, servicedInterrupt);
      } else {
	disAsm.disassemble(reg[PC], memory, reg);
      }
    }

    instCtr++;
    if ((instCtr % 10000007) == 0 && !debug) {
      printCPUSpeed(reg[PC]);
    }

    if (execCounter != null) {
      execCounter[reg[PC]]++;
    }

    emulateOP();
    return cycles;
  }

  public void stop() {
    running = false;
  }

  public int getExecCount(int address) {
    if (execCounter != null) {
      return execCounter[address];
    }
    return 0;
  }

  public void setMonitorExec(boolean mon) {
    if (mon) {
      if (execCounter == null) {
	execCounter = new int[MAX_MEM];
      }
    } else {
      execCounter = null;
    }
  }

  private void printCPUSpeed(int pc) {
    int td = (int)(System.currentTimeMillis() - time);
    int cd = (int) (cycles - lastCycles);

    if (DEBUGGING_LEVEL > 0) {
      System.out.println("Time elapsed: " + td
          +  " cycDiff: " + cd
          + " => " + 1000 * (cd / td ) + " cycles / s");
    }
    time = System.currentTimeMillis();
    lastCycles = cycles;
    disAsm.disassemble(pc, memory, reg);
  }

  public boolean getDebug() {
    return debug;
  }

  public void setDebug(boolean db) {
    debug = db;
  }

  public void setMap(MapTable map) {
    if (profileData == null) {
      profileData = new Hashtable<String,CallEntry>();
      callStack = new CallEntry[2048];
    }
    this.map = map;
  }

  private static class CallEntry implements Comparable {
    String function;
    long cycles;

    public int compareTo(Object o) {
      if (o instanceof CallEntry) {
	long diff = ((CallEntry)o).cycles - cycles;
	if (diff > 0) return 1;
	if (diff < 0) return -1;
      }
      return 0;
    }
  }

}
