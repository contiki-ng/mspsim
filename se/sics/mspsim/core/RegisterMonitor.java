package se.sics.mspsim.core;

public interface RegisterMonitor {

    public void notifyReadBefore(int reg, int mode);
    public void notifyReadAfter(int reg, int mode);

    public void notifyWriteBefore(int reg, int data, int mode);
    public void notifyWriteAfter(int reg, int data, int mode);

}
