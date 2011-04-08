package se.sics.mspsim.core;

public interface USARTSource {

    public void setUSARTListener(USARTListener listener);
    
    /* for input into this UART */
    public boolean isReceiveFlagCleared();
    public void byteReceived(int b);
    
}
