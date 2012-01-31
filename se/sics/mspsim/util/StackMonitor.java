package se.sics.mspsim.util;

import se.sics.mspsim.core.CPUMonitor;
import se.sics.mspsim.core.MSP430;

public class StackMonitor implements CPUMonitor {

  private MSP430 cpu;
  private int heapStartAddress;
  private int stackStartAddress;

  private int stackMin = 0;
  private int stackMax = 0;
  private int stack = 0;
  private int profStackMax = 0;
  
  private DataSource maxDataSource = new DataSource() {
    public int getValue() {
      int tmp = stackMax;
      stackMax = stack;
      return tmp;
    }
    public double getDoubleValue() {
      return getValue();
    }
  };
  
  private DataSource minDataSource = new DataSource() {
    public int getValue() {
      int tmp = stackMin;
      stackMin = stack;
      return tmp;
    }   
    public double getDoubleValue() {
      return getValue();
    }
  };
  
  private DataSource dataSource = new DataSource() {
    public int getValue() {
      return stack;
    }
    public double getDoubleValue() {
      return getValue();
    }
  };
  
  public StackMonitor(MSP430 cpu) {
    this.cpu = cpu;
    this.cpu.setRegisterWriteMonitor(MSP430.SP, this);
    Object p = cpu.getRegistry().getComponent("profiler");
    if (p instanceof SimpleProfiler) {
        ((SimpleProfiler) p).setStackMonitor(this);
        System.out.println("Found simple profiler!!!: " + p);
    } else {
        System.out.println("Could not find simple profiler: " + p);
    }
    if (cpu.getDisAsm() != null) {
      MapTable mapTable = cpu.getDisAsm().getMap();
      if (mapTable != null) {
        this.heapStartAddress = mapTable.heapStartAddress;
        this.stackStartAddress = mapTable.stackStartAddress;
      }
    }
  }

  public int getStackStart() {
    return stackStartAddress;
  }
  
  public int getHeapStart() {
    return heapStartAddress;
  }
  

  public DataSource getMaxSource() {
    return maxDataSource;
  }

  public DataSource getMinSource() {
    return minDataSource; 
  }
  
  public DataSource getSource() {
    return dataSource; 
  }

  /* specialized profiler support for the stack */
  /* set current profiler Stack max to this value */
  public void setProfStackMax(int max) {
      profStackMax = max;
  }

  /* get profiler stack max */
  public int getProfStackMax() {
      return profStackMax;
  }
  
  public int getStack() {
      return stack;
  }
  
  public void cpuAction(int type, int adr, int data) {
    stack = ((stackStartAddress - data) + 0xffff) & 0xffff;
    if (stack > stackMax) {
      stackMax = stack;
    }
    if (stack < stackMin) {
      stackMin = stack;
    }
    if (stack > profStackMax) {
        profStackMax = stack;
    }
  }
}