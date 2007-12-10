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
 * $Id: MapTable.java,v 1.3 2007/10/21 21:17:34 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * MapTable
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.core;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import se.sics.mspsim.util.Utils;

/**
 * The map reader reads the map file with memory map and
 * other information about the binary/firmware to load into the
 * node.
 *
 * Format of the map file must be:
 * .text WS Adress WS Size WS file
 * WS Adress WS function_name
 * WS Adress WS function_name
 * ...
 */
public class MapTable {

  private enum Mode {NONE,CODE,DATA,BSS};
  private Mode mode;

  public String[] functionNames;

  public int heapStartAddress = -1;
  public int stackStartAddress = -1;

  public MapTable() {
  }

  public MapTable(String file) throws IOException {
    loadMap(file);
  }

  /**
   *  <code>parseMapLine</code>
   * parses a line of a map file!
   * @param line a <code>String</code> value
   */
  public void parseMapLine(String line) {
    String parts[] = line.split("\\s+");
    if (line.startsWith(".text")) {
      mode = Mode.CODE;
      System.out.println("CODE Mode");
    } else if (line.startsWith(".bss")) {
      mode = Mode.BSS;
      System.out.println("BSS Mode!");
    } else if (line.startsWith(".data")) {
      mode = Mode.DATA;
      System.out.println("Data Mode!");
    } else if (line.startsWith(" .text")) {
      if (parts.length > 3) {
	// 	  System.out.println("Code " + parts[2] + " Name:" + parts[4]);
      }
    } else if (mode == Mode.CODE && line.startsWith("    ")) {
      if (parts.length > 2) {
	// Scrap 0x and parse as hex!
	int val = Integer.parseInt(parts[1].substring(2), 16);
	System.out.println("Function: " + parts[2] + " at " +
			   Utils.hex16(val));
	// Add the file part later some time...
	// After the demo...
	functionNames[val] = parts[2]; // + " " + currentFile;
      }

    } else if (line.contains(" _end = .") && parts.length > 2) {
      heapStartAddress = Integer.parseInt(parts[1].substring(2), 16);

    } else if (line.contains("PROVIDE (__stack") && parts.length > 2) {
      stackStartAddress = Integer.parseInt(parts[1].substring(2), 16);
    }
  }

  public void loadMap(String file) throws IOException {
    FileInputStream fInput = new FileInputStream(file);
    BufferedReader bInput = new BufferedReader(new InputStreamReader(fInput));
    String line;
    // Allocate stringpointers... -- should it be like this???
    functionNames = new String[0x10000];
    while ((line = bInput.readLine()) != null) {
      parseMapLine(line);
    }
    bInput.close();
    fInput.close();
  }

  // For fast access???
  public String[] getFunctionArray() {
    return functionNames;
  }

  public String getFunction(int address) {
    if (functionNames[address] != null) {
      return functionNames[address];
    } else {
      return null;
    }
  }

  // Should be any symbol... not just function...
  public void setFunctionName(int address, String name) {
    if (functionNames == null)
      functionNames = new String[0x10000];
    functionNames[address] = name;
  }

  // Really slow way to find a specific function address!!!!
  // Either reimplement this or cache in hashtable...
  public int getFunctionAddress(String function) {
    for (int i = 0, n = functionNames.length; i < n; i++) {
      if (function.equals(functionNames[i])) {
        return i;
      }
    }
    return -1;
  }

  public void setStackStart(int start) {
    stackStartAddress = start;
  }

  public void setHeapStart(int start) {
    heapStartAddress = start;
  }

  public static void main(String[] args) throws IOException {
    new MapTable(args[0]);
  }
}
