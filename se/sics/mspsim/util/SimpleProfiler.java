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
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.EventListener;
import se.sics.mspsim.core.EventSource;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.Profiler;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.regex.Pattern;


public class SimpleProfiler implements Profiler, EventListener {

  private Hashtable<MapEntry,CallEntry> profileData;
  private Hashtable<String, TagEntry> tagProfiles;
  private Hashtable<String, TagEntry> startTags;
  private Hashtable<String, TagEntry> endTags;
  private CallEntry[] callStack;
  private int cSP = 0;
  private MSP430Core cpu;
  private PrintStream logger;

  public SimpleProfiler() {
    profileData = new Hashtable<MapEntry, CallEntry>();
    tagProfiles = new Hashtable<String, TagEntry>();
    startTags = new Hashtable<String, TagEntry>();
    endTags = new Hashtable<String, TagEntry>();
    callStack = new CallEntry[2048];
  }

  public void setCPU(MSP430Core cpu) {
    this.cpu = cpu;
  }

  public void profileCall(MapEntry entry, long cycles) {
    if (callStack[cSP] == null) {
      callStack[cSP] = new CallEntry();
    }

    if (logger != null) {
      printSpace(logger, cSP * 2);
      logger.println("Call to: " + entry);
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
        profileData.values().toArray(new CallEntry[profileData.size()]);
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
  
  public void printProfile(PrintStream out) {
    printProfile(out, null);
  }

  public void printProfile(PrintStream out, String functionNameRegexp) {
    Pattern pattern = null;
    CallEntry[] entries = profileData.values().toArray(new CallEntry[profileData.size()]);
    Arrays.sort(entries);

    out.println("************************* Profile Data **************************************");
    out.println("Function                                         Average    Calls  Tot.Cycles");

    if (functionNameRegexp != null && functionNameRegexp.length() > 0) {
      pattern = Pattern.compile(functionNameRegexp);
    }
    for (int i = 0, n = entries.length; i < n; i++) {
      int c = entries[i].calls;
      if (c > 0) {
        String functionName = entries[i].function.getName();
        if (pattern == null || pattern.matcher(functionName).find()) {
          String cyclesS = "" + entries[i].cycles;
          String callS = "" + c;
          String avgS = "" + (c > 0 ? (entries[i].cycles / c) : 0);
          out.print(functionName);
          printSpace(out, 56 - functionName.length() - avgS.length());
          out.print(avgS);
          out.print(' ');
          printSpace(out, 8 - callS.length());
          out.print(callS);
          out.print(' ');
          printSpace(out, 10 - cyclesS.length());
          out.println(cyclesS);
        }
      }
    }
  }

  private void printSpace(PrintStream out, int len) {
    for (int i = 0; i < len; i++) {
      out.print(' ');
    }
  }

  public void printStackTrace(PrintStream out) {
    out.println("Stack Trace: number of calls: " + cSP);
    for (int i = 0; i < cSP; i++) {
      out.println("  " + callStack[cSP - i - 1].function.getInfo());
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

  private static class TagEntry implements Comparable<TagEntry> {
    String tag;
    long cycles;
    long lastCycles;
    int calls;

    public int compareTo(TagEntry o) {
      long diff = o.cycles - cycles;
      if (diff > 0) return 1;
      if (diff < 0) return -1;
      return 0;
    }
  }

  
  
  public void setLogger(PrintStream out) {
    logger = out;
  }
  
  /* 
   * Tag profiling.
   */
  public void measureStart(String tag) {
    TagEntry tagEntry = tagProfiles.get(tag);
    if (tagEntry == null) {
      tagEntry = new TagEntry();
      tagEntry.tag = tag;
      tagProfiles.put(tag, tagEntry);
    }
    /* only the first occurrence of event will set the lastCycles */
    if (tagEntry.lastCycles == 0) {
      tagEntry.lastCycles = cpu.cycles;
    }
  }
  
  public void measureEnd(String tag) {
    TagEntry tagEntry = tagProfiles.get(tag);
    if (tagEntry != null) {
      if (tagEntry.lastCycles != 0) {
        tagEntry.calls++;
        tagEntry.cycles += cpu.cycles - tagEntry.lastCycles;
        tagEntry.lastCycles = 0;
      }
    }
  }
  
  public void printTagProfile(PrintStream out) {
    TagEntry[] entries = tagProfiles.values().toArray(new TagEntry[tagProfiles.size()]);
    Arrays.sort(entries);
    for (int i = 0; i < entries.length; i++) {
      TagEntry entry = entries[i];
      System.out.println(entry.tag + "\t" + entry.calls + "\t" + entry.cycles);
    }
  }

  public void addProfileTag(String tag, Chip chip, String start,
      Chip chip2, String end) {
    System.out.println("Add profile: " + tag +
        " start: " + start + " end: " + end);
    TagEntry tagEntry = new TagEntry();
    tagEntry.tag = tag;
    startTags.put(start, tagEntry);
    endTags.put(end, tagEntry);
    tagProfiles.put(tag, tagEntry);
    chip.setEventListener(this);
    chip2.setEventListener(this);
  }

  public void event(EventSource source, String event, Object data) {
    TagEntry tagEntry = null;
    if ((tagEntry = startTags.get(event)) != null) {
      /* only the first occurrence of event will set the lastCycles */
      if (tagEntry.lastCycles == 0) {
        tagEntry.lastCycles = cpu.cycles;
      }
    } else if ((tagEntry = endTags.get(event)) != null) {
      if (tagEntry.lastCycles != 0) {
        tagEntry.calls++;
        tagEntry.cycles += cpu.cycles - tagEntry.lastCycles;
        tagEntry.lastCycles = 0;
      }      
    }
  }
}
