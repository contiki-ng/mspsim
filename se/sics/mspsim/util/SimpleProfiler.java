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

package se.sics.mspsim.util;
import se.sics.mspsim.core.Profiler;
import java.util.Arrays;
import java.util.Hashtable;


public class SimpleProfiler implements Profiler {

  private Hashtable<String,CallEntry> profileData;
  private CallEntry[] callStack;
  private int cSP = 0;

  public SimpleProfiler() {
    profileData = new Hashtable<String,CallEntry>();
    callStack = new CallEntry[2048];
  }

  public void profileCall(String function, long cycles) {
//     System.out.println("Call at: " + Utils.hex16(reg[PC]));
    if (callStack[cSP] == null) {
      callStack[cSP] = new CallEntry();
    }
    callStack[cSP].function = function;
    callStack[cSP].calls = 0;
    callStack[cSP++].cycles = cycles;
  }

  public void profileReturn(long cycles) {
    String fkn = callStack[--cSP].function;
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
	System.out.print(entries[i].function);
	printSpace(56 - entries[i].function.length() - avgS.length());
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

  private static class CallEntry implements Comparable {
    String function;
    long cycles;
    int calls;

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
