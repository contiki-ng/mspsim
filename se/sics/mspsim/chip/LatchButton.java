/**
 * Copyright (c) 2014, 3B Scientific GmbH.
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
 * Author: Björn Rennfanz <bjoern.rennfanz@3bscientific.com>
 * 
 */

package se.sics.mspsim.chip;

import se.sics.mspsim.core.EmulationLogger.WarningType;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.TimeEvent;

public class LatchButton extends Button implements PortListener 
{
	// The Operation modes of the beeper
	public static final int MODE_LATCH_OFF = 0;
	public static final int MODE_LATCH_ON = 1;
	private static final int MODE_MAX = MODE_LATCH_ON;
	
	// Private members
	private boolean latchEnabled;
	private IOPort latchPort;
	private int latchPin;

	private boolean pressReleaseSchedule;
	private Button that = this;
	private MSP430Core cpu;
	
	private TimeEvent pressRelease = new TimeEvent(0, "LatchButton Release Event")
	{
		// Event handler to release button
		public void execute(long t) 
		{
			// Call super class
			that.setPressed(false);
			
			// Reset event flag
			pressReleaseSchedule = false;
		}
	};
	
	public LatchButton(String id, MSP430Core cpu, IOPort buttonPort, int buttonPin, IOPort latchPort, int latchEnablePin, boolean polarity) 
	{
		// Call super class constructor
		super(id, cpu, buttonPort, buttonPin, polarity);
		setMode(MODE_LATCH_OFF);
		
		// Save members
		this.latchPin = latchEnablePin;
		this.latchPort = latchPort;
		this.cpu = cpu;

		// Attach event listener
		this.latchPort.addPortListener(this);
		
		// Release event
		pressReleaseSchedule = false;
	}

    @Override
    public int getModeMax() 
    {
        return MODE_MAX;
    }
    
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void portWrite(IOPort source, int data) 
	{
		// Check if source matches latch port
		if (source == latchPort)
		{
			// Latch is active low
			if ((data & (1 << latchPin)) == 0)
			{
				// Latch enableds
				latchEnabled = true;
				
				// Set mode to latch enabled
				setMode(MODE_LATCH_ON);
			}
			else
			{
				// Latch disabled
				latchEnabled = false;
				
				// Set mode to latch enabled
				setMode(MODE_LATCH_OFF);	
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setPressed(boolean isPressed) 
	{
		if ((latchEnabled) && (isPressed) && (!pressReleaseSchedule))
		{
			// Call super class
			super.setPressed(true);
			
			// Set flags and schedule release event
			cpu.scheduleTimeEventMillis(pressRelease, 200.0);
			pressReleaseSchedule = true;
		}
	}
}
