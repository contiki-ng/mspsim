/**
 * Copyright (c) 2007-2011 Swedish Institute of Computer Science.
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
import se.sics.mspsim.util.ArrayFIFO;
import se.sics.mspsim.util.CCITT_CRC;
import se.sics.mspsim.util.Utils;

public class CC2420 extends Chip implements USARTListener, RFListener, RFSource {

  public enum Reg {
    SNOP, SXOSCON, STXCAL, SRXON, /* 0x00 */
    STXON, STXONCCA, SRFOFF, SXOSCOFF, /* 0x04 */
    SFLUSHRX, SFLUSHTX, SACK, SACKPEND, /* 0x08 */
    SRXDEC, STXENC, SAES, foo,   /* 0x0c */
    MAIN, MDMCTRL0, MDMCTRL1, RSSI, /* 0x10 */ 
    SYNCWORD, TXCTRL, RXCTRL0, RXCTRL1, /* 0x14 */
    FSCTRL, SECCTRL0, SECCTRL1, BATTMON, /* 0x18 */
    IOCFG0, IOCFG1, MANFIDL, MANFIDH, /* 0x1c */
    FSMTC, MANAND, MANOR, AGCCTRL, /* 0x20 */
    AGCTST0, AGCTST1, AGCTST2, FSTST0, /* 0x24 */
    FSTST1, FSTST2, FSTST3, RXBPFTST, /* 0x28 */
    FSMSTATE, ADCTST, DACTST, TOPTST,
    RESERVED, RES1, RES2, RES3,  /* 0x30 */
    RES4, RES5, RES6, RES7,
    RES8, RES9, RESa, RESb,
    RESc, RESd, TXFIFO, RXFIFO
  };

  public enum SpiState {
    WAITING, WRITE_REGISTER, READ_REGISTER, RAM_ACCESS,
    READ_RXFIFO, WRITE_TXFIFO
  };


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
  public static final int CCAMUX_CCA = 0;
  public static final int CCAMUX_XOSC16M_STABLE = 24;

  // MDMCTRO0 values
  public static final int ADR_DECODE = (1 << 11);
  public static final int ADR_AUTOCRC = (1 << 5);
  public static final int AUTOACK = (1 << 4);
  public static final int PREAMBLE_LENGTH = 0x0f;
  
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

  public static final int SHORT_ADDRESS = 2;
  public static final int LONG_ADDRESS = 3;

  
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
  public enum RadioState {
     VREG_OFF(-1),
     POWER_DOWN(0),
     IDLE(1),
     RX_CALIBRATE(2),
     RX_SFD_SEARCH(3),
     RX_WAIT(14),
     RX_FRAME(16),
     RX_OVERFLOW(17),
     TX_CALIBRATE(32),
     TX_PREAMBLE(34),
     TX_FRAME(37),
     TX_ACK_CALIBRATE(48),
     TX_ACK_PREAMBLE(49),
     TX_ACK(52),
     TX_UNDERFLOW(56);

     private final int state;
     RadioState(int stateNo) {
       state = stateNo;
     }

     public int getFSMState() {
       return state;
     }
  };
  
  // FCF High
  public static final int FRAME_TYPE = 0x07;
  public static final int SECURITY_ENABLED = (1<<3);
  public static final int FRAME_PENDING = (1<<4);
  public static final int ACK_REQUEST = (1<<5);
  public static final int INTRA_PAN = (1<<6);

  public static final int TYPE_BEACON_FRAME = 0x00;
  public static final int TYPE_DATA_FRAME = 0x01;
  public static final int TYPE_ACK_FRAME = 0x02;
  
  // FCF Low
  public static final int DESTINATION_ADDRESS_MODE = 0x30;
  public static final int SOURCE_ADDRESS_MODE = 0x3;

  // Position of SEQ-NO in ACK packet...
  public static final int ACK_SEQPOS = 3;
  
  private RadioState stateMachine = RadioState.VREG_OFF;

  // 802.15.4 symbol period in ms
  public static final double SYMBOL_PERIOD = 0.016; // 16 us

  // when reading registers this flag is set!
  public static final int FLAG_READ = 0x40;

  public static final int FLAG_RAM = 0x80;
  // When accessing RAM the second byte of the address contains
  // a flag indicating read/write
  public static final int FLAG_RAM_READ = 0x20;
  private static final int[] BC_ADDRESS = new int[] {0xff, 0xff};
  
  private SpiState state = SpiState.WAITING;
  private int pos;
  private int address;
  private int shrPos;
  private int txfifoPos;
  private boolean txfifoFlush;	// TXFIFO is automatically flushed on next write
  private int rxfifoReadLeft; // number of bytes left to read from current packet
  private int rxlen;
  private int rxread;
  private int zeroSymbols;
  private boolean ramRead = false;

  /* RSSI is an externally set value of the RSSI for this CC2420 */
  /* low RSSI => CCA = true in normal mode */

  private int rssi = -100;
  private static int RSSI_OFFSET = -45; /* cc2420 datasheet */
  /* current CCA value */
  private boolean cca = false;

  /* FIFOP Threshold */
  private int fifopThr = 64;

  /* if autoack is configured or if */
  private boolean autoAck = false;
  private boolean shouldAck = false;
  private boolean addressDecode = false;
  private boolean ackRequest = false;
  private boolean autoCRC = false;

  // Data from last received packet
  private int dsn = 0;
  private int fcf0 = 0;
  private int fcf1 = 0;
  private int frameType = 0;
  private boolean crcOk = false;
  
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
  /* fifoP state */
  private boolean fifoP = false;

  private IOPort fifoPort = null;
  private int fifoPin;

  private IOPort sfdPort = null;
  private int sfdPin;

  private int txCursor;
  private RFListener listener;
  private boolean on;

  private TimeEvent oscillatorEvent = new TimeEvent(0, "CC2420 OSC") {
    public void execute(long t) {
      status |= STATUS_XOSC16M_STABLE;
      if(DEBUG) log("Oscillator Stable Event.");
      setState(RadioState.IDLE);
      if( (registers[REG_IOCFG1] & CCAMUX) == CCAMUX_XOSC16M_STABLE) {
        updateCCA();
      } else {
        if(DEBUG) log("CCAMUX != CCA_XOSC16M_STABLE! Not raising CCA");
      }
    }
  };

  private TimeEvent vregEvent = new TimeEvent(0, "CC2420 VREG") {
    public void execute(long t) {
      if(DEBUG) log("VREG Started at: " + t + " cyc: " +
          cpu.cycles + " " + getTime());
      on = true;
      setState(RadioState.POWER_DOWN);
      updateCCA();
    }
  };

  private TimeEvent sendEvent = new TimeEvent(0, "CC2420 Send") {
    public void execute(long t) {
      txNext();
    }
  };

  private TimeEvent ackEvent = new TimeEvent(0, "CC2420 Ack") {
      public void execute(long t) {
        ackNext();
      }
    };
  
  private TimeEvent shrEvent = new TimeEvent(0, "CC2420 SHR") {
    public void execute(long t) {
      shrNext();
    }
  };

  private TimeEvent symbolEvent = new TimeEvent(0, "CC2420 Symbol") {
    public void execute(long t) {
      switch(stateMachine) {
      case RX_CALIBRATE:
        setState(RadioState.RX_SFD_SEARCH);
        break;
        /* this will be called 8 symbols after first SFD_SEARCH */
      case RX_SFD_SEARCH:
        status |= STATUS_RSSI_VALID;
        updateCCA();
        break;

      case TX_CALIBRATE:
        setState(RadioState.TX_PREAMBLE);
        break;

      case RX_WAIT:
        setState(RadioState.RX_SFD_SEARCH);
        break;

      case TX_ACK_CALIBRATE:
          setState(RadioState.TX_ACK_PREAMBLE);
          break;
      }
    }
  };
  private boolean currentSFD;
  private boolean currentFIFO;
  private boolean overflow = false;
  private boolean frameRejected = false;

  public interface StateListener {
    public void newState(RadioState state);
  }

  private StateListener stateListener = null;
  private int ackPos;
  /* type = 2 (ACK), third byte needs to be sequence number... */
  private int[] ackBuf = {0x05, 0x02, 0x00, 0x00, 0x00, 0x00};
  private boolean ackFramePending = false;
  private CCITT_CRC rxCrc = new CCITT_CRC();
  private CCITT_CRC txCrc = new CCITT_CRC();

  private ArrayFIFO rxFIFO;
  
  public void setStateListener(StateListener listener) {
    stateListener = listener;
  }

  public RadioState getState() {
      return stateMachine;
  }

  public CC2420(MSP430Core cpu) {
      super("CC2420", "Radio", cpu);
      rxFIFO = new ArrayFIFO("RXFIFO", memory, RAM_RXFIFO, 128);
      
    registers[REG_SNOP] = 0;
    registers[REG_TXCTRL] = 0xa0ff;
    setModeNames(MODE_NAMES);
    setMode(MODE_POWER_OFF);
    fifoP = false;
    rxFIFO.reset();
    overflow = false;
    reset();
  }
  
  private void reset() {
      setReg(REG_MDMCTRL0, 0x0ae2);
  }
  
  private boolean setState(RadioState state) {
    if(DEBUG) log("State transition from " + stateMachine + " to " + state);
    stateMachine = state;
    /* write to FSM state register */
    registers[REG_FSMSTATE] = state.getFSMState();

    switch(stateMachine) {

    case VREG_OFF:
      if (DEBUG) log("VREG Off.");
      flushRX();
      flushTX();
      status &= ~(STATUS_RSSI_VALID | STATUS_XOSC16M_STABLE);
      crcOk = false;
      reset();
      setMode(MODE_POWER_OFF);
      updateCCA();
      break;

    case POWER_DOWN:
      rxFIFO.reset();
      status &= ~(STATUS_RSSI_VALID | STATUS_XOSC16M_STABLE);
      crcOk = false;
      reset();
      setMode(MODE_POWER_OFF);
      updateCCA();
      break;

    case RX_CALIBRATE:
      /* should be 12 according to specification */
      setSymbolEvent(12);
      setMode(MODE_RX_ON);
      break;
    case RX_SFD_SEARCH:
      zeroSymbols = 0;
      /* eight symbols after first SFD search RSSI will be valid */
      if ((status & STATUS_RSSI_VALID) == 0) {
          setSymbolEvent(8);
      }
//      status |= STATUS_RSSI_VALID;
      updateCCA();
      setMode(MODE_RX_ON);
      break;

    case TX_CALIBRATE:
      /* 12 symbols calibration, and one byte's wait since we deliver immediately
       * to listener when after calibration?
       */
      setSymbolEvent(12 + 2);
      setMode(MODE_TXRX_ON);
      break;

    case TX_PREAMBLE:
      shrPos = 0;
      SHR[0] = 0;
      SHR[1] = 0;
      SHR[2] = 0;
      SHR[3] = 0;
      SHR[4] = 0x7A;
      shrNext();
      break;

    case TX_FRAME:
      txfifoPos = 0;
      // Reset CRC ok flag to disable software acknowledgments until next received packet 
      crcOk = false;
      txNext();
      break;

    case RX_WAIT:
      setSymbolEvent(8);
      setMode(MODE_RX_ON);
      break;
      
    case IDLE:
      status &= ~STATUS_RSSI_VALID;
      setMode(MODE_TXRX_OFF);
      updateCCA();
      break;
      
    case TX_ACK_CALIBRATE:
        /* TX active during ACK + NOTE: we ignore the SFD when receiveing full packets so
         * we need to add another extra 2 symbols here to get a correct timing */
        status |= STATUS_TX_ACTIVE;
        setSymbolEvent(12 + 2 + 2);
        setMode(MODE_TXRX_ON);
      break;
    case TX_ACK_PREAMBLE:
        /* same as normal preamble ?? */
        shrPos = 0;
        SHR[0] = 0;
        SHR[1] = 0;
        SHR[2] = 0;
        SHR[3] = 0;
        SHR[4] = 0x7A;
        shrNext();
        break;
    case TX_ACK:
        ackPos = 0;
        // Reset CRC ok flag to disable software acknowledgments until next received packet 
        crcOk = false;
        ackNext();
        break;
    case RX_FRAME:
        /* mark position of frame start - for rejecting when address is wrong */
        rxFIFO.mark();
        rxread = 0;
        frameRejected = false;
        shouldAck = false;
        crcOk = false;
        break;
    }

    /* Notify state listener */
    if (stateListener != null) {
        stateListener.newState(stateMachine);
    }
    stateChanged(stateMachine.state);

    return true;
  }

  private void rejectFrame() {
      // Immediately jump to SFD Search again... something more???
      /* reset state */
      rxFIFO.restore();
      setSFD(false);
      setFIFO(rxFIFO.length() > 0);
      frameRejected = true;
  }
  
  /* variables for the address recognition */
  int destinationAddressMode = 0;
  boolean decodeAddress = false;
  /* Receive a byte from the radio medium
   * @see se.sics.mspsim.chip.RFListener#receivedByte(byte)
   */
  public void receivedByte(byte data) {
      // Received a byte from the "air"

      if (DEBUG)
        log("RF Byte received: " + Utils.hex8(data) + " state: " + stateMachine + " noZeroes: " + zeroSymbols +
              ((stateMachine == RadioState.RX_SFD_SEARCH || stateMachine == RadioState.RX_FRAME) ? "" : " *** Ignored"));

      if(stateMachine == RadioState.RX_SFD_SEARCH) {
          // Look for the preamble (4 zero bytes) followed by the SFD byte 0x7A
          if(data == 0) {
              // Count zero bytes
              zeroSymbols++;
          } else if(zeroSymbols >= 4 && data == 0x7A) {
              // If the received byte is !zero, we have counted 4 zero bytes prior to this one,
              // and the current received byte == 0x7A (SFD), we're in sync.
              // In RX mode, SFD goes high when the SFD is received
              setSFD(true);
              if (DEBUG) log("RX: Preamble/SFD Synchronized.");
              setState(RadioState.RX_FRAME);
          } else {
              /* if not four zeros and 0x7A then no zeroes... */
              zeroSymbols = 0;
          }

      } else if(stateMachine == RadioState.RX_FRAME) {
          if (overflow) {
              /* if the CC2420 RX FIFO is in overflow - it needs a flush before receiving again */
          } else if(rxFIFO.isFull()) {
              setRxOverflow();
          } else {
              if (!frameRejected) {
                  rxFIFO.write(data);
                  if (rxread == 0) {
                      rxCrc.setCRC(0);
                      rxlen = data & 0xff;
                      //System.out.println("Starting to get packet at: " + rxfifoWritePos + " len = " + rxlen);
                      decodeAddress = addressDecode;
                      if (DEBUG) log("RX: Start frame length " + rxlen);
                      // FIFO pin goes high after length byte is written to RXFIFO
                      setFIFO(true);
                  } else if (rxread < rxlen - 1) {
                      /* As long as we are not in the length or FCF (CRC) we count CRC */
                      rxCrc.addBitrev(data & 0xff);
                      if (rxread == 1) {
                          fcf0 = data & 0xff;
                          frameType = fcf0 & FRAME_TYPE;
                      } else if (rxread == 2) {
                          fcf1 = data & 0xff;
                          if (frameType == TYPE_DATA_FRAME) {
                              ackRequest = (fcf0 & ACK_REQUEST) > 0;
                              destinationAddressMode = (fcf1 >> 2) & 3;
                              /* check this !!! */
                              if (addressDecode && destinationAddressMode != LONG_ADDRESS &&
                                      destinationAddressMode != SHORT_ADDRESS) {
                                  rejectFrame();
                              }
                          } else if (frameType == TYPE_BEACON_FRAME ||
                                  frameType == TYPE_ACK_FRAME){
                              decodeAddress = false;
                              ackRequest = false;
                          } else if (addressDecode) {
                              /* illegal frame when decoding address... */
                              rejectFrame();
                          }
                      } else if (rxread == 3) {
                          // save data sequence number
                          dsn = data & 0xff;
                      } else if (decodeAddress) {
                          boolean flushPacket = false;
                          /* here we decode the address !!! */
                          if (destinationAddressMode == LONG_ADDRESS && rxread == 8 + 5) {
                              /* here we need to check that this address is correct compared to the stored address */
                              flushPacket = !rxFIFO.tailEquals(memory, RAM_IEEEADDR, 8);
                              flushPacket |= !rxFIFO.tailEquals(memory, RAM_PANID, 2, 8)
                                      && !rxFIFO.tailEquals(BC_ADDRESS, 0, 2, 8);
                              decodeAddress = false;
                          } else if (destinationAddressMode == SHORT_ADDRESS && rxread == 2 + 5){
                              /* should check short address */
                              flushPacket = !rxFIFO.tailEquals(BC_ADDRESS, 0, 2)
                                      && !rxFIFO.tailEquals(memory, RAM_SHORTADDR, 2);
                              flushPacket |= !rxFIFO.tailEquals(memory, RAM_PANID, 2, 2)
                                      && !rxFIFO.tailEquals(BC_ADDRESS, 0, 2, 2);
                              decodeAddress = false;
                          }
                          if (flushPacket) {
                              rejectFrame();
                          }
                      }
                  }

                  /* In RX mode, FIFOP goes high when the size of the first enqueued packet exceeds
                   * the programmable threshold and address recognition isn't ongoing */ 
                  if(fifoP == false
                          && rxFIFO.length() <= rxlen + 1
                          && !decodeAddress && !frameRejected
                          && rxFIFO.length() > fifopThr) {
                      setFIFOP(true);
                      if (DEBUG) log("RX: FIFOP Threshold reached - setting FIFOP");
                  }
              }

              if (rxread++ == rxlen) {
                  if (frameRejected) {
                      log("Frame rejected - setting SFD to false and RXWAIT\n");
                      setSFD(false);
                      setState(RadioState.RX_WAIT);
                      return;
                  }
                  // In RX mode, FIFOP goes high, if threshold is higher than frame length....

                  // Here we check the CRC of the packet!
                  //System.out.println("Reading from " + ((rxfifoWritePos + 128 - 2) & 127));
                  int crc = rxFIFO.get(-2) << 8;
                  crc += rxFIFO.get(-1); //memory[RAM_RXFIFO + ((rxfifoWritePos + 128 - 1) & 127)];

                  crcOk = crc == rxCrc.getCRCBitrev();
                  if (DEBUG && !crcOk) {
                      log("CRC not OK: recv:" + Utils.hex16(crc) + " calc: " + Utils.hex16(rxCrc.getCRCBitrev()));
                  }
                  // Should take a RSSI value as input or use a set-RSSI value...
                  rxFIFO.set(-2, registers[REG_RSSI] & 0xff); 
                  rxFIFO.set(-1, 37 | (crcOk ? 0x80 : 0));
                  //          memory[RAM_RXFIFO + ((rxfifoWritePos + 128 - 2) & 127)] = ;
                  //          // Set CRC ok and add a correlation - TODO: fix better correlation value!!!
                  //          memory[RAM_RXFIFO + ((rxfifoWritePos + 128 - 1) & 127)] = 37 |
                  //              (crcOk ? 0x80 : 0);

                  /* set FIFOP only if this is the first received packet - e.g. if rxfifoLen is at most rxlen + 1
                   * TODO: check what happens when rxfifoLen < rxlen - e.g we have been reading before FIFOP */
                  if (rxFIFO.length() <= rxlen + 1) {
                      setFIFOP(true);
                  } else {
                      if (DEBUG) log("Did not set FIFOP rxfifoLen: " + rxFIFO.length() + " rxlen: " + rxlen);
                  }
                  setSFD(false);
                  if (DEBUG) log("RX: Complete: packetStart: " + rxFIFO.stateToString());

                  /* if either manual ack request (shouldAck) or autoack + ACK_REQ on package do ack! */
                  /* Autoack-mode + good CRC => autoack */
                  if (((autoAck && ackRequest) || shouldAck) && crcOk) {
                      setState(RadioState.TX_ACK_CALIBRATE);
                  } else {
                      setState(RadioState.RX_WAIT);
                  }
              }
          }
      }
  }

  private void setReg(int address, int data) {
      int oldValue = registers[address];
      registers[address] = data;
      switch(address) {
      case REG_IOCFG0:
          fifopThr = data & FIFOP_THR;
          if (DEBUG) log("IOCFG0: " + registers[address]);
          break;
      case REG_IOCFG1:
          if (DEBUG)
            log("IOCFG1: SFDMUX "
                          + ((registers[address] & SFDMUX) >> SFDMUX)
                          + " CCAMUX: " + (registers[address] & CCAMUX));
//        if( (registers[address] & CCAMUX) == CCA_CCA)
//          setCCA(false);
          updateCCA();
          break;
      case REG_MDMCTRL0:
          addressDecode = (data & ADR_DECODE) != 0;
          autoCRC = (data & ADR_AUTOCRC) != 0;
          autoAck = (data & AUTOACK) != 0;
          break;
      case REG_FSCTRL:
          if (cl != null) {
              updateActiveFrequency();
              cl.changedChannel(activeChannel);
          }
        break;
      }
      configurationChanged(address, oldValue, data);
  }
    
  public void dataReceived(USARTSource source, int data) {
    int oldStatus = status;
    if (DEBUG) {
      log("byte received: " + Utils.hex8(data) +
          " (" + ((data >= ' ' && data <= 'Z') ? (char) data : '.') + ')' +
          " CS: " + chipSelect + " SPI state: " + state + " StateMachine: " + stateMachine);
    }

    if (!chipSelect) {
      // Chip is not selected

    } else if (stateMachine != RadioState.VREG_OFF) {
      switch(state) {
      case WAITING:
        if ((data & FLAG_READ) != 0) {
          state = SpiState.READ_REGISTER;
        } else {
          state = SpiState.WRITE_REGISTER;
        }
        if ((data & FLAG_RAM) != 0) {
          state = SpiState.RAM_ACCESS;
          address = data & 0x7f;
        } else {
          // The register address
          address = data & 0x3f;

          if (address == REG_RXFIFO) {
            // check read/write???
            //          log("Reading RXFIFO!!!");
            state = SpiState.READ_RXFIFO;
          } else if (address == REG_TXFIFO) {
            state = SpiState.WRITE_TXFIFO;
          }
        }
        if (data < 0x0f) {
          strobe(data);
          state = SpiState.WAITING;
        }
        pos = 0;
        // Assuming that the status always is sent back???
        //source.byteReceived(status);
        break;
                
      case WRITE_REGISTER:
        if (pos == 0) {
          source.byteReceived(registers[address] >> 8);
          // set the high bits
          registers[address] = (registers[address] & 0xff) | (data << 8);
          pos = 1;
        } else {
          source.byteReceived(registers[address] & 0xff);
          // set the low bits
          registers[address] = (registers[address] & 0xff00) | data;

          if (DEBUG) {
            log("wrote to " + Utils.hex8(address) + " = "
                + registers[address]);
          }
          data = registers[address];
          setReg(address, data);
          /* register written - go back to wating... */
          state = SpiState.WAITING;
        }
        break;
      case READ_REGISTER:
        if (pos == 0) {
          source.byteReceived(registers[address] >> 8);
          pos = 1;
        } else {
          source.byteReceived(registers[address] & 0xff);
          if (DEBUG) {
            log("read from " + Utils.hex8(address) + " = "
                + registers[address]);
          }
          state = SpiState.WAITING;
        }
        return;
        //break;
      case READ_RXFIFO: {
          int fifoData = rxFIFO.read(); 
          if (DEBUG) log("RXFIFO READ: " + rxFIFO.stateToString());
          source.byteReceived(fifoData);

          /* first check and clear FIFOP - since we now have read a byte! */
          if (fifoP && !overflow) {
              /* FIFOP is lowered when rxFIFO is lower than or equal to fifopThr */
              if(rxFIFO.length() <= fifopThr) {
                  if (DEBUG) log("*** FIFOP cleared at: " + rxFIFO.stateToString());
                  setFIFOP(false);
              }
          }

          /* initiate read of another packet - update some variables to keep track of packet reading... */
          if (rxfifoReadLeft == 0) {
              rxfifoReadLeft = fifoData;
              if (DEBUG) log("Init read of packet - len: " + rxfifoReadLeft +
                      " fifo: " + rxFIFO.stateToString());
          } else if (--rxfifoReadLeft == 0) {
              /* check if we have another packet in buffer */
              if (rxFIFO.length() > 0) {
                  /* check if the packet is complete or longer than fifopThr */
                  if (rxFIFO.length() > rxFIFO.peek(0) ||
                          (rxFIFO.length() > fifopThr && !decodeAddress && !frameRejected)) {
                      if (DEBUG) log("More in FIFO - FIFOP = 1! plen: " + rxFIFO.stateToString());
                      if (!overflow) setFIFOP(true);
                  }
              }
          }
          // Set the FIFO pin low if there are no more bytes available in the RXFIFO.
          if (rxFIFO.length() == 0) {
              if (DEBUG) log("Setting FIFO to low (buffer empty)");
              setFIFO(false);
          }
      }
      return; /* avoid returning the status byte */
      case WRITE_TXFIFO:
        if(txfifoFlush) {
          txCursor = 0;
          txfifoFlush = false;
        }
        if (DEBUG) log("Writing data: " + data + " to tx: " + txCursor);

        if(txCursor == 0) {
          if ((data & 0xff) > 127) {
            logger.warning(this, "CC2420: Warning - packet size too large: " + (data & 0xff));
          }
        } else if (txCursor > 127) {
          logger.warning(this, "CC2420: Warning - TX Cursor wrapped");
          txCursor = 0;
        }
        memory[RAM_TXFIFO + txCursor] = data & 0xff;
        txCursor++;
        if (sendEvents) {
          sendEvent("WRITE_TXFIFO", null);
        }
        break;
      case RAM_ACCESS:
        if (pos == 0) {
          address |= (data << 1) & 0x180;
          ramRead = (data & 0x20) != 0;
          if (DEBUG) {
            log("Address: " + Utils.hex16(address) +
                " read: " + ramRead);
          }
          pos++;
        } else {
          if (!ramRead) {
            memory[address++] = data;
            if (address >= 0x180) {
              logger.warning(this, "CC2420: Warning - RAM position too big - wrapping!");
              address = 0;
            }
            if (DEBUG && address == RAM_PANID + 2) {
              log("Pan ID set to: 0x" +
                  Utils.hex8(memory[RAM_PANID]) +
                  Utils.hex8(memory[RAM_PANID + 1]));
            }
          } else {
            //log("Read RAM Addr: " + address + " Data: " + memory[address]);  
            source.byteReceived(memory[address++]);
            if (address >= 0x180) {
              logger.warning(this, "CC2420: Warning - RAM position too big - wrapping!");
              address = 0;
            }      
            return;
          }
        }
        break;
      }
      source.byteReceived(oldStatus);  
    } else {
        /* No VREG but chip select */
        source.byteReceived(0);
        logw("**** Warning - writing to CC2420 when VREG is off!!!");
    }
  }

  // Needs to get information about when it is possible to write
  // next data...
  private void strobe(int data) {
    // Resets, on/off of different things...
    if (DEBUG) {
      log("Strobe on: " + Utils.hex8(data) + " => " + Reg.values()[data]);
    }

    if( (stateMachine == RadioState.POWER_DOWN) && (data != REG_SXOSCON) ) {
      if (DEBUG) log("Got command strobe: " + data + " in POWER_DOWN.  Ignoring.");
      return;
    }

    switch (data) {
    case REG_SNOP:
      if (DEBUG) log("SNOP => " + Utils.hex8(status) + " at " + cpu.cycles);
      break;
    case REG_SRXON:
      if(stateMachine == RadioState.IDLE) {
        setState(RadioState.RX_CALIBRATE);
        //updateActiveFrequency();
        if (DEBUG) {
            log("Strobe RX-ON!!!");
        }
      } else {
        if (DEBUG) log("WARNING: SRXON when not IDLE");
      }

      break;
    case REG_SRFOFF:
      if (DEBUG) {
        log("Strobe RXTX-OFF!!! at " + cpu.cycles);
        if (stateMachine == RadioState.TX_ACK ||
              stateMachine == RadioState.TX_FRAME ||
              stateMachine == RadioState.RX_FRAME) {
          log("WARNING: turning off RXTX during " + stateMachine);
        }
      }
      setState(RadioState.IDLE);
      break;
    case REG_STXON:
      // State transition valid from IDLE state or all RX states
      if( (stateMachine == RadioState.IDLE) || 
          (stateMachine == RadioState.RX_CALIBRATE) ||
          (stateMachine == RadioState.RX_SFD_SEARCH) ||
          (stateMachine == RadioState.RX_FRAME) ||
          (stateMachine == RadioState.RX_OVERFLOW) ||
          (stateMachine == RadioState.RX_WAIT)) {
        status |= STATUS_TX_ACTIVE;
        setState(RadioState.TX_CALIBRATE);
        if (sendEvents) {
          sendEvent("STXON", null);
        }
        // Starting up TX subsystem - indicate that we are in TX mode!
        if (DEBUG) log("Strobe STXON - transmit on! at " + cpu.cycles);
      }
      break;
    case REG_STXONCCA:
      // Only valid from all RX states,
      // since CCA requires ??(look this up) receive symbol periods to be valid
      if( (stateMachine == RadioState.RX_CALIBRATE) ||
          (stateMachine == RadioState.RX_SFD_SEARCH) ||
          (stateMachine == RadioState.RX_FRAME) ||
          (stateMachine == RadioState.RX_OVERFLOW) ||
          (stateMachine == RadioState.RX_WAIT)) {
        
        if (sendEvents) {
          sendEvent("STXON_CCA", null);
        }
        
        if(cca) {
          status |= STATUS_TX_ACTIVE;
          setState(RadioState.TX_CALIBRATE);
          if (DEBUG) log("Strobe STXONCCA - transmit on! at " + cpu.cycles);
        }else{
          if (DEBUG) log("STXONCCA Ignored, CCA false");
        }
      }
      break;
    case REG_SFLUSHRX:
      flushRX();
      break;
    case REG_SFLUSHTX:
      if (DEBUG) log("Flushing TXFIFO");
      flushTX();
      break;
    case REG_SXOSCON:
      //log("Strobe Oscillator On");
      startOscillator();
      break;
    case REG_SXOSCOFF:
      //log("Strobe Oscillator Off");
      stopOscillator();
      break;
    case REG_SACK:
    case REG_SACKPEND:
        // Set the frame pending flag for all future autoack based on SACK/SACKPEND
        ackFramePending = data == REG_SACKPEND;
        if (stateMachine == RadioState.RX_FRAME) {
            shouldAck = true;
        } else if (crcOk) {
            setState(RadioState.TX_ACK_CALIBRATE);
        }
        break;
    default:
      if (DEBUG) {
        log("Unknown strobe command: " + data);
      }
    break;
    }
  }

  private void shrNext() {
    if(shrPos == 5) {
      // Set SFD high
      setSFD(true);

      if (stateMachine == RadioState.TX_PREAMBLE) {
          setState(RadioState.TX_FRAME);
      } else if (stateMachine == RadioState.TX_ACK_PREAMBLE) {
          setState(RadioState.TX_ACK);
      } else {
          log("Can not move to TX_FRAME or TX_ACK after preamble since radio is in wrong mode: " +
                  stateMachine);
      }
    } else {
      if (listener != null) {
        if (DEBUG) log("transmitting byte: " + Utils.hex8(SHR[shrPos]));
        listener.receivedByte(SHR[shrPos]);
      }
      shrPos++;
      cpu.scheduleTimeEventMillis(shrEvent, SYMBOL_PERIOD * 2);
    }
  }

  private void txNext() {
    if(txfifoPos <= memory[RAM_TXFIFO]) {
      int len = memory[RAM_TXFIFO] & 0xff;
      if (txfifoPos == len - 1) {
          txCrc.setCRC(0);
          for (int i = 1; i < len - 1; i++) {
            txCrc.addBitrev(memory[RAM_TXFIFO + i] & 0xff);
          }
          memory[RAM_TXFIFO + len - 1] = txCrc.getCRCHi();
          memory[RAM_TXFIFO + len] = txCrc.getCRCLow();
      }
      if (txfifoPos > 0x7f) {
        log("Warning: packet size too large - repeating packet bytes txfifoPos: " + txfifoPos);
      }
      if (listener != null) {
        if (DEBUG) log("transmitting byte: " + Utils.hex8(memory[RAM_TXFIFO + (txfifoPos & 0x7f)] & 0xFF));
        listener.receivedByte((byte)(memory[RAM_TXFIFO + (txfifoPos & 0x7f)] & 0xFF));
      }
      txfifoPos++;
      // Two symbol periods to send a byte...
      cpu.scheduleTimeEventMillis(sendEvent, SYMBOL_PERIOD * 2);
    } else {
      if (DEBUG) log("Completed Transmission.");
      status &= ~STATUS_TX_ACTIVE;
      setSFD(false);
      if (overflow) {
        /* TODO: is it going back to overflow here ?=? */
        setState(RadioState.RX_OVERFLOW);
      } else {
        setState(RadioState.RX_CALIBRATE);
      }
      /* Back to RX ON */
      setMode(MODE_RX_ON);
      txfifoFlush = true;
    }
  }

  private void ackNext() {
      if (ackPos < ackBuf.length) {
          if(ackPos == 0) {
              txCrc.setCRC(0);
              if (ackFramePending) {
                  ackBuf[1] |= FRAME_PENDING;
              } else {
                  ackBuf[1] &= ~FRAME_PENDING;
              }
              // set dsn
              ackBuf[3] = dsn;
              int len = 4;
              for (int i = 1; i < len; i++) {
                  txCrc.addBitrev(ackBuf[i] & 0xff);
              }
              ackBuf[4] = txCrc.getCRCHi();
              ackBuf[5] = txCrc.getCRCLow();
          }
          if (listener != null) {
              if (DEBUG) log("transmitting byte: " + Utils.hex8(memory[RAM_TXFIFO + (txfifoPos & 0x7f)] & 0xFF));

              listener.receivedByte((byte)(ackBuf[ackPos] & 0xFF));
          }
          ackPos++;
          // Two symbol periods to send a byte...
          cpu.scheduleTimeEventMillis(ackEvent, SYMBOL_PERIOD * 2);
      } else {
          if (DEBUG) log("Completed Transmission of ACK.");
          status &= ~STATUS_TX_ACTIVE;
          setSFD(false);
          setState(RadioState.RX_CALIBRATE);
          /* Back to RX ON */
          setMode(MODE_RX_ON);
      }
  }


  private void setSymbolEvent(int symbols) {
    double period = SYMBOL_PERIOD * symbols;
    cpu.scheduleTimeEventMillis(symbolEvent, period);
    //log("Set Symbol event: " + period);
  }

  private void startOscillator() {
    // 1ms crystal startup from datasheet pg12
    cpu.scheduleTimeEventMillis(oscillatorEvent, 1);
  }

  private void stopOscillator() {
    status &= ~STATUS_XOSC16M_STABLE;
    setState(RadioState.POWER_DOWN);
    if (DEBUG) log("Oscillator Off.");
    // Reset state
    setFIFOP(false);
  }

  private void flushRX() {
    if (DEBUG) {
      log("Flushing RX len = " + rxFIFO.length());
    }
    rxFIFO.reset();
    setSFD(false);
    setFIFOP(false);
    setFIFO(false);
    overflow = false;
    /* goto RX Calibrate */
    if( (stateMachine == RadioState.RX_CALIBRATE) ||
        (stateMachine == RadioState.RX_SFD_SEARCH) ||
        (stateMachine == RadioState.RX_FRAME) ||
        (stateMachine == RadioState.RX_OVERFLOW) ||
        (stateMachine == RadioState.RX_WAIT)) {
      setState(RadioState.RX_SFD_SEARCH);
    }
  }

  // TODO: update any pins here?
  private void flushTX() {
    txCursor = 0;
  }
  
  private void updateCCA() {
    boolean oldCCA = cca;
    int ccaMux = (registers[REG_IOCFG1] & CCAMUX);

    if (ccaMux == CCAMUX_CCA) {
      /* If RSSI is less than -95 then we have CCA / clear channel! */
      cca = (status & STATUS_RSSI_VALID) > 0 && rssi < -95;
    } else if (ccaMux == CCAMUX_XOSC16M_STABLE) {
      cca = (status & STATUS_XOSC16M_STABLE) > 0;
    }
    
    if (cca != oldCCA) {
      setInternalCCA(cca);
    }
  }

  private void setInternalCCA(boolean clear) {
    setCCAPin(clear);
    if (DEBUG) log("Internal CCA: " + clear);
  }

  
  private void setSFD(boolean sfd) {
    if( (registers[REG_IOCFG0] & SFD_POLARITY) == SFD_POLARITY)
      sfdPort.setPinState(sfdPin, sfd ? 0 : 1);
    else 
      sfdPort.setPinState(sfdPin, sfd ? 1 : 0);
    currentSFD = sfd;
    if (DEBUG) log("SFD: " + sfd + "  " + cpu.cycles);
  }

  private void setCCAPin(boolean cca) {
    if (DEBUG) log("Setting CCA to: " + cca);
    if( (registers[REG_IOCFG0] & CCA_POLARITY) == CCA_POLARITY)
      ccaPort.setPinState(ccaPin, cca ? 0 : 1);
    else
      ccaPort.setPinState(ccaPin, cca ? 1 : 0);
  }

  private void setFIFOP(boolean fifop) {
    fifoP = fifop;
    if (DEBUG) log("Setting FIFOP to " + fifop);
    if( (registers[REG_IOCFG0] & FIFOP_POLARITY) == FIFOP_POLARITY) {
      fifopPort.setPinState(fifopPin, fifop ? 0 : 1);
    } else {
      fifopPort.setPinState(fifopPin, fifop ? 1 : 0);
    }
  }

  private void setFIFO(boolean fifo) {
    if (DEBUG) log("Setting FIFO to " + fifo);
    currentFIFO = fifo;
    fifoPort.setPinState(fifoPin, fifo ? 1 : 0);
  }

  
  private void setRxOverflow() {
    if (DEBUG) log("RXFIFO Overflow! Read Pos: " + rxFIFO.stateToString());
    setFIFOP(true);
    setFIFO(false);
    setSFD(false);
    overflow = true;
    shouldAck = false;
    setState(RadioState.RX_OVERFLOW);
  }
  
  
  /*****************************************************************************
   *  External APIs for simulators simulating Radio medium, etc.
   * 
   *****************************************************************************/
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

  public void setRSSI(int power) {
    if (DEBUG) log("external setRSSI to: " + power);
    if (power < -128) {
      power = -128;
    }
    rssi = power;
    registers[REG_RSSI] = power - RSSI_OFFSET;
    updateCCA();
  }

  public int getRSSI() {
    return rssi;
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


  public void setRFListener(RFListener rf) {
    listener = rf;
  }

  public interface ChannelListener {
    public void changedChannel(int channel);
  }
  private ChannelListener cl = null;
  public void setChannelListener(ChannelListener cl) {
    this.cl = cl;
  }

  public void setVRegOn(boolean newOn) {
    if(on == newOn) return;

    if(newOn) {
      // 0.6ms maximum vreg startup from datasheet pg 13
      // but Z1 platform does not work with 0.1 so trying with lower...
      cpu.scheduleTimeEventMillis(vregEvent, 0.05);
      if (DEBUG) log("Scheduling vregEvent at: cyc = " + cpu.cycles +
         " target: " + vregEvent.getTime() + " current: " + cpu.getTime());
    } else {
      on = false;
      setState(RadioState.VREG_OFF);
    }
  }

  public void setChipSelect(boolean select) {
    chipSelect = select;
    if (!chipSelect) {
      state = SpiState.WAITING;
    }

    if (DEBUG) {
      log("setting chipSelect: " + chipSelect);
    }
  }

  public boolean getChipSelect() {
    return chipSelect;
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

  /*****************************************************************************
   * Chip APIs
   *****************************************************************************/

  public int getModeMax() {
    return MODE_MAX;
  }

  private String getLongAddress() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        if ((i % 2 == 0) && i > 0) {
            sb.append(':');
        }
        sb.append(Utils.hex8(memory[RAM_IEEEADDR + 7 - i]));
      }
      return sb.toString();
  }

  public String info() {
    updateActiveFrequency();
    return " VREG_ON: " + on + "  Chip Select: " + chipSelect +
    "  OSC Stable: " + ((status & STATUS_XOSC16M_STABLE) > 0) + 
    "\n RSSI Valid: " + ((status & STATUS_RSSI_VALID) > 0) + "  CCA: " + cca +
    "\n FIFOP Polarity: " + ((registers[REG_IOCFG0] & FIFOP_POLARITY) == FIFOP_POLARITY) +
    "  FIFOP: " + fifoP + "  FIFO: " + currentFIFO + "  SFD: " + currentSFD + 
    "\n " + rxFIFO.stateToString() + " expPacketLen: " + rxlen +
    "\n Radio State: " + stateMachine + "  SPI State: " + state + 
    "\n AutoACK: " + autoAck + "  AddrDecode: " + addressDecode + "  AutoCRC: " + autoCRC +
    "\n PanID: 0x" + Utils.hex8(memory[RAM_PANID + 1]) + Utils.hex8(memory[RAM_PANID]) +
    "  ShortAddr: 0x" + Utils.hex8(memory[RAM_SHORTADDR + 1]) + Utils.hex8(memory[RAM_SHORTADDR]) +
    "  LongAddr: 0x" + getLongAddress() +
    "\n Channel: " + activeChannel +
    "\n FIFOP Threshold: " + fifopThr +
    "\n";
  }

  public void stateChanged(int state) {
  }

  /* return data in register at the correct position */
  public int getConfiguration(int parameter) {
      return registers[parameter];
  }
  
} // CC2420
