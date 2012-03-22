package se.sics.mspsim.core;

import se.sics.mspsim.util.ArrayUtils;

public class MemoryMonitorProxy implements MemoryMonitor {
    private MemoryMonitor[] monitors;

    public MemoryMonitorProxy(MemoryMonitor mon1, MemoryMonitor mon2) {
        monitors = new MemoryMonitor[] { mon1, mon2 };
    }

    public static MemoryMonitor addMemoryMonitor(MemoryMonitor MemoryMonitor, MemoryMonitor mon) {
        if (MemoryMonitor == null) {
          return mon;
        }
        if (MemoryMonitor instanceof MemoryMonitorProxy) {
            return ((MemoryMonitorProxy)MemoryMonitor).add(mon);
        }
        return new MemoryMonitorProxy(MemoryMonitor, mon);
      }

    public static MemoryMonitor removeMemoryMonitor(MemoryMonitor MemoryMonitor, MemoryMonitor mon) {
        if (MemoryMonitor == mon) {
            return null;
        }
        if (MemoryMonitor instanceof MemoryMonitorProxy) {
            return ((MemoryMonitorProxy)MemoryMonitor).remove(mon);
        }
        return MemoryMonitor;
      }
    
    
    public MemoryMonitor add(MemoryMonitor mon) {
        monitors = (MemoryMonitor[]) ArrayUtils.add(MemoryMonitor.class, monitors, mon);
        return this;
    }
    
    public MemoryMonitor remove(MemoryMonitor mon) {
        MemoryMonitor[] mons = (MemoryMonitor[]) ArrayUtils.remove(monitors, mon);
        if (mons == null) {
            return null;
        }
        if (mons.length == 1) {
            return mons[0];
        }
        monitors = mons;
        return this;
    }
    
    @Override
    public void notifyReadBefore(int addr, int mode, int type) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void notifyReadAfter(int addr, int mode, int type) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void notifyWriteBefore(int dstAddress, int data, int mode) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void notifyWriteAfter(int dstAddress, int data, int mode) {
        // TODO Auto-generated method stub
        
    }
}
