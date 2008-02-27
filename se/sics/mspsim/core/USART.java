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
 * $Id: USART.java,v 1.3 2007/10/21 21:17:34 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * USART
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.core;

public class USART extends IOUnit {

  public static final boolean DEBUG = false;//true;

  // USART 0/1 register offset (0x70 / 0x78)
  public static final int UCTL = 0;
  public static final int UTCTL = 1;
  public static final int URCTL = 2;
  public static final int UMCTL = 3;
  public static final int UBR0 = 4;
  public static final int UBR1 = 5;
  public static final int URXBUF = 6;
  public static final int UTXBUF = 7;

  public static final int UTXIFG0 = 0x80;
  public static final int URXIFG0 = 0x40;

  public static final int UTXIFG1 = 0x20;
  public static final int URXIFG1 = 0x10;

  // USART SRF mod enable registers (absolute + 1)
  public static final int ME1 = 4;
  public static final int IE1 = 0;
  public static final int IFG1 = 2;

  private int uartID = 0;

  public static final int USART0_RCV_VEC = 9;
  public static final int USART0_TRS_VEC = 8;
  public static final int USART1_RCV_VEC = 3;
  public static final int USART1_TRS_VEC = 2;

  // Flags.
  public static final int UTCTL_TXEMPTY = 0x01;


  private USARTListener listener;

  private int receiveInterrupt = 0;
  private int transmitInterrupt = 0;

  private int utxifg;
  private int urxifg;

  private int clockSource = 0;
  private int baudRate = 0;
  private int tickPerByte = 0;
  private long nextTXReady = -1;

  private MSP430Core cpu;
  private SFR sfr;

  private int uctl;
  private int utctl;
  private int urctl;
  private int umctl;
  private int ubr0;
  private int ubr1;
  private int urxbuf;

  private int utxbuf;
  /**
   * Creates a new <code>USART</code> instance.
   *
   */
  public USART(MSP430Core cpu, int[] memory, int offset) {
    super(memory, offset);
    this.cpu = cpu;
    sfr = cpu.getSFR();
    if (offset == 0x78) {
      uartID = 1;
    }

    // Initialize - transmit = ok...
    // and set which interrupts are used
    if (uartID == 0) {
      receiveInterrupt = USART0_RCV_VEC;
      transmitInterrupt = USART0_TRS_VEC;
      utxifg = UTXIFG0;
      urxifg = URXIFG0;
      memory[IFG1] = 0x82;
    } else {
      receiveInterrupt = USART1_RCV_VEC;
      transmitInterrupt = USART1_TRS_VEC;
      utxifg = UTXIFG1;
      urxifg = URXIFG1;
      memory[IFG1 + 1] = 0x20;
    }
    nextTXReady = cpu.cycles + 1000;
  }

  public void reset() {
    nextTXReady = cpu.cycles + 1000;
  }
  
  private void setBitIFG(int bits) {
    sfr.setBitIFG(uartID, bits);
  }

  private void clrBitIFG(int bits) {
    sfr.clrBitIFG(uartID, bits);
  }

  private int getIFG() {
    return sfr.getIFG(uartID);
  }

  private boolean isIEBitsSet(int bits) {
    return sfr.isIEBitsSet(uartID, bits);
  }

  public void setUSARTListener(USARTListener listener) {
    this.listener = listener;
  }

  // Only 8 bits / read!
  public void write(int address, int data, boolean word, long cycles) {
    address = address - offset;

    // Indicate ready to write!!! - this should not be done here...
//     if (uartID == 0) memory[IFG1] |= 0x82;
//     else memory[IFG1 + 1] |= 0x20;

//     System.out.println(">>>> Write to " + getName() + " at " +
// 		       address + " = " + data);
    switch (address) {
    case UCTL:
      uctl = data;
      if (DEBUG) System.out.println(getName() + " write to UCTL " + data);
      break;
    case UTCTL:
      utctl = data;
      if (DEBUG) System.out.println(getName() + " write to UTCTL " + data);

      if (((data >> 4) & 3) == 1) {
        clockSource = MSP430Constants.CLK_ACLK;
        if (DEBUG) {
          System.out.println(getName() + " Selected ACLK as source");
        }
      } else {
        clockSource = MSP430Constants.CLK_SMCLK;
        if (DEBUG) {
          System.out.println(getName() + " Selected SMCLK as source");
        }
      }
      updateBaudRate();
      break;
    case URCTL:
      urctl = data;
      break;
    case UMCTL:
      umctl = data;
      if (DEBUG) System.out.println(getName() + " write to UMCTL " + data);
      break;
    case UBR0:
      ubr0 = data;
      updateBaudRate();
      break;
    case UBR1:
      ubr1 = data;
      updateBaudRate();
      break;
    case UTXBUF:
      if (DEBUG) System.out.print(getName() + ": USART_UTXBUF:" + (char) data + "\n");

      if (listener != null) {
	listener.dataReceived(this, data);
      }

      // Interruptflag not set!
      clrBitIFG(utxifg);
      utctl &= ~UTCTL_TXEMPTY;
      utxbuf = data;
      if (DEBUG) System.out.println(getName() + " flagging off transmit interrupt");
      cpu.flagInterrupt(transmitInterrupt, this, false);

      nextTXReady = cycles + tickPerByte;

      // We should set the "not-ready" flag here!
      // When should the reception interrupt be received!?
      break;
    }
  }

  public int read(int address, boolean word, long cycles) {
    address = address - offset;
//     System.out.println(">>>>> Read from " + getName() + " at " +
// 		       address + " = " + memory[address]);
    
    switch (address) {
    case UCTL:
      if (DEBUG) System.out.println(getName() + " read from UCTL");
      return uctl;
    case UTCTL:
      if (DEBUG) System.out.println(getName() + " read from UTCTL: " + utctl);
      return utctl;
    case URCTL:
      return urctl;
    case UMCTL:
      return umctl;
    case UBR0:
      return ubr0;
    case UBR1:
      return ubr1;
    case UTXBUF:
      return utxbuf;
    case URXBUF:
      // When byte is read - the interruptflag is cleared!
      // and error status should also be cleared later...
      if (MSP430Constants.DEBUGGING_LEVEL > 0) {
        System.out.println(getName() + " clearing rx interrupt flag");
      }
      clrBitIFG(urxifg);
      return urxbuf;
    }
    return 0;
  }

  private void updateBaudRate() {
    int div = ubr0 + (ubr1 << 8);
    if (div == 0) {
      div = 1;
    }
    if (clockSource == MSP430Constants.CLK_ACLK) {
      if (DEBUG) {
        System.out.println(getName() + " Baud rate is: " + cpu.aclkFrq / div);
      }
      baudRate = cpu.aclkFrq / div;
    } else {
      if (DEBUG) {
        System.out.println(getName() + " Baud rate is: " + cpu.smclkFrq / div);
      }
      baudRate = cpu.smclkFrq / div;
    }
    if (baudRate == 0) baudRate = 1;
    // Is this correct??? Is it the DCO or smclkFRQ we should have here???
    tickPerByte = (8 * cpu.smclkFrq) / baudRate;
    if (DEBUG) {
      System.out.println(getName() +  " Ticks per byte: " + tickPerByte);
    }
  }


  public String getName() {
    return "USART " + uartID;
  }

  // We should add "Interrupt serviced..." to indicate that its latest
  // Interrupt was serviced...
  public void interruptServiced(int vector) {
    cpu.flagInterrupt(vector, this, false);
  }


  // This should be used to delay the output of the USART down to the
  // baudrate!
  public long ioTick(long cycles) {
    if (nextTXReady != -1 && cycles > nextTXReady) {
      // Ready to transmit new byte!

      setBitIFG(utxifg);
      utctl |= UTCTL_TXEMPTY;
      cpu.flagInterrupt(transmitInterrupt, this, isIEBitsSet(utxifg));

      if (DEBUG) {
        if (isIEBitsSet(utxifg)) {
          System.out.println(getName() + " flagging on transmit interrupt");
        }
        System.out.println(getName() + " Ready to transmit next: " + cycles + " " +
            tickPerByte);
      }
      nextTXReady = -1;
    }
    if (baudRate == 0) {
      return cycles + 1000;
    } else {
      return cycles + tickPerByte;
    }
  }


  public boolean isReceiveFlagCleared() {
    return (getIFG() & urxifg) == 0;
  }

  // A byte have been received!
  // This needs to be complemented with a method for checking if the USART
  // is ready for next byte (readyForReceive) that respects the current speed
  public void byteReceived(int b) {
    if (MSP430Constants.DEBUGGING_LEVEL > 0) {
      System.out.println(getName() + " byteReceived: " + b);
    }
    urxbuf = b & 0xff;
    // Indicate interrupt also!
    setBitIFG(urxifg);

    // Check if the IE flag is enabled! - same as the IFlag to indicate!
    if (isIEBitsSet(urxifg)) {
      if (MSP430Constants.DEBUGGING_LEVEL > 0) {
        System.out.println(getName() + " flagging receive interrupt: " +
			   receiveInterrupt);
      }
      cpu.flagInterrupt(receiveInterrupt, this, true);
    }
  }

  public int getModeMax() {
    return 0;
  }

}