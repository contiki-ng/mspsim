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
 * CC2420
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 *
 */

package se.sics.mspsim.chip;
import se.sics.mspsim.core.*;
import se.sics.mspsim.util.Utils;

public class CC2420 extends Chip implements USARTListener, RFListener {

  public static final boolean DEBUG = true; //false; //true;

  public static final int REG_SNOP		= 0x00;
  public static final int REG_SXOSCON	        = 0x01;
  public static final int REG_STXCAL		= 0x02;
  public static final int REG_SRXON		= 0x03;
  public static final int REG_STXON		= 0x04;
  public static final int REG_STXONCCA	        = 0x05;
  public static final int REG_SRFOFF		= 0x06;
  public static final int REG_SXOSCOFF	        = 0x07;
  public static final int REG_SFLUSHRX	        = 0x08;
  public static final int REG_SFLUSHTX	        = 0x09;
  public static final int REG_SACK		= 0x0A;
  public static final int REG_SACKPEND	        = 0x0B;
  public static final int REG_SRXDEC		= 0x0C;
  public static final int REG_STXENC		= 0x0D;
  public static final int REG_SAES		= 0x0E;
  public static final int REG_foo		= 0x0F;
  public static final int REG_MAIN		= 0x10;
  public static final int REG_MDMCTRL0	        = 0x11;
  public static final int REG_MDMCTRL1	        = 0x12;
  public static final int REG_RSSI		= 0x13;
  public static final int REG_SYNCWORD	        = 0x14;
  public static final int REG_TXCTRL		= 0x15;
  public static final int REG_RXCTRL0	        = 0x16;
  public static final int REG_RXCTRL1	        = 0x17;
  public static final int REG_FSCTRL		= 0x18;
  public static final int REG_SECCTRL0	        = 0x19;
  public static final int REG_SECCTRL1       	= 0x1A;
  public static final int REG_BATTMON   	= 0x1B;
  public static final int REG_IOCFG0		= 0x1C;
  public static final int REG_IOCFG1		= 0x1D;
  public static final int REG_MANFIDL   	= 0x1E;
  public static final int REG_MANFIDH   	= 0x1F;
  public static final int REG_FSMTC		= 0x20;
  public static final int REG_MANAND		= 0x21;
  public static final int REG_MANOR		= 0x22;
  public static final int REG_AGCCTRL    	= 0x23;
  public static final int REG_AGCTST0   	= 0x24;
  public static final int REG_AGCTST1   	= 0x25;
  public static final int REG_AGCTST2   	= 0x26;
  public static final int REG_FSTST0		= 0x27;
  public static final int REG_FSTST1		= 0x28;
  public static final int REG_FSTST2		= 0x29;
  public static final int REG_FSTST3		= 0x2A;
  public static final int REG_RXBPFTST    	= 0x2B;
  public static final int REG_FSMSTATE   	= 0x2C;
  public static final int REG_ADCTST		= 0x2D;
  public static final int REG_DACTST		= 0x2E;
  public static final int REG_TOPTST		= 0x2F;
  public static final int REG_RESERVED   	= 0x30;
  /* 0x31 - 0x3D not used */
  public static final int REG_TXFIFO		= 0x3E;
  public static final int REG_RXFIFO		= 0x3F;

  public static final int STATUS_XOSC16M_STABLE = 1 << 6;
  public static final int STATUS_TX_UNDERFLOW   = 1 << 5;
  public static final int STATUS_ENC_BUSY	    = 1 << 4;
  public static final int STATUS_TX_ACTIVE	= 1 << 3;
  public static final int STATUS_LOCK	= 1 << 2;
  public static final int STATUS_RSSI_VALID	= 1 << 1;

  // IOCFG0 Register Bit masks
  public static final int BCN_ACCEPT = (1<<11);
  public static final int FIFO_POLARITY = (1<<10);
  public static final int FIFOP_POLARITY = (1<<9);
  public static final int SFD_POLARITY = (1<<8);
  public static final int CCA_POLARITY = (1<<7);
  public static final int FIFOP_THR = 0x7F;

  // IOCFG1 Register Bit Masks
  public static final int SFDMUX = 0x3E0;
  public static final int CCAMUX = 0x1F;

  // CCAMUX values
  public static final int CCA_CCA = 0;
  public static final int CCA_XOSC16M_STABLE = 24;


  // RAM Addresses
  public static final int RAM_TXFIFO	= 0x000;
  public static final int RAM_RXFIFO	= 0x080;
  public static final int RAM_KEY0	= 0x100;
  public static final int RAM_RXNONCE	= 0x110;
  public static final int RAM_SABUF	= 0x120;
  public static final int RAM_KEY1	= 0x130;
  public static final int RAM_TXNONCE	= 0x140;
  public static final int RAM_CBCSTATE	= 0x150;
  public static final int RAM_IEEEADDR	= 0x160;
  public static final int RAM_PANID	= 0x168;
  public static final int RAM_SHORTADDR	= 0x16A;

  // The Operation modes of the CC2420
  public static final int MODE_TXRX_OFF = 0x00;
  public static final int MODE_RX_ON = 0x01;
  public static final int MODE_TXRX_ON = 0x02;
  public static final int MODE_POWER_OFF = 0x03;
  public static final int MODE_MAX = MODE_POWER_OFF;
  private static final String[] MODE_NAMES = new String[] {
    "off", "listen", "transmit", "power_off"
  };

  // State Machine - Datasheet Figure 25 page 44
  public static final int STATE_VREG_OFF = -1;
  public static final int STATE_POWER_DOWN = 0;
  public static final int STATE_IDLE = 1;
  public static final int STATE_RX_CALIBRATE = 2;
  public static final int STATE_RX_SFD_SEARCH = 3;
  public static final int STATE_RX_WAIT = 14;
  public static final int STATE_RX_FRAME = 16;
  public static final int STATE_RX_OVERFLOW = 17;
  public static final int STATE_TX_CALIBRATE = 32;
  public static final int STATE_TX_PREAMBLE = 34;
  public static final int STATE_TX_FRAME = 37;
  public static final int STATE_TX_ACK_CALIBRATE = 48;
  public static final int STATE_TX_ACK_PREABLE = 49;
  public static final int STATE_TX_ACK = 52;
  public static final int STATE_TX_UNDERFLOW = 56;

  // FCF High
  public static final int FRAME_TYPE = 0xC0;
  public static final int SECURITY_ENABLED = (1<<6);
  public static final int FRAME_PENDING = (1<<5);
  public static final int ACK_REQUEST = (1<<4);
  public static final int INTRA_PAN = (1<<3);
  // FCF Low
  public static final int DESTINATION_ADDRESS_MODE = 0x30;
  public static final int SOURCE_ADDRESS_MODE = 0x3;
  
  private int stateMachine = STATE_VREG_OFF;

  // 802.15.4 symbol period in ms
  public static final double SYMBOL_PERIOD = 0.016; // 16 us

  // when reading registers this flag is set!
  public static final int FLAG_READ = 0x40;

  public static final int FLAG_RAM = 0x80;
  // When accessing RAM the second byte of the address contains
  // a flag indicating read/write
  public static final int FLAG_RAM_READ = 0x20;


  public static final int WAITING = 0;
  public static final int WRITE_REGISTER = 1;
  public static final int READ_REGISTER = 2;
  public static final int RAM_ACCESS = 3;

  public static final int READ_RXFIFO = 4;
  public static final int WRITE_TXFIFO = 5;

  private int state = WAITING;
  private int pos;
  private int address;
  private int shr_pos;
  private int txfifo_pos;
  private boolean txfifo_flush;	// TXFIFO is automatically flushed on next write
  private int rxfifo_write_pos;
  private int rxfifo_read_pos;
  private int rxfifo_len;
  private int rxlen;
  private int rxread;
  private int zero_symbols;
  private boolean ramRead = false;
  private boolean cca;

  private int activeFrequency = 0;
  private int activeChannel = 0;

  //private int status = STATUS_XOSC16M_STABLE | STATUS_RSSI_VALID;
  private int status = 0;

  private int[] registers = new int[64];
  // More than needed...
  private int[] memory = new int[512];

  // Buffer to hold 5 byte Synchronization header, as it is not written to the TXFIFO
  private byte[] SHR = new byte[5];

  private boolean chipSelect;

  private IOPort ccaPort = null;
  private int ccaPin;

  private IOPort fifopPort = null;
  private int fifopPin;

  private IOPort fifoPort = null;
  private int fifoPin;

  private IOPort sfdPort = null;
  private int sfdPin;

  private boolean rxPacket = false;
  private int rxLen;
  private int txCursor;
  private RFListener listener;


  private MSP430Core cpu;


  private TimeEvent oscillatorEvent = new TimeEvent(0) {
    public void execute(long t) {
      status |= STATUS_XOSC16M_STABLE;
      if(DEBUG) System.out.println("CC2420: Oscillator Stable Event.");
      setState(STATE_IDLE);
      if( (registers[REG_IOCFG1] & CCAMUX) == CCA_XOSC16M_STABLE) {
        setCCA(true);
      } else {
        if(DEBUG) System.out.println("CC2420: CCAMUX != CCA_XOSC16M_STABLE! Not raising CCA");
      }
    }
  };

  private TimeEvent vregEvent = new TimeEvent(0) {
    public void execute(long t) {
      if(DEBUG) System.out.println("CC2420: VREG Started at: " + t + " cyc: " +
          cpu.cycles + " " + getTime());
      setCCA(false);
      on = true;
      setState(STATE_POWER_DOWN);
    }
  };

  private TimeEvent sendEvent = new TimeEvent(0) {
    public void execute(long t) {
      txNext();
    }
  };

  private TimeEvent shrEvent = new TimeEvent(0) {
    public void execute(long t) {
      shrNext();
    }
  };

  private TimeEvent symbolEvent = new TimeEvent(0) {
    public void execute(long t) {
      switch(stateMachine) {
      case STATE_RX_CALIBRATE:
        setClear(true);
        setState(STATE_RX_SFD_SEARCH);
        break;

      case STATE_TX_CALIBRATE:
        setState(STATE_TX_PREAMBLE);
        break;

      case STATE_RX_WAIT:
        setClear(true);
        setState(STATE_RX_SFD_SEARCH);
        break;
      }
    }
  };

  private boolean setState(int state) {
    //if(DEBUG) System.out.println("CC2420: State Transition from " + stateMachine + " to " + state);
    stateMachine = state;

    switch(stateMachine) {

    case STATE_VREG_OFF:
      if (DEBUG) System.out.println("CC2420: VREG Off.");
      break;

    case STATE_POWER_DOWN:
      rxfifo_read_pos = 0;
      rxfifo_write_pos = 0;
      break;

    case STATE_RX_CALIBRATE:
      setSymbolEvent(12);
      break;

    case STATE_RX_SFD_SEARCH:
      zero_symbols = 0;
      // RSSI valid here?
      status |= STATUS_RSSI_VALID;
      break;

    case STATE_TX_CALIBRATE:
      /* 12 symbols calibration, and one byte's wait since we deliver immediately
       * to listener when after calibration?
       */
      setSymbolEvent(12 + 2);
      break;

    case STATE_TX_PREAMBLE:
      shr_pos = 0;
      SHR[0] = 0;
      SHR[1] = 0;
      SHR[2] = 0;
      SHR[3] = 0;
      SHR[4] = 0x7A;
      shrNext();
      break;

    case STATE_TX_FRAME:
      txfifo_pos = 0;
      txNext();
      break;

    case STATE_RX_WAIT:
      setSymbolEvent(8);
      break;
      
    case STATE_IDLE:
      status &= ~STATUS_RSSI_VALID;
      break;
    }

    return true;

  }

  private boolean on;

  public CC2420(MSP430Core cpu) {
    registers[REG_SNOP] = 0;
    registers[REG_TXCTRL] = 0xa0ff;
    this.cpu = cpu;
    setModeNames(MODE_NAMES);
    setMode(MODE_POWER_OFF);
    rxPacket = false;
    rxfifo_read_pos = 0;
    rxfifo_write_pos = 0;
    cca = false;    
  }

  public void receivedByte(byte data) {
    // Received a byte from the "air"
    if(cca)
      setClear(false);

    // Above RX_WAIT => RX_SFD_SEARCH after 8 symbols should make this work without this???
//    if (stateMachine == STATE_RX_WAIT) {
//      setState(STATE_RX_SFD_SEARCH);
//    }
    
    if(stateMachine == STATE_RX_SFD_SEARCH) {
      // Look for the preamble (4 zero bytes) followed by the SFD byte 0x7A
      if(data == 0) {
        // Count zero bytes
        zero_symbols++;
        return;
      }
      // If the received byte is !zero, we have counted 4 zero bytes prior to this one,
      // and the current received byte == 0x7A (SFD), we're in sync.
      if(zero_symbols == 4) {
        if(data == 0x7A) {
          // In RX mode, SFD goes high when the SFD is received
          setSFD(true);
          if (DEBUG) System.out.println("CC2420: RX: Preamble/SFD Synchronized.");
          rxread = 0;
          setState(STATE_RX_FRAME);
        } else {
          zero_symbols = 0;
        }
      }

    } else if(stateMachine == STATE_RX_FRAME) {
      if(rxfifo_len == 128) {
        setRxOverflow();
      }else{		  
        memory[RAM_RXFIFO + rxfifo_write_pos++] = data & 0xFF;
        rxfifo_len++;

        if(rxfifo_write_pos == 128) {
          if (DEBUG) System.out.println("Wrapped RXFIFO write pos");
          rxfifo_write_pos = 0;
        }

        if(rxread == 0) {
          rxlen = (int)data;
          if (DEBUG) System.out.println("CC2420: RX: Start frame length " + rxlen);
          // FIFO pin goes high after length byte is written to RXFIFO
          setFIFO(true);
        }

        if(rxread++ == rxlen) {
          // In RX mode, FIFOP goes high, if threshold is higher than frame length....

          // Should take a RSSI value as input or use a set-RSSI value...
          memory[RAM_RXFIFO + (rxfifo_write_pos - 2)] = (registers[REG_RSSI]) & 0xff;
          // Set CRC ok and add a correlation
          memory[RAM_RXFIFO + (rxfifo_write_pos -1 )] = 37 | 0x80;
          setFIFOP(true);
          setSFD(false);
          if (DEBUG) System.out.println("CC2420: RX: Complete.");
          setState(STATE_RX_WAIT);
        }
      }


    }

  }

  public void dataReceived(USART source, int data) {
    if ( (stateMachine != STATE_VREG_OFF) && chipSelect) {

      /*
      if (DEBUG) {
    	System.out.println("State Machine: " + stateMachine);  
        System.out.println("CC2420 byte received: " + Utils.hex8(data) +
            " (" + ((data >= ' ' && data <= 'Z') ? (char) data : '.') + ')' +
            " CS: " + chipSelect + " state: " + state);
      }
       */
      switch(state) {
      case WAITING:
        state = WRITE_REGISTER;
        if ((data & FLAG_READ) != 0) {
          state = READ_REGISTER;
        }
        if ((data & FLAG_RAM) != 0) {
          state = RAM_ACCESS;
          address = data & 0x7f;
        } else {
          // The register address
          address = data & 0x3f;

          if (address == REG_RXFIFO) {
            // check read/write???
            //          System.out.println("CC2420: Reading RXFIFO!!!");
            state = READ_RXFIFO;
          } else if (address == REG_TXFIFO) {
            state = WRITE_TXFIFO;
          }
        }
        if (data < 0x0f) {
          strobe(data);
        }
        pos = 0;
        // Assuming that the status always is sent back???
        //source.byteReceived(status);
        break;
      case WRITE_REGISTER:
        if (pos == 0) {
          source.byteReceived(registers[address] >> 8);
          // set the high bits
          registers[address] = registers[address] & 0xff | (data << 8);
        } else {
          source.byteReceived(registers[address] & 0xff);
          // set the low bits
          registers[address] = registers[address] & 0xff00 | data;
          /*
          if (DEBUG) {
            System.out.println("CC2420: wrote to " + Utils.hex8(address) + " = "
                + registers[address]);
            switch(address) {
            case REG_IOCFG0:
            	System.out.println("CC2420: IOCFG0: " + registers[address]);
            	break;
            case REG_IOCFG1:
            	System.out.println("CC2420: IOCFG1: SFDMUX "
            			+ ((registers[address] & SFDMUX) >> SFDMUX)
            			+ " CCAMUX: " + (registers[address] & CCAMUX));
            	if( (registers[address] & CCAMUX) == CCA_CCA)
            		setCCA(false);
            	break;
            }
          }
           */
        }
        pos++;
        break;
      case READ_REGISTER:
        if (pos == 0) {
          source.byteReceived(registers[address] >> 8);
        } else {
          source.byteReceived(registers[address] & 0xff);
          if (DEBUG) {
            System.out.println("CC2420: read from " + Utils.hex8(address) + " = "
                + registers[address]);
          }
        }
        pos++;
        return;
        //break;
      case READ_RXFIFO:
        if(rxfifo_len == 0)
          break;
        if(DEBUG) System.out.println("CC2420: RXFIFO READ " + rxfifo_read_pos + " => " +
            (memory[RAM_RXFIFO + rxfifo_read_pos] & 0xFF) );
        source.byteReceived( (memory[RAM_RXFIFO + rxfifo_read_pos] & 0xFF) );
        rxfifo_read_pos++;
        setFIFOP(false);

        // Set the FIFO pin low if there are no more bytes available in the RXFIFO.
        if(--rxfifo_len == 0)
          setFIFO(false);

        // What if wrap cursor???
        if (rxfifo_read_pos >= 128) {
          rxfifo_read_pos = 0;
        }
        // When is this set to "false" - when is interrupt de-triggered?
        // TODO:
        // -MT FIFOP is lowered when there are less than IOCFG0:FIFOP_THR bytes in the RXFIFO
        // If FIFO_THR is greater than the frame length, FIFOP goes low when the first byte is read out.
        if (rxPacket) {
          rxPacket = false;
          setFIFOP(false);
        }
        return;
      case WRITE_TXFIFO:
        if(txfifo_flush) {
          txCursor = 0;
          txfifo_flush = false;
        }  
        if (DEBUG) System.out.println("Writing data: " + data + " to tx: " + txCursor);

        memory[RAM_TXFIFO + txCursor++] = data & 0xff;
        break;
      case RAM_ACCESS:
        if (pos == 0) {
          address = address | (data << 1) & 0x180;
          ramRead = (data & 0x20) != 0;
          if (DEBUG) {
            System.out.println("CC2420: Address: " + Utils.hex16(address) +
                " read: " + ramRead);
          }
          pos++;
        } else {
          if (!ramRead) {
            memory[address++] = data;
            if (DEBUG && address == RAM_PANID + 2) {
              System.out.println("CC2420: Pan ID set to: 0x" +
                  Utils.hex8(memory[RAM_PANID]) +
                  Utils.hex8(memory[RAM_PANID + 1]));
            }
          }else{
            //System.out.println("Read RAM Addr: " + address + " Data: " + memory[address]);  
            source.byteReceived(memory[address++]);
            return;
          }
        }
        break;
      }

      source.byteReceived(status);  
    }
  }

  // Needs to get information about when it is possible to write
  // next data...
  private void strobe(int data) {
    // Resets, on/off of different things...
    //if (DEBUG) {
    //  System.out.println("CC2420: Strobe on: " + Utils.hex8(data));
    //}

    if( (stateMachine == STATE_POWER_DOWN) && (data != REG_SXOSCON) ) {
      if (DEBUG) System.out.println("CC2420: Got command strobe: " + data + " in STATE_POWER_DOWN.  Ignoring.");
      return;
    }

    switch (data) {
    case REG_SNOP:
      if (DEBUG) System.out.println("CC2420: SNOP => " + Utils.hex8(status) + " at " + cpu.cycles);
      break;
    case REG_SRXON:
      if(stateMachine == STATE_IDLE) {
        setState(STATE_RX_CALIBRATE);
        //updateActiveFrequency();
        if (DEBUG) {
            System.out.println("CC2420: Strobe RX-ON!!!");
        }
        setMode(MODE_RX_ON);
      }else{
        if (DEBUG) System.out.println("CC2420: WARNING: SRXON when not IDLE");
      }

      break;
    case REG_SRFOFF:
      if (DEBUG) {
        System.out.println("CC2420: Strobe RXTX-OFF!!! at " + cpu.cycles);
      }
      setState(STATE_IDLE);
      setMode(MODE_TXRX_OFF);
      break;
    case REG_STXON:
      // State transition valid from IDLE state or all RX states
      if( (stateMachine == STATE_IDLE) || 
          (stateMachine == STATE_RX_CALIBRATE) ||
          (stateMachine == STATE_RX_SFD_SEARCH) ||
          (stateMachine == STATE_RX_FRAME) ||
          (stateMachine == STATE_RX_OVERFLOW) ||
          (stateMachine == STATE_RX_WAIT)) {
        status |= STATUS_TX_ACTIVE;
        setState(STATE_TX_CALIBRATE);
        // Starting up TX subsystem - indicate that we are in TX mode!
        setMode(MODE_TXRX_ON);
        if (DEBUG) System.out.println("CC2420: Strobe STXON - transmit on! at " + cpu.cycles);
      }
      break;
    case REG_STXONCCA:
      // Only valid from all RX states,
      // since CCA requires ??(look this up) receive symbol periods to be valid
      if( (stateMachine == STATE_RX_CALIBRATE) ||
          (stateMachine == STATE_RX_SFD_SEARCH) ||
          (stateMachine == STATE_RX_FRAME) ||
          (stateMachine == STATE_RX_OVERFLOW) ||
          (stateMachine == STATE_RX_WAIT)) {
        if(cca) {
          status |= STATUS_TX_ACTIVE;
          setState(STATE_TX_CALIBRATE);
          setMode(MODE_TXRX_ON);
          if (DEBUG) System.out.println("CC2420: Strobe STXONCCA - transmit on! at " + cpu.cycles);
        }else{
          if (DEBUG) System.out.println("CC2420: STXONCCA Ignored, CCA false");
        }
      }
      break;
    case REG_SFLUSHRX:
      flushRX();
      break;
    case REG_SFLUSHTX:
      if (DEBUG) System.out.println("CC2420: Flushing TXFIFO");
      flushTX();
      break;
    case REG_SXOSCON:
      //System.out.println("CC2420: Strobe Oscillator On");
      startOscillator();
      break;
    case REG_SXOSCOFF:
      //System.out.println("CC2420: Strobe Oscillator Off");
      stopOscillator();
      break;
    default:
      if (DEBUG) {
        System.out.println("Unknown strobe command: " + data);
      }
    break;
    }
  }

  public void updateActiveFrequency() {
    /* INVERTED: f = 5 * (c - 11) + 357 + 0x4000 */
    activeFrequency = registers[REG_FSCTRL] - 357 + 2405 - 0x4000;
    activeChannel = (registers[REG_FSCTRL] - 357 - 0x4000)/5 + 11;
  }

  public int getActiveFrequency() {
    return activeFrequency;
  }

  public int getActiveChannel() {
    return activeChannel;
  }

  public int getOutputPowerIndicator() {
    return (registers[REG_TXCTRL] & 0x1f);
  }

  private static int RSSI_OFFSET = -45; /* cc2420 datasheet */

  public void setRSSI(int power) {
    if (power < -128) {
      power = -128;
    }
    registers[REG_RSSI] = power - RSSI_OFFSET;
  }

  public int getRSSI() {
    return registers[REG_RSSI] + RSSI_OFFSET;
  }

  public int getOutputPower() {
    /* From CC2420 datasheet */
    int indicator = getOutputPowerIndicator();
    if (indicator >= 31) {
      return 0;
    } else if (indicator >= 27) {
      return -1;
    } else if (indicator >= 23) {
      return -3;
    } else if (indicator >= 19) {
      return -5;
    } else if (indicator >= 15) {
      return -7;
    } else if (indicator >= 11) {
      return -10;
    } else if (indicator >= 7) {
      return -15;
    } else if (indicator >= 3) {
      return -25;
    }

    /* Unknown */
    return -100;
  }

  private void shrNext() {
    if(shr_pos == 5) {
      // Set SFD high
      setSFD(true);
      setState(STATE_TX_FRAME);
    } else {
      listener.receivedByte(SHR[shr_pos++]);
      cpu.scheduleTimeEventMillis(shrEvent, SYMBOL_PERIOD * 2);
    }
  }

  private void txNext() {
    if(txfifo_pos <= memory[RAM_TXFIFO]) {
      listener.receivedByte((byte)(memory[RAM_TXFIFO + txfifo_pos++] & 0xFF));
      // Two symbol periods to send a byte...
      long time = cpu.scheduleTimeEventMillis(sendEvent, SYMBOL_PERIOD * 2);
//      System.out.println("Scheduling 2 SYMB at: " + time + " getTime(now): " + cpu.getTime());
    } else {
      if (DEBUG) System.out.println("Completed Transmission.");
      status &= ~STATUS_TX_ACTIVE;
      setSFD(false);
      setState(STATE_RX_CALIBRATE);
      setMode(MODE_RX_ON);
      txfifo_flush = true;
    }
  }

  private void setSymbolEvent(int symbols) {
    double period = SYMBOL_PERIOD * symbols;
    cpu.scheduleTimeEventMillis(symbolEvent, period);
    //System.out.println("Set Symbol event: " + period);
  }

  private void startOscillator() {
    // 1ms crystal startup from datasheet pg12
    cpu.scheduleTimeEventMillis(oscillatorEvent, 1);
  }

  private void stopOscillator() {
    status &= ~STATUS_XOSC16M_STABLE;
    setState(STATE_POWER_DOWN);

    if (DEBUG) System.out.println("CC2420: Oscillator Off.");
    setMode(MODE_POWER_OFF);
    // Reset state
    rxPacket = false;
    setFIFOP(false);
  }

  public void setRFListener(RFListener rf) {
    listener = rf;
  }

  public void setVRegOn(boolean on) {
    if(this.on == on) return;

    if(on) {
      // 0.6ms maximum vreg startup from datasheet pg 13
      cpu.scheduleTimeEventMillis(vregEvent, 0.1);
      if (DEBUG) System.out.println(getName() + ": Scheduling vregEvent at: cyc = " + cpu.cycles +
         " target: " + vregEvent.getTime() + " current: " + cpu.getTime());
    }else{
      this.on = on;
      setState(STATE_VREG_OFF);
    }
    //this.on = on;
  }

  public void setChipSelect(boolean select) {
    chipSelect = select;
    if (!chipSelect) {
      state = WAITING;
    }

    //if (DEBUG) {
    //  System.out.println("CC2420: setting chipSelect: " + chipSelect);
    //}
  }

  public void setCCAPort(IOPort port, int pin) {
    ccaPort = port;
    ccaPin = pin;
  }

  public void setFIFOPPort(IOPort port, int pin) {
    fifopPort = port;
    fifopPin = pin;
  }

  public void setFIFOPort(IOPort port, int pin) {
    fifoPort = port;
    fifoPin = pin;
  }

  public void setSFDPort(IOPort port, int pin) {
    sfdPort = port;
    sfdPin = pin;
  }


  // -------------------------------------------------------------------
  // Methods for accessing and writing to registers, etc from outside
  // And for receiveing data
  // -------------------------------------------------------------------

  public int getRegister(int register) {
    return registers[register];
  }

  public void setRegister(int register, int data) {
    registers[register] = data;
  }

  private void flushRX() {
    if (DEBUG) {
      System.out.println("CC2420: Flushing RX len = " + rxfifo_len);
    }
    rxPacket = false;
    rxfifo_read_pos = 0;
    rxfifo_write_pos = 0;
    rxfifo_len = 0;
    setClear(true);
    setSFD(false);
    setFIFOP(false);
  }

  // TODO: update any pins here?
  private void flushTX() {
    txCursor = 0;
  }
  
  public void setClear(boolean clear) {
    cca = clear;
    setCCA(clear);
    if (DEBUG) System.out.println("CC2420: CCA: " + clear);
  }

  public void setSFD(boolean sfd) {
    if (DEBUG) System.out.println("SFD: " + sfd);
    sfdPort.setPinState(sfdPin, sfd ? 1 : 0);
  }

  public void setCCA(boolean cca) {
    if( (registers[REG_IOCFG0] & CCA_POLARITY) == CCA_POLARITY)
      ccaPort.setPinState(ccaPin, cca ? 0 : 1);
    else
      ccaPort.setPinState(ccaPin, cca ? 1 : 0);
  }

  public void setFIFOP(boolean fifop) {
    if( (registers[REG_IOCFG0] & FIFOP_POLARITY) == FIFOP_POLARITY) {
      fifopPort.setPinState(fifopPin, fifop ? 0 : 1);
    } else {
      fifopPort.setPinState(fifopPin, fifop ? 1 : 0);
    }
  }

  public void setFIFO(boolean fifo) {
    fifoPort.setPinState(fifoPin, fifo ? 1 : 0);
  }

  public void setRxOverflow() {
    if (DEBUG) System.out.println("CC2420: RXFIFO Overflow! Read Pos: " + rxfifo_read_pos + " Write Pos: " + rxfifo_write_pos);
    setFIFOP(true);
    setFIFO(false);
  }

  public String getName() {
    return "CC2420";
  }

  public int getModeMax() {
    return MODE_MAX;
  }
  
  public String chipinfo() {
    return " VREG_ON: " + on +
    "\n OSC_Stable: " + ((status & STATUS_XOSC16M_STABLE) > 0) + 
    "\n RSSI_Valid: " + ((status & STATUS_RSSI_VALID) > 0) +
    "\n";
  }

} // CC2420
