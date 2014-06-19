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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import se.sics.mspsim.core.StateChangeListener;
import se.sics.mspsim.platform.AbstractNodeGUI;

public class Exp5438Gui extends AbstractNodeGUI {

  private static final long serialVersionUID = 7753659717805292786L;

  public static final Point Button1_Pos = new Point(400, 463);
  public static final Point Button2_Pos = new Point(500, 463);
  public static final Point ButtonR_Pos = new Point(325, 84);
  public static final Point JoyL_Pos = new Point(103, 437);
  public static final Point JoyM_Pos = new Point(123, 437);
  public static final Point JoyR_Pos = new Point(143, 437);
  public static final Point JoyT_Pos = new Point(123, 421);
  public static final Point JoyB_Pos = new Point(123, 461);

  public static final int LEDR_X = 238;
  public static final int LEDR_Y = 462;
  public static final int LEDO_X = 294;
  public static final int LEDO_Y = 462;

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
    super("MSPSIM fork by Prof. Rüdiger Heintz", "images/MSP-EXP430F5438.jpg");
    this.node = node;

  }

  protected boolean isIn(Point mouse, Point btn) {
    if (mouse.y > (btn.y - 10) && mouse.y < (btn.y + 10)) {
      if (mouse.x > (btn.x - 10) && mouse.x < (btn.x + 10)) {
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
        }
      }

      public void mouseReleased(MouseEvent e) {
        Exp5438Gui.this.node.button1.setPressed(false);
        Exp5438Gui.this.node.button2.setPressed(false);
        Exp5438Gui.this.node.joyl.setPressed(false);
        Exp5438Gui.this.node.joym.setPressed(false);
        Exp5438Gui.this.node.joyr.setPressed(false);
        Exp5438Gui.this.node.joyt.setPressed(false);
        Exp5438Gui.this.node.joyb.setPressed(false);

        if (isIn(e.getPoint(), ButtonR_Pos)) {
          Exp5438Gui.this.node.getCPU().reset();
        }
      }
    };
    this.addMouseListener(mouseHandler);
    node.leds.addStateChangeListener(ledsListener);
  }

  protected void paintComponent(Graphics g) {
    
    
    Color old = g.getColor();

    super.paintComponent(g);

    // Display all active leds
    if (node.LEDR) {
      g.setColor(LEDR_TRANS);
      g.fillOval(LEDR_X - 7, LEDR_Y - 2, 13, 9);
      g.setColor(LEDR_C);
      g.fillOval(LEDR_X - 5, LEDR_Y - 1, 9, 7);
    }
    if (node.LEDO) {
      g.setColor(LEDO_TRANS);
      g.fillOval(LEDO_X - 7, LEDO_Y - 2, 13, 9);
      g.setColor(LEDO_C);
      g.fillOval(LEDO_X - 5, LEDO_Y - 1, 9, 7);
    }
    this.node.lcd.drawDisplay(g);
    g.setColor(old);
  }

  protected void stopGUI() {
    node.leds.removeStateChangeListener(ledsListener);
  }
}
