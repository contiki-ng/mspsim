package se.sics.mspsim.util;

import se.sics.mspsim.core.CPUMonitor;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.MSP430Constants;

public class StackMonitor implements CPUMonitor {

  private MSP430 cpu;
  private int heapStartAddress;
  private int stackStartAddress;

  private int stackMin = 0;
  private int stackMax = 0;
  private int stack = 0;
  
  private DataSource maxDataSource = new DataSource() {
    public int getValue() {
      int tmp = stackMax;
      stackMax = stack;
      return tmp;
    }
  };
  
  private DataSource minDataSource = new DataSource() {
    public int getValue() {
      int tmp = stackMin;
      stackMin = stack;
      return tmp;
    }   
  };
  
  private DataSource dataSource = new DataSource() {
    public int getValue() {
      return stack;
    }
  };
  
  public StackMonitor(MSP430 cpu) {
    this.cpu = cpu;
    this.cpu.setRegisterWriteMonitor(MSP430.SP, this);

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

  public void cpuAction(int type, int adr, int data) {
    stack = ((stackStartAddress - data) + 0xffff) % 0xffff;
    if (stack > stackMax) {
      stackMax = stack;
    }
    if (stack < stackMin) {
      stackMin = stack;
    }
  }
}
