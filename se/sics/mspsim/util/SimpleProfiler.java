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
 * MSP430
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.util;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.Profiler;
import java.util.Arrays;
import java.util.Hashtable;


public class SimpleProfiler implements Profiler {

  private Hashtable<MapEntry,CallEntry> profileData;
  private CallEntry[] callStack;
  private int cSP = 0;
  private MSP430Core cpu;

  public SimpleProfiler() {
    profileData = new Hashtable<MapEntry, CallEntry>();
    callStack = new CallEntry[2048];
  }

  public void setCPU(MSP430Core cpu) {
    this.cpu = cpu;
  }
  
  public void profileCall(MapEntry entry, long cycles) {
//  System.out.println("Call at: " + Utils.hex16(reg[PC]));
    if (callStack[cSP] == null) {
      callStack[cSP] = new CallEntry();
    }
    callStack[cSP].function = entry;
    callStack[cSP].calls = 0;
    callStack[cSP++].cycles = cycles;
  }

  public void profileReturn(long cycles) {
    MapEntry fkn = callStack[--cSP].function;
//     System.out.println("Profiler: return / call stack: " + cSP + ", " + fkn);

    long elapsed = cycles - callStack[cSP].cycles;
    if (callStack[cSP].calls >= 0) {
      CallEntry ce = profileData.get(fkn);
      if (ce == null) {
	profileData.put(fkn, ce = new CallEntry());
	ce.function = fkn;
      }
      ce.cycles += elapsed;
      ce.calls++;
    }
  }

  public void clearProfile() {
    if (profileData != null) {
      CallEntry[] entries =
        profileData.values().toArray(new CallEntry[0]);
      for (int i = 0, n = entries.length; i < n; i++) {
        entries[i].cycles = 0;
        entries[i].calls = 0;
      }
      for (int i = 0, n = callStack.length; i < n; i++) {
        CallEntry e = callStack[i];
        if (e != null) {
          e.calls = -1;
        }
      }
    }
  }

  public void printProfile() {
    CallEntry[] entries =
      profileData.values().toArray(new CallEntry[0]);
    Arrays.sort(entries);

    System.out.println("************************* Profile Data **************************************");
    System.out.println("Function                                         Average    Calls  Tot.Cycles");


    for (int i = 0, n = entries.length; i < n; i++) {
      int c = entries[i].calls;
      if (c > 0) {
	String cyclesS = "" + entries[i].cycles;
	String callS = "" + c;
	String avgS = "" + (c > 0 ? (entries[i].cycles / c) : 0);
	System.out.print(entries[i].function.getName());
	printSpace(56 - entries[i].function.getName().length() - avgS.length());
	System.out.print(avgS);
	System.out.print(' ');
	printSpace(8 - callS.length());
	System.out.print(callS);
	System.out.print(' ');
	printSpace(10 - cyclesS.length());
	System.out.println(cyclesS);
      }
    }
  }

  private void printSpace(int len) {
    for (int i = 0; i < len; i++) {
      System.out.print(' ');
    }
  }

  public void printStackTrace() {
    System.out.println("Stack Trace: number of calls: " + cSP);
    for (int i = 0; i < cSP; i++) {
      System.out.println("  " + callStack[cSP - i - 1].function.getInfo());
    }
  }
  
  private static class CallEntry implements Comparable<CallEntry> {
    MapEntry function;
    long cycles;
    int calls;

    public int compareTo(CallEntry o) {
      long diff = o.cycles - cycles;
      if (diff > 0) return 1;
      if (diff < 0) return -1;
      return 0;
    }
  }
}
