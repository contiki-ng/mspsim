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
 * $Id$
 *
 * -----------------------------------------------------------------
 *
 * ESBGui
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.platform.esb;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;

import se.sics.mspsim.chip.Beeper;
import se.sics.mspsim.core.*;
import se.sics.mspsim.ui.SerialMon;
import se.sics.mspsim.ui.WindowUtils;

public class ESBGui extends JComponent implements KeyListener,
						  MouseMotionListener,
						  MouseListener, ADCInput {

  private static final long serialVersionUID = -139331418649524704L;

  public static final int GREEN_X = 3;
  public static final int YELLOW_X = 10;
  public static final int RED_X = 17;
  public static final int LED_Y = 4;

  public static final Color RED_TRANS = new Color(0xff,0x40,0x40,0xa0);
  public static final Color YELLOW_TRANS = new Color(0xff, 0xff, 0x00, 0xa0);
  public static final Color GREEN_TRANS = new Color(0x40, 0xf0, 0x40, 0xa0);

  public static final Color RED_C = new Color(0xffff6060);
  public static final Color YELLOW_C = new Color(0xffffff00);
  public static final Color GREEN_C = new Color(0xff40ff40);

  private static final float SAMPLE_RATE = 22050;
  private static final int DL_BUFFER_SIZE = 2200;

  private SerialMon serial;
  Beeper beeper;

  private ImageIcon esbImage;
  private JFrame window;
  private ESBNode node;
  private boolean buttonDown = false;
  private boolean resetDown = false;

  private TargetDataLine inDataLine;

  public ESBGui(ESBNode node) {
    this.node = node;

    setBackground(Color.black);
    setOpaque(true);

    esbImage = new ImageIcon("images/esb.jpg");
    if (esbImage.getIconWidth() == 0 || esbImage.getIconHeight() == 0) {
      // Image not found
      throw new IllegalStateException("image not found");
    }
    setPreferredSize(new Dimension(esbImage.getIconWidth(),
				   esbImage.getIconHeight()));

    window = new JFrame("ESB");
//     window.setSize(190,240);
    window.add(this);
    WindowUtils.restoreWindowBounds("ESBGui", window);
    WindowUtils.addSaveOnShutdown("ESBGui", window);
    window.setVisible(true);

    window.addKeyListener(this);
    addMouseMotionListener(this);
    addMouseListener(this);

    // Add some windows for listening to serial output
    MSP430 cpu = node.getCPU();
    IOUnit usart = cpu.getIOUnit("USART 1");
    if (usart instanceof USART) {
      serial = new SerialMon((USART)usart, "RS232 Port Output");
      ((USART) usart).setUSARTListener(serial);
    }

    IOUnit adc = cpu.getIOUnit("ADC12");
    if (adc instanceof ADC12) {
      ((ADC12) adc).setADCInput(0, this);
    }
    
    beeper = new Beeper();
    cpu.addIOUnit(-1,0,-1,0,beeper, true);
    
    
    // Just a test... TODO: remove!!!
    AudioFormat af = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    DataLine.Info dlin =
      new DataLine.Info(TargetDataLine.class, af, DL_BUFFER_SIZE);

    try {
      inDataLine = (TargetDataLine) AudioSystem.getLine(dlin);

      if (inDataLine == null) {
        System.out.println("No in dataline");
      } else {
        System.out.println("Format: " + inDataLine.getFormat());
        inDataLine.open(inDataLine.getFormat(), DL_BUFFER_SIZE);
        inDataLine.start();
      }
    } catch (Exception e) {
      System.out.println("Problem while getting data line ");
      e.printStackTrace();
    }
  }

  byte[] data = new byte[4];
  public int nextData() {
    inDataLine.read(data, 0, 4);
    //System.out.println("sampled: " + ((data[1] << 8) + data[0]));
    return (((data[1] & 0xff) << 8) | data[0] & 0xff) >> 4;
  }
  
  public void mouseMoved(MouseEvent e) {
    //    System.out.println("Mouse moved: " + e.getX() + "," + e.getY());
    int x = e.getX();
    int y = e.getY();
    node.setPIR(x > 18 && x < 80 && y > 35 && y < 100);
    node.setVIB(x > 62 && x < 95 && y > 160 && y < 178);
  }

  public void mouseDragged(MouseEvent e) {}
  public void mouseClicked(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}

  // For the button sensor on the ESB nodes.
  public void mousePressed(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    if (y > 152 && y < 168) {
      if (x > 0 && x < 19) {
	node.setButton(buttonDown = true);
      } else {
	int w = esbImage.getIconWidth();
	if (x > w - 20 && x < w) {
	  resetDown = true;
	}
      }
    }
  }
  public void mouseReleased(MouseEvent e) {
    if (buttonDown) {
      node.setButton(buttonDown = false);
    } else if (resetDown) {
      int x = e.getX();
      int y = e.getY();
      if (y > 152 && y < 168) {
	int w = esbImage.getIconWidth();
	if (x > w - 20 && x < w) {
	  node.getCPU().reset();
	}
      }
      resetDown = false;
    }
  }



  public void paintComponent(Graphics g) {
    Color old = g.getColor();
    int w = getWidth(), h = getHeight();
    int iw = esbImage.getIconWidth(), ih = esbImage.getIconHeight();
    esbImage.paintIcon(this, g, 0, 0);
    // Clear all areas not covered by the image
    g.setColor(getBackground());
    if (w > iw) {
      g.fillRect(iw, 0, w, h);
    }
    if (h > ih) {
      g.fillRect(0, ih, w, h);
    }

    // Display all active leds
    if (node.greenLed) {
      g.setColor(GREEN_TRANS);
      g.fillOval(GREEN_X - 1, LED_Y - 3, 5, 9);
      g.setColor(GREEN_C);
      g.fillOval(GREEN_X, LED_Y, 3, 4);
    }
    if (node.redLed) {
      g.setColor(RED_TRANS);
      g.fillOval(RED_X - 1, LED_Y - 3, 5, 9);
      g.setColor(RED_C);
      g.fillOval(RED_X, LED_Y, 3, 4);
    }
    if (node.yellowLed) {
      g.setColor(YELLOW_TRANS);
      g.fillOval(YELLOW_X - 1, LED_Y - 3, 5, 9);
      g.setColor(YELLOW_C);
      g.fillOval(YELLOW_X, LED_Y, 3, 4);
    }
    g.setColor(old);
  }

  public void keyPressed(KeyEvent key) {
    if (key.getKeyChar() == 'd') {
      node.setDebug(!node.getDebug());
    }
  }

  public void keyReleased(KeyEvent key) {
  }

  public void keyTyped(KeyEvent key) {
  }

}
