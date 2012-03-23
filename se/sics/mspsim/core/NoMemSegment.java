package se.sics.mspsim.core;

public class NoMemSegment implements Memory {

    private final MSP430Core core;
    private final int mask;

    NoMemSegment(MSP430Core core, int mask) {
        this.core = core;
        this.mask = mask;
    }

    @Override
    public int read(int address, int mode, AccessType type) throws EmulationException {
        if ((address & 0xfff00) != mask) {
            core.currentSegment = core.memorySegments[address >> 8];
            return core.currentSegment.read(address, mode, type);
        }
//        core.printWarning(MSP430Constants., address);
        System.out.println("WARNING - no memory to read from...");
        return 0;
    }

    @Override
    public void write(int dstAddress, int dst, int mode)
            throws EmulationException {
        if ((dstAddress & 0xfff00) != mask) {
            core.currentSegment = core.memorySegments[dstAddress >> 8];
            core.currentSegment.write(dstAddress, dst, mode);
            return;
        }
        System.out.println("WARNING - no memory to write to...");
    }

}
