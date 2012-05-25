/**
 * Copyright (c) 2011, Swedish Institute of Computer Science.
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
 *
 * -----------------------------------------------------------------
 *
 * Author  : Joakim Eriksson
 */

package se.sics.mspsim.config;

import java.util.ArrayList;

import se.sics.mspsim.core.ADC12;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOUnit;
import se.sics.mspsim.core.MSP430Config;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.Multiplier;
import se.sics.mspsim.core.Timer;
import se.sics.mspsim.core.USCI;
import se.sics.mspsim.util.Utils;

public class MSP430f2617Config extends MSP430Config {

    
    public MSP430f2617Config() {
        /* 32 vectors for the MSP430X series */
        maxInterruptVector = 31;
        MSP430XArch = true;
        
        /* configuration for the timers */
        TimerConfig timerA = new TimerConfig(25, 24, 3, 0x160, Timer.TIMER_Ax149, "TimerA", Timer.TAIV);
        TimerConfig timerB = new TimerConfig(29, 28, 7, 0x180, Timer.TIMER_Bx149, "TimerB", Timer.TBIV);
        timerConfig = new TimerConfig[] {timerA, timerB};

        /* TX Vec, RX Vec, TX Bit, RX Bit, SFR-reg, Offset, Name, A?*/
        UARTConfig uA0 = new UARTConfig(22, 23, 1, 0, 1, 0x60, "USCI A0", true);
        UARTConfig uB0 = new UARTConfig(22, 23, 3, 2, 1, 0x60, "USCI B0", false);
        UARTConfig uA1 = new UARTConfig(16, 17, 1, 0, 6, 0xD0, "USCI A1", true);
        UARTConfig uB1 = new UARTConfig(16, 17, 3, 2, 6, 0xD0, "USCI B1", false);
        uartConfig = new UARTConfig[] {uA0, uB0, uA1, uB1};

        /* configure memory */
        infoMemConfig(0x1000, 128 * 2);
        mainFlashConfig(0x3100, 92 * 1024);
        ramConfig(0x1100, 8 * 1024);
    }

    public int setup(MSP430Core cpu, ArrayList<IOUnit> ioUnits) {

        Multiplier mp = new Multiplier(cpu, cpu.memory, 0);
        cpu.setIORange(0x130, 0x0f, mp);

        USCI usciA0 = new USCI(cpu, 0, cpu.memory, this);
        USCI usciB0 = new USCI(cpu, 1, cpu.memory, this);
        USCI usciA1 = new USCI(cpu, 2, cpu.memory, this);
        USCI usciB1 = new USCI(cpu, 3, cpu.memory, this);
        cpu.setIORange(0x60, 8, usciA0);
        cpu.setIORange(0x68, 8, usciB0);
        cpu.setIORange(0xd0, 8, usciA1);
        cpu.setIORange(0xd8, 8, usciB1);

        /* usciBx for i2c mode */
        cpu.setIORange(0x118, 2, usciB0);
        cpu.setIORange(0x11a, 2, usciB0);
        cpu.setIORange(0x17c, 2, usciB1);
        cpu.setIORange(0x17e, 2, usciB1);

        ioUnits.add(usciA0);
        ioUnits.add(usciB0);
        ioUnits.add(usciA1);
        ioUnits.add(usciB1);
        
        /* usciA1 handles interrupts for both usciA1 and B1 */
        cpu.setIORange(0x06, 2, usciA1);

        // Add port 1,2 with interrupt capability!
        // IOPorts will add themselves to the CPU
        ioUnits.add(new IOPort(cpu, 1, 4, cpu.memory, 0x20));
        ioUnits.add(new IOPort(cpu, 2, 1, cpu.memory, 0x28));

        // Add port 3,4 & 5,6
        for (int i = 0, n = 2; i < n; i++) {
            ioUnits.add(new IOPort(cpu, (3 + i), 0, cpu.memory, 0x18 + i * 4));
            ioUnits.add(new IOPort(cpu, (5 + i), 0, cpu.memory, 0x30 + i * 4));
        }

        ADC12 adc12 = new ADC12(cpu);
        ioUnits.add(adc12);
        cpu.setIORange(0x080, 16, adc12);
        cpu.setIORange(0x140, 16, adc12);
        cpu.setIORange(0x150, 16, adc12);
        cpu.setIORange(0x1a0,  8, adc12);

        /* 4 usci units + 6 io port*/
        return 4 + 6;
    }

    @Override
    public String getAddressAsString(int addr) {
        return Utils.hex20(addr);
    }

}