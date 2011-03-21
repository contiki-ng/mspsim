package se.sics.mspsim.core;

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

    /* default for the 149/1611 */
    public TimerConfig[] timerConfig = {
            new TimerConfig(6, 5, 3, 0x160, Timer.TIMER_Ax149, "TimerA"),
            new TimerConfig(13, 12, 7, 0x180, Timer.TIMER_Bx149, "TimerB")
    };
    public int maxInterruptVector = 15;
    public boolean MSP430XArch = false;
    
    public abstract int setup(MSP430Core cpu, IOUnit[] ioUnits, int ioPos);
}
