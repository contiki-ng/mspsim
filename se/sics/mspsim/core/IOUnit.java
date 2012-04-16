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
 * IOUnit
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */

package se.sics.mspsim.core;

import java.io.PrintStream;

public abstract class IOUnit implements InterruptHandler, Loggable {

  int[] memory;
  int offset;

  protected final String id;
  protected final String name;

  private StateChangeListener stateListener;
  private int ioState;
  
  protected EmulationLogger logger;
  private PrintStream log;
  protected boolean DEBUG = false;

  public IOUnit(String id, int[] memory, int offset) {
    this(id, id, memory, offset);
  }

  public IOUnit(String id, String name, int[] memory, int offset) {
    this.id = id;
    this.name = name;
    this.memory = memory;
    this.offset = offset;
  }

  public void addStateChangeListener(StateChangeListener listener) {
      stateListener = StateChangeListener.Proxy.INSTANCE.add(stateListener, listener);
  }

  public void removeStateChangeListener(StateChangeListener listener) {
      stateListener = StateChangeListener.Proxy.INSTANCE.remove(stateListener, listener);
  }


  protected void stateChanged(int newState) {
      stateChanged(newState, false);
  }
  /* Called by subclasses to inform about changes of state */
  protected void stateChanged(int newState, boolean forceCallback) {
      if (forceCallback || ioState != newState) {
          int oldState = ioState;
          ioState = newState;
          /* inform listeners */
          StateChangeListener listener = stateListener;
          if (listener != null) {
              listener.stateChanged(this, oldState, ioState);
          }
      }
  }
  
  public void reset(int type) {
  }

  // write
  // write a value to the IO unit
  public abstract void write(int address, int value, boolean word, long cycles);

  // read
  // read a value from the IO unit
  public abstract int read(int address, boolean word, long cycles);

  public String getID() {
      return id;
  }

  public String getName() {
      return name;
  }
  
  /* Loggable */
  public void clearLogStream() {
    log = null;
    DEBUG = false;
  }

  public PrintStream getLogStream() {
    return log;
  }
  
  public void setLogStream(PrintStream out) {
    log = out;
    DEBUG = true;
  }

  protected void log(String msg) {
    PrintStream log = this.log;
    if (log != null) {
      log.println(getID() + ": " + msg);
    }
  }

  protected void logw(String msg) {
    String logMessage = getID() + ": " + msg;
    PrintStream log = this.log;
    if (log != null) {
      log.println(logMessage);
    }
    System.err.println(logMessage);
  }

  public void setEmulationLogger(EmulationLogger logger) {
    this.logger = logger;
  }

  public String info() {
      return "* no info";
  }
}
