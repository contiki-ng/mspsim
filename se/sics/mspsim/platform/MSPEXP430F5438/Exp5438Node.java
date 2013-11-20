/**
 * Copyright (c) 2013, DHBW Cooperative State University Mannheim 
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
 * Author: Rüdiger Heintz <ruediger.heintz@dhbw-mannheim.de>
 */
package se.sics.mspsim.platform.MSPEXP430F5438;

import java.awt.Rectangle;
import java.io.IOException;

import se.sics.mspsim.chip.Button;
import se.sics.mspsim.chip.HD66753Listener;
import se.sics.mspsim.chip.Leds;
import se.sics.mspsim.config.MSP430f5438Config;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.USART;
import se.sics.mspsim.extutil.jfreechart.DataChart;
import se.sics.mspsim.extutil.jfreechart.DataSourceSampler;
import se.sics.mspsim.platform.GenericNode;
import se.sics.mspsim.ui.SerialMon;
import se.sics.mspsim.util.ArgumentManager;
import se.sics.mspsim.util.OperatingModeStatistics;
import se.sics.mspsim.chip.HD66753;

public class Exp5438Node extends GenericNode implements PortListener,HD66753Listener {

	public static final int MODE_LEDS_OFF = 0;
	public static final int MODE_MAX = 2;

	private static final int[] LEDSCOLOR = { 0xff2020, 0x40ff40 };
	public static final int LEDO_BIT = 0x02;
	public static final int LEDR_BIT = 0x01;

	public boolean LEDR;
	public boolean LEDO;

	public Leds leds;
	public Button button1;
	public Button button2;
	public Button joyl;
	public Button joyr;
	public Button joym;
	public Button joyt;
	public Button joyb;
	
	public HD66753 lcd;


	public Exp5438Gui gui;

	public Exp5438Node() {
		super("Exp5438", new MSP430f5438Config());
		setMode(MODE_LEDS_OFF);	
	}

	

	public void setupNodePorts() {	
		IOPort port1 = cpu.getIOUnit(IOPort.class, "P1");
		port1.addPortListener(this);	
		
		IOPort port2 = cpu.getIOUnit(IOPort.class, "P2");

		leds = new Leds(cpu, LEDSCOLOR);
		button1 = new Button("Button", cpu, port2, 6, false,Button.Btn_Typ.HighOpen);
		button2 = new Button("Button", cpu, port2, 7, false,Button.Btn_Typ.HighOpen);
		joyl = new Button("Button", cpu, port2, 1, false,Button.Btn_Typ.HighOpen);
		joyr = new Button("Button", cpu, port2, 2, false,Button.Btn_Typ.HighOpen);
		joym = new Button("Button", cpu, port2, 3, false,Button.Btn_Typ.HighOpen);
		joyt = new Button("Button", cpu, port2, 4, false,Button.Btn_Typ.HighOpen);
		joyb = new Button("Button", cpu, port2, 5, false,Button.Btn_Typ.HighOpen);
		
		lcd=new HD66753(cpu,"USCI B2",8,3,new Rectangle(368, 189, 179, 138));
		lcd.addListener(this);		
	}

	public void setupGUI() {
		if (gui == null) {
			gui = new Exp5438Gui(this);
			registry.registerComponent("nodegui", gui);
		}
	}

	public void portWrite(IOPort source, int data) {
		if (source.getPort() == 1) {
			LEDR = (data & LEDR_BIT) != 0;
			LEDO = (data & LEDO_BIT) != 0;
			leds.setLeds((LEDR ? 1 : 0) + (LEDO ? 2 : 0));
			int newMode = (LEDR ? 1 : 0) + (LEDO ? 1 : 0);
			setMode(newMode);
			gui.repaint();
		}	
	}
	
	public void displayChanged(){
		
		
		gui.repaint();
	}

	public void setupNode() {

		setupNodePorts();
		
		cpu.setFlashFile("flash.dat");

		if (stats != null) {
			stats.addMonitor(this);
			stats.addMonitor(cpu);
		}
		if (!config.getPropertyAsBoolean("nogui", true)) {
			setupGUI();

			IOPort port1 = cpu.getIOUnit(IOPort.class, "P1");

			// Add some windows for listening to serial output
			USART usart = cpu.getIOUnit(USART.class, "USART1");
			if (usart != null) {
				SerialMon serial = new SerialMon(usart, "USART1 Port Output");
				registry.registerComponent("serialgui", serial);
			}
			if (stats != null) {
				DataChart dataChart = new DataChart(registry, "Duty Cycle","Duty Cycle");
				registry.registerComponent("dutychart", dataChart);
				DataSourceSampler dss = dataChart.setupChipFrame(cpu);
				dataChart.addDataSource(dss, "LEDS", stats.getDataSource(
						getID(), 0, OperatingModeStatistics.OP_INVERT));
				dataChart.addDataSource(dss, "CPU",
						stats.getDataSource(cpu.getID(), MSP430.MODE_ACTIVE));
			}
		}
	}

	public int getModeMax() {
		return MODE_MAX;
	}

	public static void main(String[] args) throws IOException {
		Exp5438Node node = new Exp5438Node();
		ArgumentManager config = new ArgumentManager();
		config.handleArguments(args);
		node.setupArgs(config);
	}
}