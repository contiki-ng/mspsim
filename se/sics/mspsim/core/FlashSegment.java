package se.sics.mspsim.core;

public class FlashSegment implements Memory {

    int memory[];
    MSP430Core core;
    int mask;
    private Flash flash;

    public FlashSegment(MSP430Core core, Flash flash, int mask) {
        this.core = core;
        memory = core.memory;
        this.mask = mask;
        this.flash = flash;
    }

    @Override
    public int read(int address, int mode, int type) throws EmulationException {
        if ((address & 0xfff00) != mask) {
            core.currentSegment = core.memorySegments[address >> 8];
            return core.currentSegment.read(address, mode, type);
        }
        
        int val = 0;
        if (core.isFlashBusy && flash.addressInFlash(address)) {
            flash.notifyRead(address);
        }

        val = memory[address] & 0xff;
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
        
        boolean word = mode != MSP430Constants.MODE_BYTE;

        flash.flashWrite(dstAddress, dst & 0xffff, word);
        if (mode > MSP430Constants.MODE_WORD) {
            flash.flashWrite(dstAddress + 2, dst >> 16, word);
        }
    }

}
