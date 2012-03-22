package se.sics.mspsim.core;

import se.sics.mspsim.util.ArrayUtils;

public class WatchedMemory implements Memory {
    
    MemoryMonitor watchPoints[][] = new MemoryMonitor[Memory.SEGMENT_SIZE][];
    Memory wrappedMemory;
    private int start = 0;
    
    WatchedMemory(int start, Memory wrapped) {
        this.start = start;
        wrappedMemory = wrapped;
    }
    
    @Override
    public int read(int address, int mode, int type) throws EmulationException {
        int a = address - start;
        MemoryMonitor mons[] = watchPoints[a];
        if (mons != null) {
            for(int i = 0; i < mons.length; i++){
                mons[i].notifyReadBefore(address, mode, type);
            }
        }
        int val = wrappedMemory.read(address, mode, type);
        if (mons != null) {
            for(int i = 0; i < mons.length; i++){
                mons[i].notifyReadAfter(address, mode, type);
            }
        }
        return val;
    }

    @Override
    public void write(int dstAddress, int dst, int mode)
            throws EmulationException {
        int a = dstAddress - start;
        MemoryMonitor mons[] = watchPoints[a];
        if (mons != null) {
            for(int i = 0; i < mons.length; i++){
                mons[i].notifyWriteBefore(dstAddress, dst, mode);
            }
        }    
        wrappedMemory.write(dstAddress, dst, mode);
        if (mons != null) {
            for(int i = 0; i < mons.length; i++){
                mons[i].notifyWriteAfter(dstAddress, dst, mode);
            }
        }
    }

    public synchronized void addWatchPoint(int address, MemoryMonitor mon) {
        MemoryMonitor[] monitors = watchPoints[address - start];
        monitors = (MemoryMonitor[]) ArrayUtils.add(MemoryMonitor.class, monitors, mon);
        watchPoints[address - start] = monitors;
    }

    public synchronized void removeWatchPoint(int address, MemoryMonitor mon) {
        MemoryMonitor[] monitors = watchPoints[address - start];
        monitors = (MemoryMonitor[]) ArrayUtils.remove(monitors, mon);
        watchPoints[address - start] = monitors;
    }
}
