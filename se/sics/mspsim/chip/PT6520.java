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
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.PortListener;

public class PT6520 extends Chip implements PortListener
{
	// Private constants
	private static final int DISPLAY_WIDTH = 61;
	private static final int DISPLAY_HEIGHT = 32;
	
	private static final int DISPLAY_LINES_PER_PAGE = 8;
	private static final int DISPLAY_PAGES = DISPLAY_HEIGHT / DISPLAY_LINES_PER_PAGE;
	private static final int DISPLAY_MAX_COLUMN = 79;
	
	private static final int DISPLAY_NORMAL_STATUS = 0x00;
	private static final int DISPLAY_BIT7 = 7;
	
	private static final int DISPLAY_CMD_SETPAGE = 0xB8;
	private static final int DISPLAY_CMD_SETPAGE_MASK = 0xFC;
	
	private static final int DISPLAY_CMD_STARTLINE = 0xC0;
	private static final int DISPLAY_CMD_STARTLINE_MASK = 0xE0;
	
	private static final int DISPLAY_CMD_ON = 0xAF;
	private static final int DISPLAY_CMD_OFF = 0xAE;
	
	private static final int DISPLAY_RGB_OFF = 0x40ff40;
	private static final int DISPLAY_RGB_ON = 0x000000;
	
	// Display buffers
	private int[][] displayMemory;
	private boolean displayMemorySet;
	
	private BufferedImage displayImage;
	private Rectangle displayPosition;
	
	// Listener
	private ArrayList<PT6520Listener> displayListeners = new ArrayList<PT6520Listener>();
	
	// Logic
	private int selectedPage;
	private int selectedColumn;
	private int selectedStartLine;
	
	// Data
	private IOPort dataPort;
	private int data;
	
	// Enable pin
	private IOPort enablePort;
	private int enablePin;
	
	// Command pin
	private IOPort commandPort;
	private int commandPin;
	private boolean commandEnable;
	
	// Read or write pin
	private IOPort readWritePort;
	private int readWritePin;
	private boolean readEnable;
	

	/**
	 * Construct a PT6520 emulated display
	 */
	public PT6520(String id, MSP430Core cpu, Rectangle position) 
	{
		super(id, cpu);
		
		// Initialize display memory and image
		displayMemory = new int[DISPLAY_PAGES][DISPLAY_MAX_COLUMN + 1];
		displayImage = new BufferedImage(DISPLAY_WIDTH, DISPLAY_HEIGHT, BufferedImage.TYPE_INT_RGB);
		refreshDisplay();
		
		// Remember position
		displayPosition = position;
	}
	
	public void addListener(PT6520Listener newListener) 
	{
		// Add to collection
		displayListeners.add(newListener);
	}

	public void removeListener(PT6520Listener oldListener)
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
		for (PT6520Listener listener : displayListeners) 
		{
			// Notify for display change
			listener.displayChanged();
		}
	}

	@Override
	public int getConfiguration(int parameter) 
	{
		return 0;
	}

	@Override
	public int getModeMax() 
	{
		return 0;
	}

	/**
	 * Set data port of display controller
	 */
	public void setDataPort(IOPort port) 
	{
		// Save reference
		this.dataPort = port;
		
		// Setup listener
		this.dataPort.addPortListener(this);
	}
	
	/**
	 * Set enable pin of display controller
	 */
	public void setEnablePin(IOPort port, int pin) 
	{
		// Save reference
		this.enablePort = port;
		this.enablePin = pin;
				
		// Setup listener
		this.enablePort.addPortListener(this);
		
	}

	/**
	 * Set command pin of display controller
	 */
	public void setCommandPin(IOPort port, int pin) 
	{
		// Save reference
		this.commandPort = port;
		this.commandPin = pin;
		
		// Setup listener
		this.commandPort.addPortListener(this);
		
	}

	/**
	 * Set read/write pin of display controller
	 */
	public void setReadWritePin(IOPort port, int pin) 
	{
		// Save reference
		this.readWritePort = port;
		this.readWritePin = pin;
		
		// Setup listener
		this.readWritePort.addPortListener(this);
		
	}

	@Override
	public void portWrite(IOPort source, int data) 
	{
		// Check if data was set
		if (source == dataPort)
		{
			// Save data
			this.data = data;
		}
		
		// Check if command was set
		if (source == commandPort)
		{
			// Save command enable flag
			commandEnable = ((data & (1 << commandPin)) == 0);
		}
		
		// Check if read or write was set
		if (source == readWritePort)
		{
			// Save read / write flag
			readEnable = ((data & (1 << readWritePin)) != 0);
		}
		
		// Check if enable is set
		if (source == enablePort)
		{
			// Check if enable pin is set
			if ((data & (1 << enablePin)) != 0)
			{
				// Check if we in command mode
				if (commandEnable)
				{
					// Check read status
					if (readEnable)
					{
						// Write normal status to pin
						setPortData(dataPort, DISPLAY_NORMAL_STATUS);
					}
					else
					{
						// Check for display commands
						if ((this.data & (1 << DISPLAY_BIT7)) != 0)
						{
							if (this.data == DISPLAY_CMD_ON)
							{
								// Log display power on
								log("Display power on command.");
							}
							else
							{
								if (this.data == DISPLAY_CMD_OFF)
								{
									// Log display power on
									log("Display power off command.");
								}
								else
								{
									if ((this.data & DISPLAY_CMD_STARTLINE_MASK) == DISPLAY_CMD_STARTLINE)
									{
										// Save selected start line
										selectedStartLine = (this.data & ~DISPLAY_CMD_STARTLINE_MASK);
									}
									else
									{
										if ((this.data & DISPLAY_CMD_SETPAGE_MASK) == DISPLAY_CMD_SETPAGE)
										{
											// Save selected page
											selectedPage = (this.data & ~DISPLAY_CMD_SETPAGE_MASK);
										}
										else
										{
											// Log not supported command
											log("Not supported command 0x" + Integer.toHexString(data));
										}
									}
								}
							}
						}
						else
						{
							// Save selected column
							selectedColumn = (this.data & 0x7F);
						}
					}
				}
				else
				{					
					// Check read status
					if (readEnable)
					{
						// Write data to port
						setPortData(dataPort, displayMemory[selectedPage][selectedColumn]);
					}
					else
					{
						// Save data to display memory and refresh
						displayMemory[selectedPage][selectedColumn] = this.data;
						displayMemorySet = true;
					}
				}
			}
			else
			{
				// Check if display data was set
				if (displayMemorySet)
				{
					// Refresh display
					refreshDisplay();
					
					// Set display data set flag
					displayMemorySet = false;
					
					// Increment column counter.
					selectedColumn = Math.min(selectedColumn + 1, DISPLAY_MAX_COLUMN);
				}
			}
		}
	}
	
	/**
	 * Set bitmask to given IOPort
	 */
	private void setPortData(IOPort port, int data) 
	{
		// Set each bit on given port
		for (int pin = 0; pin < 8; pin++)
		{
			// Set pin status from bitmask
			port.setPinState(pin, ((data & (1 << pin)) != 0) ? IOPort.PinState.HI : IOPort.PinState.LOW);
		}
	}
	
	/**
	 * Refresh buffer bitmap from memory
	 */
	private void refreshDisplay()
	{
		// Redraw display memory
		for (int page = 0; page < DISPLAY_PAGES; page++)
		{		
			for (int x = 0; x < DISPLAY_WIDTH; x++)
			{
				for (int y = 0; y < DISPLAY_LINES_PER_PAGE; y++)
				{
					int currentLine = (y + (page * DISPLAY_LINES_PER_PAGE));
					if (currentLine < selectedStartLine)
					{
						// Draw blank display
						displayImage.setRGB(x, currentLine, DISPLAY_RGB_OFF);
					}
					else
					{
						// Draw content of memory
						displayImage.setRGB(x, currentLine, (((displayMemory[page][x] & (1 << y)) != 0) ? DISPLAY_RGB_ON : DISPLAY_RGB_OFF)); 
					}
				}
			}
		}
		
		// Notify listeners
		notifyListener();
	}
	
	/**
	 * Draws buffer image to given graphics
	 */
	public void drawDisplay(Graphics g)
	{
		// Draw to graphics with scaling
		g.drawImage(displayImage, displayPosition.x + displayPosition.width, displayPosition.y + displayPosition.height, displayPosition.x, displayPosition.y, 0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT, null);
	}
}
