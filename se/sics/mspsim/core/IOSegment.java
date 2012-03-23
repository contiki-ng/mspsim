package se.sics.mspsim.core;

public class IOSegment implements Memory {

    private final MSP430Core core;
    private final int mask;

    IOSegment(MSP430Core core, int mask) {
        this.core = core;
        this.mask = mask;
    }

    @Override
    public int read(int address, int mode, AccessType type) throws EmulationException {
        if ((address & 0xfff00) != mask) {
            core.currentSegment = core.memorySegments[address >> 8];
            return core.currentSegment.read(address, mode, type);
        }
        boolean word = mode != MSP430Constants.MODE_BYTE;
        // Only word reads at 0x1fe which is highest address...
        int val = core.memIn[address].read(address, word, core.cycles);
        if (mode == MSP430Constants.MODE_WORD20) {
            val |= core.memIn[address + 2].read(address, word, core.cycles) << 16;
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
        boolean word = mode != MSP430Constants.MODE_BYTE;

        if (!word) dst &= 0xff;
        core.memOut[dstAddress].write(dstAddress, dst & 0xffff, word, core.cycles);
        if (mode > MSP430Constants.MODE_WORD) {
            core.memOut[dstAddress].write(dstAddress + 2, dst >> 16, word, core.cycles);
        }
    }
}
