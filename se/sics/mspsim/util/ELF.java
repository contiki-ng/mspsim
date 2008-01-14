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
 * $Id: ELF.java,v 1.3 2007/10/21 21:17:34 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * ELF
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.util;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import se.sics.mspsim.core.*;

public class ELF {

  public static final int EI_NIDENT = 16;
  public static final int EI_ENCODING = 5;

  public static final boolean DEBUG = false;

  boolean encMSB = true;
  int type;
  int machine;
  int version;
  int entry;
  int phoff;
  int shoff;
  int flags;
  int ehsize;
  int phentsize;
  int phnum;
  int shentsize;
  int shnum;
  int shstrndx;

  byte[] elfData;
  int pos = 0;

  private ELFSection sections[];
  private ELFProgram programs[];

  ELFSection strTable;
  ELFSection symTable;
  ELFSection dbgStab;
  ELFSection dbgStabStr;

  ELFDebug debug;

  public ELF(byte[] data) {
    elfData = data;
    pos = 0;
  }

  public void readHeader() {
    if (elfData[EI_ENCODING] == 2) {
      encMSB = true;
    } else if (elfData[EI_ENCODING] == 1) {
      encMSB = false;
    } else {
      System.out.println("ERROR: Wroing encoding???");
    }
    pos += 16;
    type = readElf16();
    machine = readElf16();
    version = readElf32();
    entry = readElf32();
    phoff = readElf32();
    shoff = readElf32();
    flags = readElf32();
    ehsize = readElf16();
    phentsize = readElf16();
    phnum = readElf16();
    shentsize = readElf16();
    shnum = readElf16();
    shstrndx = readElf16();

    if (DEBUG) {
      System.out.println("-- ELF Header --");
      System.out.println("type: " + Integer.toString(type, 16));
      System.out.println("machine: " + Integer.toString(machine, 16));
      System.out.println("version: " + Integer.toString(version, 16));
      System.out.println("entry: " + Integer.toString(entry, 16));
      System.out.println("phoff: " + Integer.toString(phoff, 16));
      System.out.println("shoff: " + Integer.toString(shoff, 16));
      System.out.println("flags: " + Integer.toString(flags, 16));
      System.out.println("ehsize: " + Integer.toString(ehsize, 16));
      System.out.println("phentsize: " + Integer.toString(phentsize, 16));
      System.out.println("phentnum: " + Integer.toString(phnum, 16));
      System.out.println("shentsize: " + Integer.toString(shentsize, 16));
      System.out.println("shentnum: " + Integer.toString(shnum, 16));
      System.out.println("shstrndx: " + Integer.toString(shstrndx, 16));
    }
  }

  public ELFSection readSectionHeader() {
    ELFSection sec = new ELFSection();
    sec.name = readElf32();
    sec.type = readElf32();
    sec.flags = readElf32();
    sec.addr = readElf32();
    sec.offset = readElf32();
    sec.size = readElf32();
    sec.link = readElf32();
    sec.info = readElf32();
    sec.addralign = readElf32();
    sec.entSize = readElf32();
    sec.elf = this;
    return sec;
  }

  public ELFProgram readProgramHeader() {
    ELFProgram pHeader = new ELFProgram();
    pHeader.type = readElf32();
    pHeader.offset = readElf32();
    pHeader.vaddr = readElf32();
    pHeader.paddr = readElf32();
    pHeader.fileSize = readElf32();
    pHeader.memSize = readElf32();
    pHeader.flags = readElf32();
    pHeader.align = readElf32();
    // 8 * 4 = 32
    if (phentsize > 32) {
      System.out.println("Program Header Entry SIZE differs from specs?!?!??!?!?***");
    }
    return pHeader;
  }

//   public ELFSection getSection(int pos) {
//     sec.name = getElf32(pos);
//     pos += 4;
//   }

  int readElf32() {
    int b = 0;
    if (encMSB) {
      b = (elfData[pos++] & 0xff) << 24 |
	((elfData[pos++] & 0xff) << 16) |
	((elfData[pos++] & 0xff) << 8) |
	(elfData[pos++] & 0xff);
    } else {
      b = (elfData[pos++] & 0xff) |
	((elfData[pos++] & 0xff) << 8) |
	((elfData[pos++] & 0xff) << 16) |
	((elfData[pos++] & 0xff) << 24);
    }
    return b;
  }

  int readElf16() {
    int b = 0;
    if (encMSB) {
      b = ((elfData[pos++] & 0xff) << 8) |
	(elfData[pos++] & 0xff);
    } else {
      b = (elfData[pos++] & 0xff) |
	((elfData[pos++] & 0xff) << 8);
    }
    return b;
  }

  int readElf8() {
    return elfData[pos++] & 0xff;
  }

  public static void printBytes(String name, byte[] data) {
    System.out.print(name + " ");
    for (int i = 0, n = data.length; i < n; i++) {
      System.out.print("" + (char) data[i]);
    }
    System.out.println("");
  }

  private void readSections() {
    pos = shoff;

    sections = new ELFSection[shnum];
    for (int i = 0, n = shnum; i < n; i++) {
      sections[i] = readSectionHeader();
      if (sections[i].type == ELFSection.TYPE_SYMTAB) {
	symTable = sections[i];
      }
      if (i == shstrndx) {
	strTable = sections[i];
      }
    }

    /* Find sections */
    for (int i = 0, n = shnum; i < n; i++) {
      if (".stabstr".equals(sections[i].getSectionName())) {
	dbgStabStr = sections[i];
      }
      if (".stab".equals(sections[i].getSectionName())) {
	dbgStab = sections[i];
      }
    }

  }

  private void readPrograms() {
    pos = phoff;
    programs = new ELFProgram[phnum];
    for (int i = 0, n = phnum; i < n; i++) {
      programs[i] = readProgramHeader();
      if (DEBUG) {
	System.out.println("-- Program header --\n" + programs[i].toString());
      }
    }
  }

  private void readAll() {
    readHeader();
    readPrograms();
    readSections();
    if (dbgStab != null) {
      debug = new ELFDebug(this, dbgStab, dbgStabStr);
    }
  }

  public void loadPrograms(int[] memory) {
    for (int i = 0, n = phnum; i < n; i++) {
      // paddr or vaddr???
      loadBytes(memory, programs[i].offset, programs[i].paddr,
		programs[i].fileSize, programs[i].memSize);
    }
  }

  private void loadBytes(int[] memory, int offset, int addr, int len,
			 int fill) {
    System.out.println("Loading " + len + " bytes into " +
		       Integer.toString(addr, 16));
    for (int i = 0, n = len; i < n; i++) {
      memory[addr++] = elfData[offset++] & 0xff;
    }
    if (fill > len) {
      for (int i = 0, n = fill - len; i < n; i++) {
	memory[addr++] = 0;
      }
    }
  }

  public DebugInfo getDebugInfo(int adr) {
    return debug.getDebugInfo(adr);
  }

  public MapTable getMap() {
    MapTable map = new MapTable();

    ELFSection name = sections[symTable.link];
    int len = symTable.size;
    int count = len / symTable.entSize;
    int addr = symTable.offset;
    String currentFile = "";
    if (DEBUG)
      System.out.println("Number of symbols:" + count);
    for (int i = 0, n = count; i < n; i++) {
      pos = addr;
      int nI = readElf32();
      String sn = name.getName(nI);
      int sAddr = readElf32();
      int size = readElf32();
      int info = readElf8();
      int bind = info >> 4;
      int type = info & 0xf;

      if (type == ELFSection.SYMTYPE_FILE) {
	currentFile = sn;
      }

      if (DEBUG)
	System.out.println("Found symbol: " + sn + " at " +
			   Integer.toString(sAddr, 16) + " bind: " + bind +
			   " type: " + type + " size: " + size);

      if (sAddr > 0 && sAddr < 0x10000) {
	String symbolName = sn;
//	if (bind == ELFSection.SYMBIND_LOCAL) {
//	  symbolName += " (" + currentFile + ')';
//	}
	if ("_end".equals(symbolName)) {
	  map.setHeapStart(sAddr);
	} else if ("__stack".equals(symbolName)){
	  map.setStackStart(sAddr);
	}

	map.setEntry(new MapEntry(MapEntry.TYPE.function, sAddr, symbolName, currentFile, 
	    bind == ELFSection.SYMBIND_LOCAL));
      }
      addr += symTable.entSize;
    }

    return map;
  }

  public static ELF readELF(String file) throws IOException {
    DataInputStream input = new DataInputStream(new FileInputStream(file));
    ByteArrayOutputStream baous = new ByteArrayOutputStream();
    byte[] buf = new byte[2048];
    for(int read; (read = input.read(buf)) != -1; baous.write(buf, 0, read));
    buf = null;
    byte[] data = baous.toByteArray();
    System.out.println("Length of data: " + data.length);

    ELF elf = new ELF(data);
    elf.readAll();

    return elf;
  }

  public static void main(String[] args) throws Exception {
    ELF elf = readELF(args[0]);

    if (args.length < 2) {
      for (int i = 0, n = elf.shnum; i < n; i++) {
	System.out.println("-- Section header " + i + " --\n" + elf.sections[i]);
	if (".stab".equals(elf.sections[i].getSectionName()) ||
	    ".stabstr".equals(elf.sections[i].getSectionName())) {
	  int adr = elf.sections[i].offset;
	  System.out.println(" == Section data ==");
	  for (int j = 0, m = 2000; j < m; j++) {
	    System.out.print((char) elf.elfData[adr++]);
	    if (i % 20 == 19) System.out.println("");
	  }
	}
	System.out.println("");
      }
    }
    elf.getMap();
    if (args.length > 1) {
      DebugInfo dbg = elf.getDebugInfo(Integer.parseInt(args[1]));
      if (dbg != null) {
	System.out.println("File: " + dbg.getFile());
	System.out.println("Function: " + dbg.getFunction());
	System.out.println("LineNo: " + dbg.getLine());
      }
    }
  }


} // ELF
