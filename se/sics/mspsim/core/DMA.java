package se.sics.mspsim.core;

import se.sics.mspsim.util.Utils;

public class DMA extends IOUnit {

    /* global DMA configuration */
    public static final int DMACTL0 = 0x122;
    public static final int DMACTL1 = 0x124;

    /* per channel configuration */
    public static final int DMAxCTL = 0x1e0;
    public static final int DMAxSA = 0x1e2;
    public static final int DMAxDA = 0x1e4;
    public static final int DMAxSZ = 0x1e6;
 
    /* DMA TSELx - from msp430x1xxx devices */
    /* new devices has more channels and more triggers */
    public static final int DMAREQ = 0;
    public static final int TACCR2 = 1;
    public static final int TBCCR2 = 2;
    public static final int URXIFG0 = 3; /* UART 0 */
    public static final int UTXIFG0 = 4; /* UART 0 */
    public static final int DAC12_0 = 5;
    public static final int ADC12_0 = 6;
    public static final int TACCR0 = 7;
    public static final int TBCCR0 = 8;
    public static final int URXIFG1 = 9; /* UART 1 */
    public static final int UTXIFG1 = 10; /* UART 1 */
    public static final int MULTIPLIER = 11;

    private static final int[] INCR = {0,0,-1,1};
    
    class Channel {
        /* public registers */
        int ctl;
        int sourceAddress;
        int destinationAddress;
        int size;
        
        /* internal registers */
        int currentSourceAddress;
        int currentDestinationAddress;
        int storedSize;

        int srcIncr = 0;
        int dstIncr = 0;
        boolean dstByteMode = false;
        boolean srcByteMode = false;
        int trigger;

        boolean enable = false;
        
        public void setTrigger(int t) {
            System.out.println("Setting trigger to " + t);
            trigger = t;
        }
        
        public void write(int address, int data) {
            switch(address) {
            case 0:
                ctl = data;
                dstIncr = INCR[(data >> 10) & 3];
                srcIncr = INCR[(data >> 8) & 3];
                dstByteMode = (data & 0x80) > 0; /* bit 7 */
                srcByteMode = (data & 0x40) > 0; /* bit 6 */
                enable = (data & 0x10) > 0; /* bit 4 */
                System.out.println("DMA: config srcIncr: " + srcIncr + " dstIncr:" + dstIncr);
                break;
            case 2:
                sourceAddress = data;
                currentSourceAddress = data;
                break;
            case 4:
                destinationAddress = data;
                currentDestinationAddress = data;
                break;
            case 6:
                size = data;
                storedSize = data;
                break;
            }
        }
        
        public int read(int address) {
            switch(address) {
            case 0:
                    return ctl;
            case 2:
                    return sourceAddress;
            case 4:
                    return destinationAddress;
            case 6:
                    return size;
            }
            System.out.println("Illegal read of DMA Channel register");
            return 0;
        }
        
        public void trigger(DMATrigger trigger, int index) {
            /* perform memory move and possibly clear triggering flag!!! */
            /* NOTE: show config byte/word also !!! */
            if (enable) {
                int data = cpu.read(currentSourceAddress, false);
                System.out.println("DMA Triggered reading from: " +
                        currentSourceAddress + " => " + data + " " + (char) data +
                        " size:" + size + " index:" + index);
                trigger.clearDMATrigger(index);
                DMA.this.cpu.write(destinationAddress, data, false);
                
                currentSourceAddress += srcIncr;
                currentDestinationAddress += dstIncr;
                size--;
                if (size == 0) {
                    currentSourceAddress = sourceAddress;
                    currentDestinationAddress = destinationAddress;
                    size = storedSize;
                    /* flag interrupt!!!! */
                }
                
            }
        }
    }

    private Channel channels[] = new Channel[3];
    private int dmactl0;
    private int dmactl1;
    
    MSP430Core cpu;
    
    public DMA(String id, int[] memory, int offset, MSP430Core msp430Core) {
        super(id, memory, offset);
        channels[0] = new Channel();
        channels[1] = new Channel();
        channels[2] = new Channel();
        this.cpu = msp430Core;
    }

    public void trigger(DMATrigger trigger, int startIndex, int index) {
        /* could make this a bit and have a bit-pattern if more dma channels but
         * with 3 channels it does not make sense. Optimize later - maybe with
         * flag in DMA triggers so that they now if a channel listens at all.
         */
        int totIndex = startIndex + index;
        for (int i = 0; i < channels.length; i++) {
            System.out.println("DMA Channel:" + channels[i].trigger + " index " + totIndex);
            if (channels[i].trigger == totIndex) channels[i].trigger(trigger, index);
        }
    }
    
    public void interruptServiced(int vector) {
    }

    public void write(int address, int value, boolean word, long cycles) {
        System.out.println("DMA write to: " + Utils.hex16(address) + ": " + value);
        switch (address) {
        case DMACTL0:
            /* DMA Control 0 */
            dmactl0 = value;
            channels[0].setTrigger(value & 0xf);
            channels[1].setTrigger((value >> 4) & 0xf);
            channels[2].setTrigger((value >> 8) & 0xf);
            break;
        case DMACTL1:
            /* DMA Control 1 */
            dmactl1 = value;
            break;
        default:
            /* must be word ??? */
            Channel c = channels[(address - DMAxCTL) / 8];
            c.write(address & 0x07, value);
        }
    }

    public int read(int address, boolean word, long cycles) {
        switch (address) {
        case DMACTL0:
            /* DMA Control 0 */
            return dmactl0;
        case DMACTL1:
            /* DMA Control 1 */
            return dmactl1; 
        default:
            /* must be word ??? */
            Channel c = channels[(address - DMAxCTL) / 8];
            return c.read(address & 7);
        }
    }
}
