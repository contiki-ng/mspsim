package se.sics.mspsim.core;

import java.util.ArrayList;

import se.sics.mspsim.util.Utils;

public abstract class MSP430Config {
    
    public class TimerConfig {
        int ccr0Vector;
        int ccrXVector;
        int ccrCount;
        int offset;
        String name;
        public int[] srcMap;
        
        public TimerConfig(int ccr0Vec, int ccrXVec, int ccrCount, int offset,
                int[] srcMap, String name) {
            ccr0Vector = ccr0Vec;
            ccrXVector = ccrXVec;
            this.ccrCount = ccrCount;
            this.name = name;
            this.offset = offset;
            this.srcMap = srcMap;
        }
    }

    public class UARTConfig {
        int txVector;
        int rxVector;
        int offset;
        String name;
        int txBit;
        int rxBit;
        int sfrAddr;
        boolean usciA;
        
        public UARTConfig(int txVector, int rxVector, int txBit, int rxBit, int sftAddr, int offset,
                    String name, boolean usciA) {
            this.txVector = txVector;
            this.rxVector = rxVector;
            this.txBit = txBit;
            this.rxBit = rxBit;
            this.offset = offset;
            this.name = name;
            this.usciA = usciA;
            this.sfrAddr = sftAddr;
        }
    }

    public UARTConfig[] uartConfig;
    
    /* default for the 149/1611 */
    public TimerConfig[] timerConfig = {
            new TimerConfig(6, 5, 3, 0x160, Timer.TIMER_Ax149, "TimerA"),
            new TimerConfig(13, 12, 7, 0x180, Timer.TIMER_Bx149, "TimerB")
    };
    
    public int maxMemIO = 0x200;
    public int maxMem = 64*1024;
    public int maxInterruptVector = 15;
    public boolean MSP430XArch = false;
    
    public abstract int setup(MSP430Core cpu, ArrayList<IOUnit> ioUnits);

    
    public String getAddressAsString(int addr) {
        return Utils.hex16(addr);
    }
}
