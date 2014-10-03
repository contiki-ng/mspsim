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

import java.awt.Rectangle;
import java.io.IOException;

import se.sics.mspsim.chip.EA122DIP;
import se.sics.mspsim.chip.EA122DIPListener;
import se.sics.mspsim.config.MSP430f149Config;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.USART;
import se.sics.mspsim.extutil.jfreechart.DataChart;
import se.sics.mspsim.extutil.jfreechart.DataSourceSampler;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.ui.SerialMon;
import se.sics.mspsim.util.ArgumentManager;

public class LCD122DIPNode extends GenericNode implements EA122DIPListener
{	
	// Port 1
	private static final int DISPLAY_E0_PIN = 7;
	private static final int DISPLAY_E1_PIN = 5;
	
	// Port 6
	private static final int DISPLAY_RW_PIN = 4;
	private static final int DISPLAY_CMD_PIN = 6;
	
	// Private hardware members
	private EA122DIP ea122dip;
	
	// Private cpu members
	private IOPort port1;
	private IOPort port2;
	private IOPort port6;
	
	// AWT gui
	private LCD122DIPGui gui;
	
	// Public getters
	public EA122DIP getLcd()
	{
		return ea122dip;
	}
	
	public LCD122DIPNode() 
	{
		super("LCD122DIP", new MSP430f149Config());

	}

	public void setupNodePorts() 
	{		
		// Setup ports
		port1 = cpu.getIOUnit(IOPort.class, "P1");
		port2 = cpu.getIOUnit(IOPort.class, "P2");
	    port6 = cpu.getIOUnit(IOPort.class, "P6");
	    
	    // Setup display
	    ea122dip = new EA122DIP(cpu, new Rectangle(125, 140, 950, 230));
	    ea122dip.addListener(this);
	    
	    // Setup display ports
	    ea122dip.setDataPort(port2);
	    ea122dip.setCommandPin(port6, DISPLAY_CMD_PIN);
	    ea122dip.setReadWritePin(port6, DISPLAY_RW_PIN);
	    ea122dip.setEnableLeftPin(port1, DISPLAY_E1_PIN);
	    ea122dip.setEnableRightPin(port1, DISPLAY_E0_PIN);
	}
	
	@Override
	public void setupNode()
	{	
        setupNodePorts();

        if (stats != null) {
            stats.addMonitor(this);
            stats.addMonitor(cpu);
        }
        
        // Check if gui is enabled
        if (!config.getPropertyAsBoolean("nogui", true)) 
        {
        	setupGUI();
	      
    		// Add some windows for listening to serial output
        	USART usart = cpu.getIOUnit(USART.class, "USART1");
        	if (usart != null) 
        	{
        		SerialMon serial = new SerialMon(usart, "USART1 Port Output");
        		registry.registerComponent("serialgui", serial);
        	}
        	
            if (stats != null) 
            {
                // A HACK for some "graphs"!!!
                DataChart dataChart =  new DataChart(registry, "Duty Cycle", "Duty Cycle");
                registry.registerComponent("dutychart", dataChart);
                
                DataSourceSampler dss = dataChart.setupChipFrame(cpu);
                dataChart.addDataSource(dss, "CPU", stats.getDataSource(cpu.getID(), MSP430.MODE_ACTIVE));
            }
        }
	}
	
	public void setupGUI() 
  	{
		  if (gui == null) 
		  {
			  gui = new LCD122DIPGui(this);
			  registry.registerComponent("nodegui", gui);
		  }				
	}

	public static void main(String[] args) throws IOException 
	{
		LCD122DIPNode node = new LCD122DIPNode();
		ArgumentManager config = new ArgumentManager();
		config.handleArguments(args);
		node.setupArgs(config);
	}

	@Override
	public void displayChanged() 
	{
		// Repaint in maximum of 20ms
		gui.repaint(20);
	}

	@Override
	public int getModeMax() 
	{
		return 0;
	}
}
