package se.sics.mspsim.core;

public class IOSegment implements Memory {

    private final MSP430Core core;

    IOSegment(MSP430Core core) {
        this.core = core;
    }

    @Override
    public int read(int address, AccessMode mode, AccessType type) throws EmulationException {
        boolean word = mode != AccessMode.BYTE;
        // Only word reads at 0x1fe which is highest address...
        int val = core.memIn[address].read(address, word, core.cycles);
        if (mode == AccessMode.WORD20) {
            val |= core.memIn[address + 2].read(address, word, core.cycles) << 16;
        }
        return val;
    }

    @Override
    public void write(int dstAddress, int data, AccessMode mode) throws EmulationException {
        boolean word = mode != AccessMode.BYTE;

        if (!word) data &= 0xff;
        core.memOut[dstAddress].write(dstAddress, data & 0xffff, word, core.cycles);
        if (mode == AccessMode.WORD20) {
            core.memOut[dstAddress].write(dstAddress + 2, data >> 16, word, core.cycles);
        }
    }

    @Override
    public int get(int address, AccessMode mode) {
        return read(address, mode, AccessType.READ);
    }

    @Override
    public void set(int address, int data, AccessMode mode) {
        write(address, data, mode);
    }

}
