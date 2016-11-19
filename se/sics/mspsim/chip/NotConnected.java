package se.sics.mspsim.chip;
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.IOPort;
import se.sics.mspsim.core.IOPort.PortReg;
import se.sics.mspsim.core.MSP430Core;
import se.sics.mspsim.core.PortListener;

public class NotConnected extends Chip implements PortListener {

    private final IOPort port;
    private final int pin;

    public NotConnected(String id, MSP430Core cpu, IOPort port, int pin) {
        super(id, cpu);
        this.port = port;
        this.pin = pin;
        port.addPortInListener(this);
    }
 

    public boolean isLow(){
        boolean Ren = ((this.port.getRegister(PortReg.REN) & (1 << this.pin)) != 0);
        boolean Up_Down = ((this.port.getRegister(PortReg.OUT) & (1 << this.pin)) != 0);
        return !Up_Down & Ren;
    }
    
    public void SetState() {
      boolean isitLow = isLow();
    
      stateChanged(isitLow ? 0 : 1);
      port.setPinState(pin, isitLow ? IOPort.PinState.LOW : IOPort.PinState.HI);
    }
    
    @Override
    public void portWrite(IOPort source, int data){
    	SetState();
    }

    @Override
    public int getConfiguration(int parameter) {
        return 0;
    }

    @Override
    public int getModeMax() {
        return 0;
    }

    @Override
    public String info() {
        return " Not connected is " + (isLow() ? "Low" : "High");
    }
    
    @Override
    public void notifyReset() {
      SetState();
    }    
}