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
 * SkyGui
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.platform.sky;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.USART;
import se.sics.mspsim.ui.ManagedWindow;
import se.sics.mspsim.ui.SerialMon;
import se.sics.mspsim.ui.WindowManager;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.ServiceComponent;

public class SkyGui extends JComponent implements ServiceComponent {

  /**
   * 
   */
  private static final long serialVersionUID = 7753659717805292786L;
  
  private ServiceComponent.Status status = Status.STOPPED;
  
  public static final int GREEN_Y = 40;
  public static final int BLUE_Y = 46;
  public static final int RED_Y = 34;
  public static final int LED_X = 10;

  public static final Color BLUE_TRANS = new Color(0x80,0x80,0xff,0xa0);
  public static final Color GREEN_TRANS = new Color(0x40, 0xf0, 0x40, 0xa0);
  public static final Color RED_TRANS = new Color(0xf0, 0x40, 0x40, 0xa0);

  public static final Color BLUE_C = new Color(0xffa0a0ff);
  public static final Color GREEN_C = new Color(0xff60ff60);
  public static final Color RED_C = new Color(0xffff8000);

  private SerialMon serial;

  private ImageIcon skyImage;
  private ManagedWindow window;
  private MoteIVNode node;

  private ComponentRegistry registry;

  private String name;

  public SkyGui(MoteIVNode node) {
    this.node = node;
  }

  public String getName() {
    return name;
  }
  
  public void start() {
    setBackground(Color.black);
    setOpaque(true);

    URL imageURL = SkyGui.class.getResource("images/sky.jpg");
    if (imageURL == null) {
      imageURL = SkyGui.class.getResource("/images/sky.jpg");
    }
    if (imageURL != null) {
      skyImage = new ImageIcon(imageURL);
    } else {
      skyImage = new ImageIcon("images/sky.jpg");
    }
    if (skyImage.getIconWidth() == 0 || skyImage.getIconHeight() == 0) {
      // Image not found
      throw new IllegalStateException("image not found");
    }
    setPreferredSize(new Dimension(skyImage.getIconWidth(),
				   skyImage.getIconHeight()));

    WindowManager wm = (WindowManager) registry.getComponent("WindowManager");
    window = wm.createWindow("Sky");
//     window.setSize(190,240);
    window.add(this);
//    WindowUtils.restoreWindowBounds("SkyGui", window);
//    WindowUtils.addSaveOnShutdown("SkyGui", window);
    window.setVisible(true);

    MouseAdapter mouseHandler = new MouseAdapter() {

	private boolean buttonDown = false;
	private boolean resetDown = false;

	// For the button sensor and reset button on the Sky nodes.
	public void mousePressed(MouseEvent e) {
	  int x = e.getX();
	  int y = e.getY();
	  if (x > 126 && x < 138) {
	    if (y > 65 && y < 76) {
	      SkyGui.this.node.setButton(buttonDown = true);
	    } else if (y > 95 && y < 107) {
	      resetDown = true;
	    }
	  }
	}

	public void mouseReleased(MouseEvent e) {
	  if (buttonDown) {
	    SkyGui.this.node.setButton(buttonDown = false);

	  } else if (resetDown) {
	    int x = e.getX();
	    int y = e.getY();
	    resetDown = false;
	    if (x > 126 && x < 138 && y > 95 && y < 107) {
	      SkyGui.this.node.getCPU().reset();
	    }
	  }
	}
      };

    this.addMouseListener(mouseHandler);

    // Add some windows for listening to serial output
    MSP430 cpu = node.getCPU();
    IOUnit usart = cpu.getIOUnit("USART 1");
    if (usart instanceof USART) {
      serial = new SerialMon((USART)usart, "USART1 Port Output");
      ((USART) usart).setUSARTListener(serial);
    }
    status = Status.STARTED;
  }

  protected void paintComponent(Graphics g) {
    Color old = g.getColor();
    int w = getWidth(), h = getHeight();
    int iw = skyImage.getIconWidth(), ih = skyImage.getIconHeight();
    skyImage.paintIcon(this, g, 0, 0);
    // Clear all areas not covered by the image
    g.setColor(getBackground());
    if (w > iw) {
      g.fillRect(iw, 0, w, h);
    }
    if (h > ih) {
      g.fillRect(0, ih, w, h);
    }

    // Display all active leds
    if (node.redLed) {
      g.setColor(RED_TRANS);
      g.fillOval(LED_X - 2, RED_Y - 1, 9, 5);
      g.setColor(RED_C);
      g.fillOval(LED_X, RED_Y, 4, 3);
    }
    if (node.greenLed) {
      g.setColor(GREEN_TRANS);
      g.fillOval(LED_X - 2, GREEN_Y - 1, 9, 5);
      g.setColor(GREEN_C);
      g.fillOval(LED_X, GREEN_Y, 4, 3);
    }
    if (node.blueLed) {
      g.setColor(BLUE_TRANS);
      g.fillOval(LED_X - 2, BLUE_Y - 1, 9, 5);
      g.setColor(BLUE_C);
      g.fillOval(LED_X, BLUE_Y, 4, 3);
    }
    g.setColor(old);
  }

  public Status getStatus() {
    return status;
  }

  public void init(String name, ComponentRegistry registry) {
    this.name = name;
    this.registry = registry;
  }

  public void stop() {    
  }

}
