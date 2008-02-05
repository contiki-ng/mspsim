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
 * $Id: ExtFlash.java,v 1.3 2007/10/21 21:17:34 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * ExtFlash
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.chip;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import se.sics.mspsim.core.*;

public class M25P80 extends Chip implements USARTListener, PortListener {

  public static final int WRITE_STATUS = 0x01;
  public static final int PAGE_PROGRAM = 0x02;
  public static final int READ_DATA = 0x03;
  public static final int WRITE_DISABLE = 0x04;
  public static final int READ_STATUS = 0x05;
  public static final int WRITE_ENABLE = 0x06;
  public static final int READ_DATA_FAST = 0x0b;
  public static final int READ_IDENT = 0x9f;
  public static final int SECTOR_ERASE = 0xd8;
  public static final int BULK_ERASE = 0xc7;
  public static final int DEEP_POWER_DOWN = 0xb9;
  public static final int WAKE_UP = 0xab;
  
  private int state = 0;
  public static final int CHIP_SELECT = 0x10;
  private boolean chipSelect;

  private USART usart;
  private int pos;
  private int status = 0;
  
  private boolean writeEnable = false;
  
  private int[] identity = new int[] {
      0x20,0x20,0x14,0x10,
      0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
      0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00
  };
  private int readAddress;
  private int loadedAddress;
  private byte[] readMemory = new byte[256];
  private byte[] buffer = new byte[256];
  
  private RandomAccessFile file;
  
  public M25P80(USART usart) {
    this.usart = usart;
    try {
      file = new RandomAccessFile("flash.bin", "rw");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }
    
    try {
      file.setLength(1024 * 1024);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void dataReceived(USART source, int data) {
    if (chipSelect) {
      System.out.println("M25P80: byte received: " + data);
      switch(state) {
      case READ_IDENT:
        source.byteReceived(identity[pos]);
        pos++;
        if (pos >= identity.length)
          pos = 0;
        return;
      case WRITE_STATUS:
        status = data;
        source.byteReceived(0);
        return;
      case READ_DATA:
        if (pos < 3) {
          readAddress = (readAddress << 8) + data;
          source.byteReceived(0);
        } else {
          source.byteReceived(readMemory(readAddress++));
          if (readAddress > 0xfffff) {
             readAddress = 0; 
          }
        }
        return;
      case PAGE_PROGRAM:
        if (pos < 3) {
          readAddress = (readAddress << 8) + data;
          source.byteReceived(0);
        } else {
          // Do the programming!!!
          source.byteReceived(0);
          writeBuffer((readAddress++) & 0xff, data);
        }
        return;
      }
      
      switch (data) {
      case WRITE_ENABLE:
        System.out.println("M2580: Write Enable");
        writeEnable = true;
        break;
      case WRITE_DISABLE:
        System.out.println("M2580: Write Disable");
        writeEnable = false;
        break;
      case READ_IDENT:
        System.out.println("M2580: Read ident.");
        state = READ_IDENT;
        pos = 0;
        source.byteReceived(identity[pos++]);
        return;
      case READ_STATUS:
        System.out.println("M2580: Read status.");
        status = status & (0xff - 1 - 2) | (writeEnable ? 0x02 : 0x00);
        source.byteReceived(status);
        return;
      case WRITE_STATUS:
        System.out.println("M2580: Write status");
        state = WRITE_STATUS;
        break;
      case READ_DATA:
        System.out.println("M2580: Read Data");
        state = READ_DATA;
        pos = 0;
        break;
      case PAGE_PROGRAM:
        System.out.println("M2580: Page Program");
        state = PAGE_PROGRAM;
        pos = 0;
        break;
      case SECTOR_ERASE:
        System.out.println("M2580: Sector Erase");
        state = SECTOR_ERASE;
        pos = 0;
        break;
      }
      source.byteReceived(0);
    }
  }

  // Should return correct data!
  private int readMemory(int address) {
    System.out.println("M2580: Reading memory address: " + Integer.toHexString(address));
    ensureLoaded(address);
    return readMemory[address & 0xff];
  }
  
  private void writeBuffer(int address, int data) {
    buffer[address] = (byte) data;
  }
  
  private void ensureLoaded(int address) {
    if (!((loadedAddress & 0xfff00) == (address & 0xfff00))) {
      try {
        System.out.println("M2580: Loading memory: " + (address & 0xfff00));
        file.seek(address & 0xfff00);
        file.readFully(readMemory);
      } catch (IOException e) {
        e.printStackTrace();
      }
      loadedAddress = address & 0xfff00;
    }
  }
  
  public void portWrite(IOPort source, int data) {
    // Chip select = active low...
    chipSelect = (data & CHIP_SELECT) == 0;
    System.out.println("M25P80: write to Port4: " +
		       Integer.toString(data, 16)
		       + " CS:" + chipSelect);
    state = 0;
  }

  public int getModeMax() {
    return 0;
  }

  public String getName() {
    return "M25P80: external flash";
  }


} // ExtFlash
