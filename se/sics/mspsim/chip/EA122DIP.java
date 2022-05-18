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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;

import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430Core;

public class EA122DIP extends Chip implements PT6520Listener
{
	// Controllers
	private PT6520 pt6520Left;
	private PT6520 pt6520Right;
	
	// Listeners
	private ArrayList<EA122DIPListener> displayListeners = new ArrayList<EA122DIPListener>();
	
	/**
	 * Construct a EA122DIP emulated display
	 */
	public EA122DIP(MSP430Core cpu, Rectangle position) 
	{
		super("EA122DIP", cpu);
		
		// Create controller for left side
		pt6520Left = new PT6520("PT6520Left", cpu, new Rectangle(position.x, position.y, position.width / 2, position.height));
		pt6520Left.addListener(this);
		
		// Create controller for right side
		pt6520Right = new PT6520("PT6520Right", cpu, new Rectangle(position.x + (position.width / 2), position.y, position.width / 2, position.height));
		pt6520Right.addListener(this);
	}
	
	public void addListener(EA122DIPListener newListener) 
	{
		// Add to collection
		displayListeners.add(newListener);
	}

	public void removeListener(EA122DIPListener oldListener)
	{
		// Remove from collection
		displayListeners.remove(oldListener);
	}
	
	/**
	 * Notify about display change
	 */
	private void notifyListener()
	{
		// Iterate through all listeners
		for (EA122DIPListener listener : displayListeners) 
		{
			// Notify for display change
			listener.displayChanged();
		}
	}
	
	/**
	 * Set data port
	 */
	public void setDataPort(IOPort port)
	{
		// Setup controller ports
		pt6520Left.setDataPort(port);
		pt6520Right.setDataPort(port);
	}
	
	/**
	 * Set read and write pin
	 */
	public void setReadWritePin(IOPort port, int pin)
	{
		// Setup controller ports
		pt6520Left.setReadWritePin(port, pin);
		pt6520Right.setReadWritePin(port, pin);
	}
	
	/**
	 * Set command pin
	 */
	public void setCommandPin(IOPort port, int pin)
	{
		// Setup controller ports
		pt6520Left.setCommandPin(port, pin);
		pt6520Right.setCommandPin(port, pin);	
	}
	
	/**
	 * Set controller one enable pin
	 */
	public void setEnableLeftPin(IOPort port, int pin)
	{
		// Setup controller ports
		pt6520Left.setEnablePin(port, pin);
	}

	/**
	 * Set controller two enable pin
	 */
	public void setEnableRightPin(IOPort port, int pin)
	{
		// Setup controller ports
		pt6520Right.setEnablePin(port, pin);
	}
	
	@Override
	public int getConfiguration(int parameter) 
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getModeMax() 
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void displayChanged() 
	{
		// Notify display has changed
		notifyListener();
	}
	
	/**
	 * Draws buffer image to given graphics
	 */
	public void drawDisplay(Graphics g)
	{
		// Get display from controllers
		pt6520Left.drawDisplay(g);
		pt6520Right.drawDisplay(g);
	}
}
