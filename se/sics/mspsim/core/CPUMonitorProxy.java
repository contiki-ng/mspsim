package se.sics.mspsim.core;

import se.sics.mspsim.util.ArrayUtils;

public class CPUMonitorProxy implements CPUMonitor {
    private CPUMonitor[] monitors;

    public CPUMonitorProxy(CPUMonitor mon1, CPUMonitor mon2) {
        monitors = new CPUMonitor[] { mon1, mon2 };
    }

    public static CPUMonitor addCPUMonitor(CPUMonitor cpuMonitor, CPUMonitor mon) {
        if (cpuMonitor == null) {
          return mon;
        }
        if (cpuMonitor instanceof CPUMonitorProxy) {
            return ((CPUMonitorProxy)cpuMonitor).add(mon);
        }
        return new CPUMonitorProxy(cpuMonitor, mon);
      }

    public static CPUMonitor removeCPUMonitor(CPUMonitor cpuMonitor, CPUMonitor mon) {
        if (cpuMonitor == mon) {
            return null;
        }
        if (cpuMonitor instanceof CPUMonitorProxy) {
            return ((CPUMonitorProxy)cpuMonitor).remove(mon);
        }
        return cpuMonitor;
      }
    
    
    public CPUMonitor add(CPUMonitor mon) {
        monitors = (CPUMonitor[]) ArrayUtils.add(CPUMonitor.class, monitors, mon);
        return this;
    }
    
    public CPUMonitor remove(CPUMonitor mon) {
        CPUMonitor[] mons = (CPUMonitor[]) ArrayUtils.remove(monitors, mon);
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
    public void cpuAction(int type, int adr, int data) {
        CPUMonitor[] mons = this.monitors;
        for(CPUMonitor mon : mons) {
            mon.cpuAction(type, adr, data);
        }
    }
}
