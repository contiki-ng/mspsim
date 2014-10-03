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

package se.sics.mspsim.platform.lcd122dip;

import java.awt.Color;
import java.awt.Graphics;

import se.sics.mspsim.platform.AbstractNodeGUI;

public class LCD122DIPGui extends AbstractNodeGUI {

	// Private final statics
	private static final long serialVersionUID = 2884239980128494468L;
	
	// Private members
	private final LCD122DIPNode node;

	/**
	 * Construct an NETlog GUI
	 */
	public LCD122DIPGui(LCD122DIPNode node) 
	{
		super("EA122DIP", "images/msp430f149-lcd122dip.jpg");
		this.node = node;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void startGUI() 
	{
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void stopGUI() 
	{
	}
	
	@Override
	protected void paintComponent(Graphics g) 
	{
		// Get draw color
	    Color old = g.getColor();
	    super.paintComponent(g);
	
	    // Draw LCD content
	    node.getLcd().drawDisplay(g);
	   
		// Restore draw color  
	    g.setColor(old);
	}
}
