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
 * $Id: Beeper.java,v 1.3 2007/10/21 21:17:33 nfi Exp $
 *
 * -----------------------------------------------------------------
 *
 * Beeper
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2007/10/21 21:17:33 $
 *           $Revision: 1.3 $
 */

package se.sics.mspsim.chip;
import javax.sound.sampled.*;
import se.sics.mspsim.core.*;

/**
 * Beeper for the esb...
 */
public class Beeper extends IOUnit {

  private SourceDataLine dataLine;
  private FloatControl volume;

  public static final int SAMPLE_RATE = 44000;

  public static final int FRQ_1 = 2200;

  public static final int WAVE_LEN = (SAMPLE_RATE / FRQ_1);

  // One second of the sound in buffer
  byte[] buffer = new byte[WAVE_LEN];
  byte[] quiet = new byte[WAVE_LEN];

  int beepCtr = 0;

  public Beeper() {
  	super(null, 0);
  	AudioFormat af = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
  	DataLine.Info dli =
  		new DataLine.Info(SourceDataLine.class, af, 16384);
  	try {
  		dataLine = (SourceDataLine) AudioSystem.getLine(dli);
  		if (dataLine == null)	{
  			System.out.println("DataLine: not existing...");
  		}	else {
  			dataLine.open(dataLine.getFormat(), 16384);
  			volume = (FloatControl)	dataLine.getControl(FloatControl.Type.MASTER_GAIN);
  		}
  	} catch (Exception e) {
  		System.out.println("Problem while  getting data line " + e);
  	}
    double f1 = 0;
    for (int i = 0, n = WAVE_LEN; i < n; i++) {
      f1 = Math.sin(i * 3.141592 * 2 / WAVE_LEN) * 40;
      f1 += Math.sin(i * 3.141592 * 4 / WAVE_LEN) * 30;
      buffer[i] = (byte) (f1);
    }

    if (dataLine != null) {
      dataLine.start();
    }
  }

  public void setVolue(int vol) {
  	volume.setValue(vol);
  }

  public void beepOn(boolean beep) {
    if (beep) {
      beepCtr = 7;
    }
  }

  public long ioTick(long cycles) {
    // Avoid blocking using timer...
  	if (dataLine != null) {
  		if (dataLine.available() > WAVE_LEN * 2) {
  			if (beepCtr > 0) {
  				dataLine.write(buffer, 0, WAVE_LEN);
  				beepCtr--;
  			} else {
  				dataLine.write(quiet, 0, WAVE_LEN);
  			}
  		}
  	}
  	return cycles + 1000;
  }


  public int read(int address, boolean word, long cycler) {
  	return 0;
  }

  public void write(int address, int data, boolean word, long cycler) {
  }

  public String getName() {
    return "Beeper";
  }

  // Nothing for interrupts...
  public void interruptServiced() {
  }

  public static void main(String[] args) {
  	Beeper beep = new Beeper();
  	while (true) {
  		beep.beepOn(true);
  		for (int i = 0, n = 1000; i < n; i++) {
  			beep.ioTick(0);
  		}
  		beep.beepOn(false);
  		for (int i = 0, n = 10000; i < n; i++) {
  			beep.ioTick(0);
  		}
  	}
  }

  public int getModeMax() {
    return 0;
  }
}
