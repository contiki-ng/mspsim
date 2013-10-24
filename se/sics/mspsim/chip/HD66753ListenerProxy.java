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


import se.sics.mspsim.util.ArrayUtils;




public class HD66753ListenerProxy implements HD66753Listener {

    private HD66753Listener[] HD66753Listeners;

    public HD66753ListenerProxy(HD66753Listener listen1, HD66753Listener listen2) {
    	HD66753Listeners = new HD66753Listener[] { listen1, listen2 };
    }

    public static HD66753Listener addListener(HD66753Listener portListener, HD66753Listener listener) {
        if (portListener == null) {
            return listener;
        }
        if (portListener instanceof HD66753ListenerProxy) {
            return ((HD66753ListenerProxy)portListener).add(listener);
        }
        return new HD66753ListenerProxy(portListener, listener);
    }

    public static HD66753Listener removeListener(HD66753Listener portListener, HD66753Listener listener) {
        if (portListener == listener) {
            return null;
        }
        if (portListener instanceof HD66753ListenerProxy) {
            return ((HD66753ListenerProxy)portListener).remove(listener);
        }
        return portListener;
    }

    public HD66753Listener add(HD66753Listener mon) {
    	HD66753Listeners = ArrayUtils.add(HD66753Listener.class, HD66753Listeners, mon);
        return this;
    }

    public HD66753Listener remove(HD66753Listener listener) {
    	HD66753Listener[] listeners = ArrayUtils.remove(HD66753Listeners, listener);
        if (listeners == null) {
            return null;
        }
        if (listeners.length == 1) {
            return listeners[0];
        }
        HD66753Listeners = listeners;
        return this;
    }

    @Override
    public void displayChanged() {
    	HD66753Listener[] listeners = this.HD66753Listeners;
        for(HD66753Listener l : listeners) {
            l.displayChanged();
        }
    }

}
