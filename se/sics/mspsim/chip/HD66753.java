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
package se.sics.mspsim.chip;

import java.awt.Color;
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

import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.PortListener;
import se.sics.mspsim.core.USARTListener;
import se.sics.mspsim.core.USARTSource;
import se.sics.mspsim.core.GenericUSCI;

public class HD66753 implements USARTListener, PortListener, ActionListener {

	int[] MacroReadBlockAddress = { 0x74, 0x00, 0x12, 0x77, 0x00, 0x00 };
	int[] MacroDrawBlockAdress = { 0x74, 0x00, 0x11, 0x76 };
	int[] MacroDrawBlockValue = { 0x74, 0x00, 0x12, 0x76 };


	private List<Integer> dataCom = new ArrayList<Integer>();
	private MSP430 cpu;
	public BufferedImage DisplayImage;
	int w = 256;
	int h = 110;
	int LED_Port;
	int LED_Bit;
	Rectangle out;
	
	private javax.swing.Timer waittimer;
	
	private HD66753Listener Listen = null;
	
	
	public HD66753(MSP430 cpu, String USARTUnit, int LED_Port,int LED_Bit,Rectangle out) {	

		DisplayImage = new BufferedImage(w, h,BufferedImage.TYPE_INT_ARGB);
		int[] initVal=new int[4*w*h];
		for(int i = 0; i < initVal.length; i++) {
			initVal[i] = 255<<24;//Transparent
        }
		DisplayImage.getRaster().setPixels(0, 0, 256, 110, initVal);

		this.cpu = cpu;
		this.LED_Port=LED_Port;
		this.LED_Bit=LED_Bit;
		this.out=out;

		GenericUSCI usart = cpu.getIOUnit(GenericUSCI.class, USARTUnit);
		usart.addUSARTListener(this);
		
		IOPort port8 = cpu.getIOUnit(IOPort.class, "P"+LED_Port);
		port8.addPortListener(this);	

		waittimer = new javax.swing.Timer(3000, this);
		waittimer.stop();
		waittimer.setRepeats(false);	
		}

	public synchronized void addListener(HD66753Listener newListener) {
		Listen = HD66753ListenerProxy.addListener(Listen,
				newListener);
	}

	public synchronized void removeListener(HD66753Listener oldListener) {
		Listen = HD66753ListenerProxy.removeListener(Listen,
				oldListener);
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

	public void dataReceived(USARTSource source, int data) {
		if (data == 0x74) {
			analyseCommand();
			dataCom.clear();
		}
		dataCom.add(data);
	}
	int Adress=0;

	public void analyseCommand() {
		if (compareList(MacroDrawBlockAdress, dataCom)) {
			Adress = dataCom.get(4) * 256 + dataCom.get(5);
		} else if (compareList(MacroDrawBlockValue, dataCom)) {
			int x = (Adress % 32) * 8;
			int y = Adress / 32;			
			for (int i = 4; i < dataCom.size(); i += 2) {
				x=x+8;
				int Value = dataCom.get(i) * 256 + dataCom.get(i+1);
				SetPixel(y,x,Value);
			}
			Listen.displayChanged();			
		} else {
			//printCommand("", dataCom);
		}
	}
	
	private static int SetInt(byte gray){
		return (((gray==-1)?0:255)<<24)|0|0|0;
	}
	
	private void SetPixel(int y,int x,int Value){
		
		int[] iArray=new int[8];
		iArray[0]=SetInt((byte)(255-((Value&0x0003)<<6)));
		iArray[1]=SetInt((byte)(255-((Value&0x000C)<<4)));
		iArray[2]=SetInt((byte)(255-((Value&0x0030)<<2)));
		iArray[3]=SetInt((byte)(255-((Value&0x00C0))));
		iArray[4]=SetInt((byte)(255-((Value&0x0300)>>2)));
		iArray[5]=SetInt((byte)(255-((Value&0x0C00)>>4)));
		iArray[6]=SetInt((byte)(255-((Value&0x3000)>>6)));
		iArray[7]=SetInt((byte)(255-((Value&0xC000)>>8)));
		DisplayImage.setRGB(x, y, 8, 1, iArray,0,8);
		//DisplayImage.getRaster().setPixels(x, y, 2, 1, iArray);
	}

	
	void SavetoImage(){	
		File outputfile = new File("/root/saved.png");
		try {
			ImageIO.write(DisplayImage, "png", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}			
	}
	
	long old=0;
	long ti=Long.MAX_VALUE/2;
	long tp=Long.MAX_VALUE/2;
	int value=0;
	
	public void portWrite(IOPort source, int data) {
		int curvalue=((data>>LED_Bit)&1);
		if ((source.getPort() == LED_Port)&&(curvalue!=value)) {
			waittimer.restart();
			value=((data>>LED_Bit)&1); 
			if(value==0){
				ti=cpu.cycles-old;
			}else{
				tp=cpu.cycles-old;
			}
			//System.out.println(tp+" "+ti);
			
			old=cpu.cycles;
			Listen.displayChanged();
		}		
	}
	
	public void actionPerformed(ActionEvent e) {
		waittimer.stop();
		if (value==0){
			ti=0;tp=Long.MAX_VALUE;
		}else{
			ti=Long.MAX_VALUE;tp=0;
		}
		if(Listen!=null)
		Listen.displayChanged();
	}
	
	private double getFrequenz(){
		return 1000000.0/(double)(ti+tp);//1Mhz Takt
	}
	
	private String getText(){
		double frequenz=getFrequenz();
		String []Unit=new String[]{"Hz","kHz","MHz","Ghz"};
		int i=0;
		if(frequenz==Double.POSITIVE_INFINITY) return "INFINITY";
		while(frequenz>1000.0){
			frequenz=frequenz/1000.0;
			i++;
		}
		return String.format("State: %d  ti/T=%.2f  %.1f"+Unit[i], value, ti/(double)(ti+tp),frequenz);
	}
	
	public void drawDisplay(Graphics g){
		int background;
		if(getFrequenz()<5){
			background=value*255;
		}else{
			background=(int)Math.min((double)(255*1.1*ti/(double)(ti+tp)),255);
		}

		g.setColor(new Color(background, background, background, 0xff));
		g.fillRect(out.x,out.y,out.width,out.height);
		g.drawImage(DisplayImage,out.x, out.y, out.x+out.width, out.y+out.height,8,0,138+8,110,null);
		String txt=getText();
		Rectangle2D rectText = g.getFont().getStringBounds(txt, g.getFontMetrics().getFontRenderContext());		
		
		g.setColor(new Color(255, 0, 0, 0xff));
		g.drawString(txt, (int)(out.x+out.width-rectText.getWidth()), out.y+out.height);
	}

}
