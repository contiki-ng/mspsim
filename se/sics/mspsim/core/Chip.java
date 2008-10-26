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
 * Chip
 *
 * Author  : Joakim Eriksson
 * Created : 17 jan 2008
 * Updated : $Date$
 *           $Revision$
 */
package se.sics.mspsim.core;
import se.sics.mspsim.util.Utils;

/**
 * @author Joakim
 *
 */
public abstract class Chip {

  private OperatingModeListener[] omListeners;
  private String[] modeNames = null;
  private int mode;
  
  public void addOperatingModeListener(OperatingModeListener listener) {
    omListeners = (OperatingModeListener[]) Utils.add(OperatingModeListener.class, omListeners, listener);
  }
  
  public void removeOperatingModeListener(OperatingModeListener listener) {
    omListeners = (OperatingModeListener[]) Utils.remove(omListeners, listener);
  }

  public int getMode() {
    return mode;
  }

  protected void setMode(int mode) {
    if (mode != this.mode) {
      this.mode = mode;
      OperatingModeListener[] listeners = omListeners;
      if (listeners != null) {
        for (int i = 0, n = listeners.length; i < n; i++) {
          listeners[i].modeChanged(this, mode);
        }
      }
    }
  }

  protected void setModeNames(String[] names) {
    modeNames = names;
  }

  public String getModeName(int index) {
    if (modeNames == null) {
      return null;
    }
    return modeNames[index];
  }

  public int getModeByName(String mode) {
    if (modeNames != null) {
      for (int i = 0; i < modeNames.length; i++) {
        if (mode.equals(modeNames[i])) return i;
      }
    }
    try {
      // If it is just an int it can be parsed!
      int modei = Integer.parseInt(mode);
      return modei;
    } catch (Exception e) {
    }
    return -1;
  }

  public abstract String getName();
  public abstract int getModeMax();

  /* By default the cs is set high */
  public boolean getChipSelect() {
    return true;
  }
  
  public String chipinfo() {
    return "* no info";
  }
}
