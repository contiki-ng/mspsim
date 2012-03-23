package se.sics.mspsim.core;

public class RAMSegment implements Memory {

    private final MSP430Core core;
    private final int memory[];
    private final int mask;

    public RAMSegment(MSP430Core core, int mask) {
        this.core = core;
        memory = core.memory;
        this.mask = mask;
    }

    @Override
    public int read(int address, int mode, AccessType type) throws EmulationException {
        if ((address & 0xfff00) != mask) {
            core.currentSegment = core.memorySegments[address >> 8];
            return core.currentSegment.read(address, mode, type);
        }
        int val = memory[address] & 0xff;
        if (mode > MSP430Constants.MODE_BYTE) {
            val |= (memory[address + 1] << 8);
            if ((address & 1) != 0) {
                core.printWarning(MSP430Constants.MISALIGNED_READ, address);
            }
            if (mode == MSP430Constants.MODE_WORD20) {
                /* will the read really get data from the full word? CHECK THIS */
                val |= (memory[address + 2] << 16) | (memory[address + 3] << 24);
                val &= 0xfffff;
            } else {
                val &= 0xffff;
            }
        }
        return val;
    }

    @Override
    public void write(int dstAddress, int dst, int mode)
            throws EmulationException {
        if ((dstAddress & 0xfff00) != mask) {
            core.currentSegment = core.memorySegments[dstAddress >> 8];
            core.currentSegment.write(dstAddress, dst, mode);
            return;
        }
        // assume RAM
        memory[dstAddress] = dst & 0xff;
        if (mode != MSP430Core.MODE_BYTE) {
            memory[dstAddress + 1] = (dst >> 8) & 0xff;
            if ((dstAddress & 1) != 0) {
                core.printWarning(MSP430Constants.MISALIGNED_WRITE, dstAddress);
            }
            if (mode > MSP430Core.MODE_WORD) {
                memory[dstAddress + 2] = (dst >> 16) & 0xff; /* should be 0x0f ?? */
                memory[dstAddress + 3] = (dst >> 24) & 0xff; /* will be only zeroes*/
            }
        }
    }

}
