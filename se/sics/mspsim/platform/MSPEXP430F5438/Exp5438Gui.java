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
package se.sics.mspsim.platform.MSPEXP430F5438;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import se.sics.mspsim.core.StateChangeListener;
import se.sics.mspsim.platform.AbstractNodeGUI;

public class Exp5438Gui extends AbstractNodeGUI {

	private static final long serialVersionUID = 7753659717805292786L;

	public static final Point Button1_Pos = new Point(400, 463);
	public static final Point Button2_Pos = new Point(500, 463);
	public static final Point ButtonR_Pos = new Point(325, 84);
	public static final Point JoyL_Pos = new Point(103, 441);
	public static final Point JoyM_Pos = new Point(123, 441);
	public static final Point JoyR_Pos = new Point(143, 441);
	public static final Point JoyT_Pos = new Point(123, 421);
	public static final Point JoyB_Pos = new Point(123, 461);
	public static Boolean ButtonR_Pressed = false;
	public static int ButtonSize=10;

	public static final Point LEDR = new Point(238, 462);
	public static final Point LEDO = new Point(294, 462);

	public static final Color LEDR_TRANS = new Color(0x80, 0x80, 0xff, 0xff);
	public static final Color LEDO_TRANS = new Color(0x80, 0x80, 0xff, 0xff);

	public static final Color LEDO_C = new Color(0xff, 0x8C, 0x00, 0xff);
	public static final Color LEDR_C = new Color(0xf0, 0x10, 0x10, 0xff);

	private final Exp5438Node node;
	private final StateChangeListener ledsListener = new StateChangeListener() {
		public void stateChanged(Object source, int oldState, int newState) {
			repaint();
		}
	};

	public Exp5438Gui(Exp5438Node node) {
		super("MSPSIM fork by Prof. Rüdiger Heintz 0.4.26", "images/MSP-EXP430F5438.jpg");
		this.node = node;
		Action scaleDownAction = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				window.scale(1.0/1.2,(double)baseSize.getWidth()/baseSize.getHeight());
			}

		};        
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN,0),"scaleDown");
		getActionMap().put("scaleDown",scaleDownAction);
		Action scaleUpAction = new AbstractAction() {
			private static final long serialVersionUID = 2L;

			public void actionPerformed(ActionEvent e) {
				window.scale(1.2,(double)baseSize.getWidth()/baseSize.getHeight());
			}

		};        
		getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,0),"scaleUp");
		getActionMap().put("scaleUp",scaleUpAction);	}

	protected boolean isIn(Point mouse, Point btn) {
		double btnSize=ButtonSize*getRatio();
		if (mouse.y > (btn.y*getRatio() - btnSize) && mouse.y < (btn.y*getRatio() + btnSize)) {
			if (mouse.x > (btn.x*getRatio() - btnSize) && mouse.x < (btn.x*getRatio() + btnSize)) {
				return true;
			}
		}
		return false;
	}

	protected void startGUI() {
		MouseAdapter mouseHandler = new MouseAdapter() {

			public void mousePressed(MouseEvent e) {

				if (isIn(e.getPoint(), Button1_Pos)) {
					Exp5438Gui.this.node.button1.setPressed(true);
				} else if (isIn(e.getPoint(), Button2_Pos)) {
					Exp5438Gui.this.node.button2.setPressed(true);
				} else if (isIn(e.getPoint(), JoyL_Pos)) {
					Exp5438Gui.this.node.joyl.setPressed(true);
				} else if (isIn(e.getPoint(), JoyM_Pos)) {
					Exp5438Gui.this.node.joym.setPressed(true);
				} else if (isIn(e.getPoint(), JoyR_Pos)) {
					Exp5438Gui.this.node.joyr.setPressed(true);
				} else if (isIn(e.getPoint(), JoyT_Pos)) {
					Exp5438Gui.this.node.joyt.setPressed(true);
				} else if (isIn(e.getPoint(), JoyB_Pos)) {
					Exp5438Gui.this.node.joyb.setPressed(true);
				} else if (isIn(e.getPoint(), ButtonR_Pos)) {
					ButtonR_Pressed=true;
				}
				repaint();
			}

			public void mouseReleased(MouseEvent e) {
				Exp5438Gui.this.node.button1.setPressed(false);
				Exp5438Gui.this.node.button2.setPressed(false);
				Exp5438Gui.this.node.joyl.setPressed(false);
				Exp5438Gui.this.node.joym.setPressed(false);
				Exp5438Gui.this.node.joyr.setPressed(false);
				Exp5438Gui.this.node.joyt.setPressed(false);
				Exp5438Gui.this.node.joyb.setPressed(false);
				ButtonR_Pressed=false;
				if (isIn(e.getPoint(), ButtonR_Pos)) {
					Exp5438Gui.this.node.getCPU().reset();
				}
				repaint();
			}
		};
		this.addMouseListener(mouseHandler);
		node.leds.addStateChangeListener(ledsListener);
	}

	public Point calcPos(Point In){
		return new Point((int)(In.x*getRatio()+.5),(int)(In.y*getRatio()+.5));
	}

	public void fillR(Graphics g,Point pos){
		Point corrpos=calcPos(pos);
		int btnSize=(int)(ButtonSize*getRatio()+.5);
		g.fillRect(corrpos.x-btnSize, corrpos.y-btnSize, 2*btnSize, 2*btnSize);
	}

	public void fillO(Graphics g,Point pos,int x1,int y1,int x2,int y2 ){
		Point corrpos=calcPos(pos);
		g.fillOval(corrpos.x-(int)(x1*getRatio()), corrpos.y-(int)(y1*getRatio()), (int)(x2*getRatio()), (int)(y2*getRatio()));
	}

	protected void paintComponent(Graphics g) {

		Color old = g.getColor();

		super.paintComponent(g);
		
		g.setColor(new Color(0,0,0,128));
		if(this.node.button1.isPressed()) fillR(g,Button1_Pos);
		if(this.node.button2.isPressed()) fillR(g,Button2_Pos);
		if(this.node.joyl.isPressed()) fillR(g,JoyL_Pos);
		if(this.node.joym.isPressed())  fillR(g,JoyM_Pos);
		if(this.node.joyr.isPressed())  fillR(g,JoyR_Pos);
		if(this.node.joyt.isPressed())  fillR(g,JoyT_Pos);
		if(this.node.joyb.isPressed())  fillR(g,JoyB_Pos);
		if(ButtonR_Pressed){
			fillR(g,ButtonR_Pos);
		}

		// Display all active leds
		if (node.LEDR) {
			g.setColor(LEDR_TRANS);
			fillO(g,LEDR,7,2,13,9);
			g.setColor(LEDR_C);
			fillO(g,LEDR,5,1,9,7);
		}
		if (node.LEDO) {
			
			g.setColor(LEDO_TRANS);
			fillO(g,LEDO,7,2,13,9);
			g.setColor(LEDO_C);
			fillO(g,LEDO,5,1,9,7);
		}
		this.node.lcd.drawDisplay(g,getRatio());
		g.setColor(old);
	}

	protected void stopGUI() {
		node.leds.removeStateChangeListener(ledsListener);
	}

}
