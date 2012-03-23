package se.sics.mspsim.core;
import se.sics.mspsim.util.ArrayUtils;

public class WatchedMemory implements Memory {

    private final MSP430Core core;
    private final int start;
    private final Memory wrappedMemory;
    private final MemoryMonitor watchPoints[][] = new MemoryMonitor[Memory.SEGMENT_SIZE][];

    WatchedMemory(MSP430Core core, int start, Memory wrapped) {
        this.core = core;
        this.start = start;
        this.wrappedMemory = wrapped;
    }
    
    @Override
    public int read(int address, int mode, AccessType type) throws EmulationException {
        if ((address & 0xfff00) != start) {
            core.currentSegment = core.memorySegments[address >> 8];
            return core.currentSegment.read(address, mode, type);
        }
        int a = address - start;
        int val;
        MemoryMonitor mons[] = watchPoints[a];
        if (mons != null) {
            for(MemoryMonitor mon : mons){
                mon.notifyReadBefore(address, mode, type);
            }
            val = wrappedMemory.read(address, mode, type);
            for(MemoryMonitor mon : mons){
                mon.notifyReadAfter(address, mode, type);
            }
        } else {
            val = wrappedMemory.read(address, mode, type);
        }
        return val;
    }

    @Override
    public void write(int dstAddress, int dst, int mode) throws EmulationException {
        if ((dstAddress & 0xfff00) != start) {
            core.currentSegment = core.memorySegments[dstAddress >> 8];
            core.currentSegment.write(dstAddress, dst, mode);
            return;
        }
        int a = dstAddress - start;
        MemoryMonitor mons[] = watchPoints[a];
        if (mons != null) {
            for(MemoryMonitor mon : mons){
                mon.notifyWriteBefore(dstAddress, dst, mode);
            }
            wrappedMemory.write(dstAddress, dst, mode);
            for(MemoryMonitor mon : mons){
                mon.notifyWriteAfter(dstAddress, dst, mode);
            }
        } else {
            wrappedMemory.write(dstAddress, dst, mode);
        }
    }

    public boolean hasWatchPoint(int address) {
        MemoryMonitor[] monitors = watchPoints[address - start];
        return monitors != null;
    }

    public synchronized void addWatchPoint(int address, MemoryMonitor mon) {
        MemoryMonitor[] monitors = watchPoints[address - start];
        monitors = ArrayUtils.add(MemoryMonitor.class, monitors, mon);
        watchPoints[address - start] = monitors;
    }

    public synchronized void removeWatchPoint(int address, MemoryMonitor mon) {
        MemoryMonitor[] monitors = watchPoints[address - start];
        monitors = ArrayUtils.remove(monitors, mon);
        watchPoints[address - start] = monitors;
    }

}
