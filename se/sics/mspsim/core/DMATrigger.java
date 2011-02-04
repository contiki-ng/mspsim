package se.sics.mspsim.core;

public interface DMATrigger {
    public void setDMA(DMA dma, int startIndex);
    public void clearDMATrigger(int index);
}
