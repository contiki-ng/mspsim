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
 * Author: RÃ¼diger Heintz <ruediger.heintz@dhbw-mannheim.de>
 */
package se.sics.mspsim.chip;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.GenericUSCI;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.MSP430Config.MUXConfig;
import se.sics.mspsim.core.MSP430Config.TimerConfig;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.core.USARTSource;
import se.sics.mspsim.core.Timer;

public class HD66753 extends Chip implements USARTListener, PortListener, ActionListener {

	int[][] Commands = new int[][] {
		{0x74, 0x00, 0x12, 0x77, 0x00, 0x00},	//MacroReadBlockAddress
		{0x74, 0x00, 0x11, 0x76},				//MacroDrawBlockAdress
		{0x74, 0x00, 0x12, 0x76},				//MacroDrawBlockValue
		{0x74, 0x00, 0x00, 0x76},				//NotUused
		{0x74, 0x00, 0x01, 0x76},				//NotUused
		{0x74, 0x00, 0x02, 0x76},				//NotUused
		{0x74, 0x00, 0x03, 0x76},				//NotUused
		{0x74, 0x00, 0x04, 0x76},				//Contrast
		{0x74, 0x00, 0x05, 0x76},				//NotUused
		{0x74, 0x00, 0x06, 0x76},				//NotUused
		{0x74, 0x00, 0x07, 0x76},				//NotUused
	};		

	int[] MacroReadBlockAddress = { 0x74, 0x00, 0x12, 0x77, 0x00, 0x00 };
	int[] MacroDrawBlockAdress = { 0x74, 0x00, 0x11, 0x76 };
	int[] MacroDrawBlockValue = { 0x74, 0x00, 0x12, 0x76 };

	private List<Integer> dataCom = new ArrayList<Integer>();
	private MSP430 cpu;
	public BufferedImage DisplayImage;
	int w = 256;
	int h = 110;
	int Contrast=255;
	int LED_Port;
	int LED_Bit;
	Rectangle out;
	boolean valueChanged=false;

	private javax.swing.Timer displayUpdatetimer;
	private javax.swing.Timer displayresetFrequenztimer;

	private HD66753Listener Listen = null;

	private Timer LEDTimer=null;

	public HD66753(String id, MSP430 cpu, String USARTUnit, int LED_Port, int LED_Bit,
			Rectangle out) {
		super(id, cpu);

		DisplayImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int[] initVal = new int[4 * w * h];
		for (int i = 0; i < initVal.length; i++) {
			initVal[i] = 255 << 24;// Transparent
		}
		DisplayImage.getRaster().setPixels(0, 0, 256, 110, initVal);

		this.cpu = cpu;
		this.LED_Port = LED_Port;
		this.LED_Bit = LED_Bit;
		this.out = out;

		GenericUSCI usart = cpu.getIOUnit(GenericUSCI.class, USARTUnit);
		usart.addUSARTListener(this);

		IOPort port8 = cpu.getIOUnit(IOPort.class, "P" + LED_Port);
		port8.addPortOutListener(this);



		for (TimerConfig tconf : cpu.config.timerConfig) {
			for (MUXConfig mconf : tconf.muxConfig) {
				if((mconf.Port==LED_Port)&&(mconf.Pin==LED_Bit)){
					LEDTimer=(Timer)cpu.getIOUnit(tconf.name);
				}
			}
		}

		displayUpdatetimer = new javax.swing.Timer(100, this);
		displayUpdatetimer.stop();
		displayUpdatetimer.setRepeats(false);
		displayresetFrequenztimer = new javax.swing.Timer(1000, actionListener2);
		displayresetFrequenztimer.stop();
		displayresetFrequenztimer.setRepeats(false);	
	}

	ActionListener actionListener2 = new ActionListener()
	{
		public void actionPerformed(ActionEvent actionEvent)
		{
			valueChanged=false;
			displayresetFrequenztimer.stop();
			DisplayTimerRestart();
		}
	};


	public synchronized void addListener(HD66753Listener newListener) {
		Listen = HD66753ListenerProxy.addListener(Listen, newListener);
	}

	public synchronized void removeListener(HD66753Listener oldListener) {
		Listen = HD66753ListenerProxy.removeListener(Listen, oldListener);
	}

	public void printCommand(String info, List<Integer> Values) {
		StringBuffer buf = new StringBuffer();
		for (Integer val : Values) {
			buf.append(Integer.toHexString(val));
			buf.append(" ");
		}
		System.out.println(info + ">" + buf.toString());
	}

	public boolean compareList(int[] Macro, List<Integer> Values) {
		if (Values.size() < Macro.length)
			return false;
		for (int i = 0; i < Macro.length; i++) {
			if (Values.get(i) != Macro[i])
				return false;
		}
		return true;
	}

	public int commandIndex(List<Integer> Values) {
		for (int j=0;j<Commands.length;j++){
			if (Values.size() < Commands[j].length)
				continue;
			boolean identic=true;
			for (int i = 0; i < Commands[j].length; i++) {
				if (Values.get(i) != Commands[j][i]){
					identic=false;
					break;
				}
			}
			if (identic) return j;			
		}
		return -1;

	}	

	public void dataReceived(USARTSource source, int data) {
		if (data == 0x74) {
			if(dataCom.size()>5){
				analyseCommand();
				dataCom.clear();
			}
		}
		dataCom.add(data);
	}

	int Adress = 0;

	public void analyseCommand() {
		int index=commandIndex(dataCom);
		switch (index){
		case 1://MacroDrawBlockAdress
			if(dataCom.size()>5){
				Adress = dataCom.get(4) * 256 + dataCom.get(5);
			}else printCommand("Illegal", dataCom);
			break;
		case 2://MacroDrawBlockValue
			int x = (Adress % 32) * 8;
			int y = Adress / 32;
			int maxX=x+((dataCom.size()-1)/2)*8;
			if((maxX>=256)|(y>=110)){
				System.out.println("Error: Writing outside display");
			} else {
				for (int i = 4; i < (dataCom.size()-1); i += 2) {
					x = x + 8;
					int Value = dataCom.get(i) * 256 + dataCom.get(i + 1);
					SetPixel(y, x, Value);
				}
			}				
			DisplayTimerRestart();
			break;
		case 7://Contrast Value
			if(dataCom.size()>5){
				Contrast=dataCom.get(5);
			}
		case -1://Unkown
			//printCommand("Unkown", dataCom);
			break;
		default://Not Used
			//printCommand(index+" not Used", dataCom);
			break;
		}
	}


	private static int SetInt(int contrast,byte gray) {
		return (((gray == -1) ? 0 : contrast) << 24) | 0 | 0 | 0;
	}

	private void SetPixel(int y, int x, int Value) {
		int[] iArray = new int[8];
		iArray[0] = SetInt(Contrast,(byte) (255 - ((Value & 0x0003) << 6)));
		iArray[1] = SetInt(Contrast,(byte) (255 - ((Value & 0x000C) << 4)));
		iArray[2] = SetInt(Contrast,(byte) (255 - ((Value & 0x0030) << 2)));
		iArray[3] = SetInt(Contrast,(byte) (255 - ((Value & 0x00C0))));
		iArray[4] = SetInt(Contrast,(byte) (255 - ((Value & 0x0300) >> 2)));
		iArray[5] = SetInt(Contrast,(byte) (255 - ((Value & 0x0C00) >> 4)));
		iArray[6] = SetInt(Contrast,(byte) (255 - ((Value & 0x3000) >> 6)));
		iArray[7] = SetInt(Contrast,(byte) (255 - ((Value & 0xC000) >> 8)));


		DisplayImage.setRGB(x, y, 8, 1, iArray, 0, 8);

		/*		
		int[] iArray2 = new int[8];
		iArray2[0]=255;
		iArray2[1]=255;
		iArray2[2]=255;
		iArray2[3]=255;
		iArray2[4]=255;
		iArray2[5]=255;
		iArray2[6]=255;
		iArray2[7]=255;


		DisplayImage.getRaster().setPixels(x, y, 8, 1, iArray2);
		 */	

	}

	void SavetoImage() {
		File outputfile = new File("/root/saved.png");
		try {
			ImageIO.write(DisplayImage, "png", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	long old = 0;
	long ti = Long.MAX_VALUE / 2;
	long tp = Long.MAX_VALUE / 2;
	int value = 0;

	public void portWrite(IOPort source, int data) {
		int curvalue = ((data >> LED_Bit) & 1);
		if ((source.getPort() == LED_Port) && (curvalue != value)) {
			System.err.println(cpu.cpuCycles);
			long eventcycles=cpu.cpuCycles;
			if(LEDTimer!=null) eventcycles+=(LEDTimer.currentdiff+1);

			value = ((data >> LED_Bit) & 1);
			if (value == 0) {
				ti = eventcycles - old;
			} else {
				tp = eventcycles - old;
			}

			valueChanged=true;
			old = eventcycles;
			DisplayTimerRestart();
			displayresetFrequenztimer.restart();		
		}
	}

	int Restarts=0;

	private void DisplayTimerRestart(){
		Restarts++;
		if(Restarts>20) {
			DisplayTimerFired();
		}
		else displayUpdatetimer.restart();
	} 

	private void DisplayTimerFired(){
		Restarts=0;
		displayUpdatetimer.stop();
		if (Listen != null)
			Listen.displayChanged();		
	}

	public void actionPerformed(ActionEvent e) {
		DisplayTimerFired();
	}

	private double getFrequenz() {
		return 1000000.0 / (double) (ti + tp);// 1Mhz Takt
	}

	private String getText() {
		double frequenz = getFrequenz();
		String[] Unit = new String[] {"µHz", "mHz", "Hz", "kHz", "MHz", "Ghz" };
		int i = 2;
		if (frequenz == Double.POSITIVE_INFINITY)
			return "INFINITY";
		while (frequenz > 1000.0) {
			frequenz = frequenz / 1000.0;
			i++;
		}
		if(frequenz>0){
			while (frequenz < 1.0 && (i>1)) {
				frequenz = frequenz * 1000.0;
				i--;
			}
		}

		return String.format("State: %d  ti/T=%.2f  %.1f" + Unit[i], value, ti
				/ (double) (ti + tp), frequenz);
	}

	public int getBackground(){
		int background;
		if (!valueChanged) {
			background = value * 255;
		} else {
			background = (int) Math.min(
					(double) (255 * 1.1 * ti / (double) (ti + tp)), 255);
		}
		return background;
	}
	public void drawDisplay(Graphics g,double scale) {
		int background=getBackground();

		Rectangle outscale=new Rectangle((int)(out.x*scale+.5),(int)(out.y*scale+.5),(int)(out.width*scale+.5),(int)(out.height*scale+.5));

		g.setColor(new Color(background, background, background, 0xff));
		g.fillRect(outscale.x, outscale.y, outscale.width, outscale.height);
		g.drawImage(DisplayImage, outscale.x, outscale.y, outscale.x + outscale.width, outscale.y
				+ outscale.height, 8, 0, 138 + 8, 110, null);

		String txt = getText();
		g.setColor(new Color(255, 0, 0, 0xff));
		g.setFont(new Font( "SansSerif", Font.PLAIN, (int)(15*scale+.5) ));
		Rectangle2D rectText = g.getFont().getStringBounds(txt,g.getFontMetrics().getFontRenderContext());
		g.drawString(txt, (int) (outscale.x + outscale.width - rectText.getWidth()), outscale.y+ outscale.height);
	}

	@Override
	public int getModeMax() {
		return 0;
	}	

	@Override
	public void notifyReset() {
		dataCom.clear();
	}  	


	@Override
	public int getConfiguration(int parameter) {
		return 0;
	}	

}
