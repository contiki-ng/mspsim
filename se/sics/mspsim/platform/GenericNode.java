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
 * $Id: $
 *
 * -----------------------------------------------------------------
 *
 * GenericNode
 *
 * Author  : Joakim Eriksson
 */

package se.sics.mspsim.platform;

import java.io.IOException;

import se.sics.mspsim.cli.CommandHandler;
import se.sics.mspsim.cli.DebugCommands;
import se.sics.mspsim.cli.MiscCommands;
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.extutil.highlight.HighlightSourceViewer;
import se.sics.mspsim.ui.ControlUI;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.ELF;
import se.sics.mspsim.util.IHexReader;
import se.sics.mspsim.util.MapTable;
import se.sics.mspsim.util.OperatingModeStatistics;
import se.sics.mspsim.util.StatCommands;

public abstract class GenericNode extends Chip implements Runnable {

  protected ComponentRegistry registry = new ComponentRegistry();
  protected MSP430 cpu = new MSP430(0);
  protected String firmwareFile = null;
  protected ELF elf;
  protected OperatingModeStatistics stats;
  
  public abstract void setupNode();
  
  public void setup(String[] args) throws IOException {
    if (args.length == 0) {
      System.out.println("Usage: " + getClass().getName() + " <firmware>");
      System.exit(1);
    }

    CommandHandler ch = new CommandHandler();
    stats = new OperatingModeStatistics(cpu);

    registry.registerComponent("cpu", cpu);
    registry.registerComponent("commandHandler", ch);
    registry.registerComponent("debugcmd", new DebugCommands());
    registry.registerComponent("misccmd", new MiscCommands());
    registry.registerComponent("statcmd", new StatCommands(cpu, stats));
    registry.registerComponent("node", this);
    
    // Monitor execution
    cpu.setMonitorExec(true);
    //cpu.setDebug(true);
    int[] memory = cpu.getMemory();

    if (args[0].endsWith("ihex")) {
      // IHEX Reading
      IHexReader reader = new IHexReader();
      reader.readFile(memory, firmwareFile = args[0]);
    } else {
      elf = ELF.readELF(firmwareFile = args[0]);
      elf.loadPrograms(memory);
      MapTable map = elf.getMap();
      cpu.getDisAsm().setMap(map);
      cpu.setMap(map);
      registry.registerComponent("elf", elf);
      registry.registerComponent("mapTable", map);
    }
      
    cpu.reset();
    setupNode();
    
    // Setup control and other UI components
    ControlUI control = new ControlUI(registry);
    HighlightSourceViewer sourceViewer = new HighlightSourceViewer();
//    sourceViewer.addSearchPath(new File("../../contiki-2.x/examples/energest-demo/"));
    control.setSourceViewer(sourceViewer);
    
    if (args.length > 1) {
      MapTable map = new MapTable(args[1]);
      cpu.getDisAsm().setMap(map);
      registry.registerComponent("mapTable", map);
    }

    registry.start();
  }
  
 
  public void run() {
    System.out.println("Starting new CPU thread...");
    cpu.cpuloop();
    System.out.println("Stopping CPU thread...");
  }
  public void start() {
    if (!cpu.isRunning()) {
      new Thread(this).start();
    }
  }
  
  public void stop() {
    cpu.setRunning(false);
  }
  
  public void step() {
    if (!cpu.isRunning()) {
      cpu.step();
    }
  }
}
