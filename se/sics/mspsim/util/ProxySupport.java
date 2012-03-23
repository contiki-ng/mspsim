/**
 * Copyright (c) 2012, Swedish Institute of Computer Science.
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
 * ProxySupport
 *
 * Author  : Niclas Finne
 * Created : 22 mar 2012
 */

package se.sics.mspsim.util;

public abstract class ProxySupport<T> {

    protected T[] listeners;

    protected ProxySupport() {
    }

    protected ProxySupport(T[] listeners) {
        this.listeners = listeners;
    }

    @SuppressWarnings("unchecked")
    public T add(T oldListener, T newListener) {
        if (oldListener == null) {
            return newListener;
        }
        if (oldListener instanceof ProxySupport<?>) {
            ProxySupport<T> proxy = (ProxySupport<T>) oldListener;
            proxy.listeners = ArrayUtils.add((Class<T>) newListener.getClass(), proxy.listeners, newListener);
            return oldListener;
        }
        return create(oldListener, newListener);
    }

    public T remove(T oldListener, T listener) {
        if (oldListener == listener) {
            return null;
        }
        if (oldListener instanceof ProxySupport<?>) {
            @SuppressWarnings("unchecked")
            ProxySupport<T> proxy = (ProxySupport<T>) oldListener;
            T[] l = ArrayUtils.remove(proxy.listeners, listener);
            if (l == null) {
                return null;
            }
            if (l.length == 1) {
                return l[0];
            }
            proxy.listeners = l;
        }
        return oldListener;
    }

    protected abstract T create(T oldListener, T newListener);

}
