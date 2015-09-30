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
 * -----------------------------------------------------------------
 *
 * DebugUI
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 */

package se.sics.mspsim.ui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import se.sics.mspsim.core.DbgInstruction;
import se.sics.mspsim.core.DisAsm;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.util.Utils;

public class DebugUI extends JPanel {

  private static final long serialVersionUID = 2123628878332126912L;

  private JList<DbgInstruction> disList;
  private DbgListModel listModel;
  private JLabel[] regsLabel;
  private MSP430 cpu;

  private DisAsm disAsm;

  /**
   * Creates a new <code>DebugUI</code> instance.
   *
   */
  public DebugUI(MSP430 cpu) {
    this(cpu, true);
  }

  public DebugUI(MSP430 cpu, boolean showRegs) {
    super(new BorderLayout());
    this.cpu = cpu;
    disAsm = cpu.getDisAsm();

    listModel = new DbgListModel();
    disList = new JList<DbgInstruction>(listModel);
    disList.setFont(new Font("courier", 0, 12));
    disList.setCellRenderer(new MyCellRenderer());
    disList.setPreferredSize(new Dimension(500, 350));
    add(disList, BorderLayout.CENTER);

    if (showRegs) {
      JPanel regs = new JPanel(new GridLayout(2,8,4,0));
      regsLabel = new JLabel[16];
      for (int i = 0, n = 16; i < n; i++) {
	regs.add(regsLabel[i] = new JLabel("$0000"));
      }
      add(regs, BorderLayout.SOUTH);
      updateRegs();
    }
  }

  public void updateRegs() {
    if (regsLabel != null) {
      for (int i = 0, n = 16; i < n; i++) {
	regsLabel[i].setText("$" + Utils.hex16(cpu.reg[i]));
      }
    }
    repaint();
  }

  private class DbgListModel extends AbstractListModel<DbgInstruction> {
    private static final long serialVersionUID = -2856626511548201481L;

    int startPos = -1;
    int endPos = -1;
    final int size = 21;

    DbgInstruction[] instructions = new DbgInstruction[size];

    // -------------------------------------------------------------------
    // ListAPI
    // -------------------------------------------------------------------

    @Override
    public int getSize() {
      return size;
    }

    private void checkPC() {
      int pc = cpu.getPC();
      if (pc < startPos || pc > endPos) {
	startPos = pc;
	// recalculate index!!! with PC at the top of the "page"
	int currentPos = pc;
	for (int i = 0, n = size; i < n; i++) {
//        if (cpu.getExecCount(currentPos) == 0) {
//	    inst = new DbgInstruction();
//	    inst.setInstruction(cpu.memory[currentPos] + (cpu.memory[currentPos + 1] << 8), 2);
//	    inst.setASMLine("    " + Utils.hex(currentPos, 4) + " " +
//			    Utils.hex8(cpu.memory[currentPos]) + " " +
//			    Utils.hex8(cpu.memory[currentPos + 1]) +
//			    "       .word " + Utils.hex8(cpu.memory[currentPos]) +
//			    Utils.hex8(cpu.memory[currentPos + 1]));
//	  } else {

          DbgInstruction inst = disAsm.getDbgInstruction(currentPos, cpu);
          inst.setPos(currentPos);
          currentPos += inst.getSize();
          instructions[i] = inst;
	}
	endPos = currentPos;
      }
    }

    // Should cache the current 20 (or size) instructions to get a faster
    // version of this...
    // And have a call to "update" instead...
    @Override
    public DbgInstruction getElementAt(int index) {
      checkPC();
      return instructions[index];
    }
  }

  class MyCellRenderer extends JLabel implements ListCellRenderer<DbgInstruction> {

    private static final long serialVersionUID = -2633138712695105181L;

    public MyCellRenderer() {
      setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(
       JList<? extends DbgInstruction> list,
       DbgInstruction instruction,            // instruct to display
       int index,               // cell index
       boolean isSelected,      // is the cell selected
       boolean cellHasFocus)    // the list and the cell have the focus
     {
       String s;
       int pos = 0;
       if (instruction == null) {
           s = "---";
       } else {
           pos = instruction.getPos();
	   s = instruction.getASMLine(false);
	   if (cpu.hasWatchPoint(pos)) {
	     s = "*B " + s;
	   } else {
	     s = "   " + s;
	   }
           if (instruction.getFunction() != null) {
               s += ";   " + instruction.getFunction();
           }
       }
       setText(s);
       if (pos == cpu.getPC()) {
	 setBackground(Color.green);
       } else {
	 if (isSelected) {
	   setBackground(list.getSelectionBackground());
	   setForeground(list.getSelectionForeground());
	 } else {
	   setBackground(list.getBackground());
	   setForeground(list.getForeground());
	 }
       }
       setEnabled(list.isEnabled());
       setFont(list.getFont());
       return this;
     }
  }
}
