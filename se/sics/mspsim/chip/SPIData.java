package se.sics.mspsim.chip;

public interface SPIData {

    public int[] getSPIData();
    public int getSPIlen();
    public void outputSPI(int data);
}
