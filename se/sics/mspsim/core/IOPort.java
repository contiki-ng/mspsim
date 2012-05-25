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
 * IOPort
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 */

package  se.sics.mspsim.core;
import se.sics.mspsim.util.Utils;

public class IOPort extends IOUnit {

    public enum PinState { LOW, HI };

    private final int port;
    private final int interrupt;

    // External pin state!
    private PinState pinState[] = new PinState[8];

    /* NOTE: The offset needs to be configurable since the new IOPorts on 
     * the 5xxx series are located at other addresses.
     * Maybe create another IOPort that just convert IOAddress to this 'old' mode?
     * - will be slightly slower on IOWrite/read but very easy to implement.
     * 
     * 
     * 
     * 
     * 
     * 
     */
    
    enum PortReg {IN, OUT, DIR, SEL, IFG, IES, IE, REN, DS, IV_L, IV_H};
    
    public static final int IN = 0;
    public static final int OUT = 1;
    public static final int DIR = 2;
    public static final int SEL = 4; /* what about SEL2? */
    public static final int IFG = 5;
    public static final int IES = 6;
    public static final int IE = 7;
    public static final int REN = 8;
    public static final int DS = 9;
    public static final int IV_L = 10;
    public static final int IV_H = 11;

    private static final String[] names = {
        "IN", "OUT", "DIR", "SEL", "IFG", "IES", "IE", "REN", "DS" };


    /* portmaps for 1611 */
    private static final PortReg[] PORTMAP_INTERRUPT = 
        {PortReg.IN, PortReg.OUT, PortReg.DIR, PortReg.IFG, PortReg.IES, PortReg.IE, PortReg.SEL}; 
    private static final PortReg[] PORTMAP_NO_INTERRUPT = 
        {PortReg.IN, PortReg.OUT, PortReg.DIR, PortReg.SEL};

    private PortReg[] portMap;

    private PortListener portListener = null;
    // represents the direction register

    /* Registers for Digital I/O */

    private int in;
    private int out;
    private int dir;
    private int sel;
    private int ie;
    private int ifg;
    private int ies; /* edge select */
    private int ren;
//    private int ds;

    private int iv; /* low / high */

    private Timer[] timerCapture = new Timer[8];

    private IOPort ioPair;
    
    /**
     * Creates a new <code>IOPort</code> instance.
     *
     */
    public IOPort(MSP430Core cpu, int port,
            int interrupt, int[] memory, int offset) {
        super("P" + port, "Port " + port, cpu, memory, offset);
        this.port = port;
        this.interrupt = interrupt;
        this.ie = 0;
        this.ifg = 0;

        if (interrupt == 0) {
            portMap = PORTMAP_NO_INTERRUPT;
        } else {
            portMap = PORTMAP_INTERRUPT;
        }
    }

    /* Create an IOPort with a special PortMap */
    public IOPort(MSP430Core cpu, int port,
            int interrupt, int[] memory, int offset, PortReg[] portMap) {
        this(cpu, port, interrupt, memory, offset);
        this.portMap = portMap;
        
//        System.out.println("Port " + port + " interrupt vector: " + interrupt);
        /* register all the registers from the port-map */
        for (int i = 0; i < portMap.length; i++) {
            if (portMap[i] != null) {
//                System.out.println("  P" + port + portMap[i] + " at " + Utils.hex16(offset + i));
                cpu.setIO(offset + i, this, false);
            }
        }
    }
    
    public static IOPort parseIOPort(MSP430Core cpu, int interrupt, String specification) {
        /* Specification = Px=Offset,REG Off, ... */
        String[] specs = specification.split(",");
        int port = specs[0].charAt(1) - '0';
        int offset = Integer.parseInt(specs[0].substring(3), 16);
        
        PortReg[] portMap = new PortReg[0x20]; /* Worst case port-map */
        
        for (int i = 1; i < specs.length; i++) {
            String[] preg = specs[i].split(" ");
            PortReg pr = PortReg.valueOf(preg[0]);
            int offs = Integer.parseInt(preg[1], 16);
            portMap[offs] = pr;
        }
        
        return new IOPort(cpu, port, interrupt, cpu.memory, offset, portMap);
    }

    public int getPort() {
        return port;
    }

    public int getIn() {
        return in;
    }

    public int getOut() {
        return out;
    }

    public int getDirection() {
        return dir;
    }

    public int getSelect() {
        return sel;
    }

    public synchronized void addPortListener(PortListener newListener) {
        portListener = PortListenerProxy.addPortListener(portListener, newListener);
    }

    public synchronized void removePortListener(PortListener oldListener) {
        portListener = PortListenerProxy.removePortListener(portListener, oldListener);
    }

    public void setTimerCapture(Timer timer, int pin) {
        if (DEBUG) {
            log("Setting timer capture for pin: " + pin);
        }
        timerCapture[pin] = timer;
    }

    public void updateIV() {
        int bitval = 0x01;
        iv = 0;
        int ie_ifg = ifg & ie;
        for (int i = 0; i < 8; i++) {
            if ((bitval & ie_ifg) > 0) {
                iv = 2 + i * 2;
                break;
            }
            bitval = bitval << 1;
        }
        //System.out.println("*** Setting IV to: " + iv + " ifg: " + ifg);
    }
    
    /* only byte access!!! */
    int read_port(PortReg function, long cycles) {
        switch(function) {
        case OUT:
            return out;
        case IN:
            return in;
        case DIR:
            return dir;
        case REN:
            return ren;
        case IFG:
            return ifg;
        case IE:
            return ie;
        case IES:
            return ies;
        case SEL:
            return sel;
        case IV_L:
            return iv & 0xff;
        case IV_H:
            int v = iv >> 8;
            updateIV();
            return v;
        }
        /* default is zero ??? */
        return 0;
    }

    void write_port(PortReg function, int data, long cycles) {
        switch(function) {
        case OUT: {
            out = data;
            PortListener listener = portListener;
            if (listener != null) {
            	listener.portWrite(this, out | (~dir) & 0xff);
            }
            break;
        }
        case IN:
            logw("WARNING: writing to read-only " + getID() + "IN");
            throw new EmulationException("Writing to read-only " + getID() + "IN");
            //          in = data;
        case DIR: {
            dir = data;
            PortListener listener = portListener;
            if (listener != null) {
                // Any output configured pin (pin-bit = 0) should have 1 here?! 
                //              if (name.equals("1"))
                //                System.out.println(getName() + " write to IOPort via DIR reg: " + Utils.hex8(data));
                listener.portWrite(this, out | (~dir) & 0xff);
            }
            break;
        }
        case REN:
            ren = data;
            break;
        case IFG:
            if (DEBUG) {
                log("Clearing IFlag: " + data);
            }
            ifg &= data;
            updateIV();
            cpu.flagInterrupt(interrupt, this, (ifg & ie) > 0);
            break;
        case IE:
            ie = data;
            if (DEBUG) {
                log("Setting IE: " + data);
            }
            cpu.flagInterrupt(interrupt, this, (ifg & ie) > 0);
            break;
        case IES:
            ies = data;
            break;
        case SEL:
            sel = data;
            break;
            /* Can IV be written ? */
        case IV_L:
            iv = (iv & 0xff00) | data;
            break;
        case IV_H:
            iv = (iv & 0x00ff) | (data << 8);
            break;
        }
    }


    public int read(int address, boolean word, long cycles) {
        PortReg reg = portMap[address - offset];
        /* only byte read allowed if not having an ioPair */
        if (word && reg == PortReg.IV_L) {
            /* Always read low first then high => update on high!!! */
            return read_port(reg, cycles) | (read_port(PortReg.IV_H, cycles) << 8);
        } else if (word && ioPair != null) {
            /* read same function from both */
            return read_port(reg, cycles) | (ioPair.read_port(reg, cycles) << 8);
        }
        /* NOTE: read of PIV might be wrong here - might be word access on IV? */
        return read_port(reg, cycles);
    }


    public void write(int address, int data, boolean word, long cycles) {
        int iAddress = address - offset;
        if (iAddress < 0 || iAddress >= portMap.length) {
            throw new EmulationException("Writing to illegal IO port address at " + getID() + ": $" + Utils.hex(address, 4));
        }
        PortReg fun = portMap[iAddress];
        if (DEBUG) {
            log("Writing to " + getID() + fun +
                    " ($" + Utils.hex(address, 2) +
                    ") => $" + Utils.hex(data, 2) + "=#" +
                    Utils.binary8(data) + (word ? " (word)" : ""));
        }

        /* only byte write - need to convert any word write here... */
        if (word && ioPair != null) {
            write_port(fun, data & 0xff, cycles);
            ioPair.write_port(fun, data >> 8, cycles);
        } else {
            write_port(fun, data, cycles);
        }
    }

    public void interruptServiced(int vector) {
    }

    // for HW to set hi/low on the pins...
    public void setPinState(int pin, PinState state) {
        if (pinState[pin] != state) {
            pinState[pin] = state;
            int bit = 1 << pin;
            if (state == PinState.HI) {
                in |= bit;
            } else {
                in &= ~bit;
            }
            if (interrupt > 0) {
                if ((ies & bit) == 0) {
                    // LO/HI transition
                    if (state == PinState.HI) {
                        ifg |= bit;
                        updateIV();
                        if (DEBUG) {
                            log("Flagging interrupt (HI): " + bit);
                        }
                    }
                } else {
                    // HI/LO transition
                    if (state == PinState.LOW) {
                        ifg |= bit;
                        updateIV();
                        if (DEBUG) {
                            log("Flagging interrupt (LOW): " + bit);
                        }
                    }
                }
                // Maybe this is not the only place where we should flag int?
                cpu.flagInterrupt(interrupt, this, (ifg & ie) > 0);
            }

            if (timerCapture[pin] != null) {
                /* should not be pin and 0 here
                 * pin might need configuration and 0 can maybe also be 1? 
                 */
                //        if (DEBUG) log("Notifying timer of changed pin value");
                timerCapture[pin].capture(pin, 0, state);
            }

        }
    }

    public void reset(int type) {
        int oldValue = out | (~dir) & 0xff;

        for (int i = 0, n = 8; i < n; i++) {
            pinState[i] = PinState.LOW;
        }
        in = 0;
        dir = 0;
        ren = 0;
        ifg = 0;
        ie = 0;
        iv = 0;
        cpu.flagInterrupt(interrupt, this, (ifg & ie) > 0);

        PortListener listener = portListener;
        int newValue = out | (~dir) & 0xff;
        if (oldValue != newValue && listener != null) {
            listener.portWrite(this, newValue);
        }
    }

    public String info() {
        StringBuilder sb = new StringBuilder();
        /* TODO: USE PORTMAP FOR THIS!!! */
        String[] regs = names;
        sb.append('$').append(Utils.hex16(offset)).append(':');
        for (int i = 0, n = regs.length; i < n; i++) {
            sb.append(' ').append(regs[i]).append(":$")
            .append(Utils.hex8(0));
        }
        return sb.toString();
    }

}
