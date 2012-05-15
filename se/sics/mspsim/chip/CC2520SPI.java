package se.sics.mspsim.chip;

public class CC2520SPI {
    
    CC2520 cc2520;
    private int[] memory; /* pointer to the memory of cc2520 */
    SPICommand[] commands = new SPICommand[256];
    
    public CC2520SPI(CC2520 cc) {
    /* the SPI commands for CC2520 */
        SPICommand[] spiCommands = {
            new SPICommand("SNOP 0 0 0 0 0 0 0 0",cc2520) {
                public void executeSPICommand() {}
            },
            new SPICommand("IBUFLD 0 0 0 0 0 0 1 0 i i i i i i i i",cc2520) {
                public void executeSPICommand() {
                    System.out.println(name + " storing in buffer: " + spiData.getSPIData()[1]);
                    cc2520.instructionBuffer = spiData.getSPIData()[1];
                }
            },
            new SPICommand("SIBUFEX 0 0 0 0 0 0 1 1",cc2520),
            new SPICommand("SSAMPLECCA 0 0 0 0 0 1 0 0",cc2520),
            new SPICommand("SRES 0 0 0 0 1 1 1 1 - - - - - - - -",cc2520),
            new SPICommand("MEMRD 0 0 0 1 a a a a a a a a a a a a - - - - - - - - ...",cc2520),
            new SPICommand("MEMWR 0 0 1 0 a a a a a a a a a a a a d d d d d d d d ...",cc2520),
            new SPICommand("RXBUF 0 0 1 1 0 0 0 0 - - - - - - - - ...",cc2520),
            new SPICommand("RXBUFCP 0 0 1 1 1 0 0 0 0 0 0 0 a a a a a a a a a a a a - - - - - - - - ...",cc2520),
            new SPICommand("RXBUFMOV 0 0 1 1 0 0 1 p c c c c c c c c 0 0 0 0 a a a a a a a a a a a a",cc2520),
            new SPICommand("TXBUF 0 0 1 1 1 0 1 0 d d d d d d d d d d d d d d d d ...",cc2520),
            new SPICommand("TXBUFCP 0 0 1 1 1 1 1 p c c c c c c c c 0 0 0 0 a a a a a a a a a a a a",cc2520),
            new SPICommand("RANDOM 0 0 1 1 1 1 0 0 - - - - - - - - - - - - - - - - ...",cc2520),
            new SPICommand("SXOSCON 0 1 0 0 0 0 0 0",cc2520),
            new SPICommand("STXCAL 0 1 0 0 0 0 0 1",cc2520),
            new SPICommand("SRXON 0 1 0 0 0 0 1 0",cc2520),
            new SPICommand("STXON 0 1 0 0 0 0 1 1",cc2520) {
                public void executeSPICommand() {
                    cc2520.stxon();
                }
            },
            new SPICommand("STXONCCA 0 1 0 0 0 1 0 0",cc2520),
            new SPICommand("SRFOFF 0 1 0 0 0 1 0 1",cc2520),
            new SPICommand("SXOSCOFF 0 1 0 0 0 1 1 0",cc2520),
            new SPICommand("SFLUSHRX 0 1 0 0 0 1 1 1",cc2520),
            new SPICommand("SFLUSHTX 0 1 0 0 1 0 0 0",cc2520),
            new SPICommand("SACK 0 1 0 0 1 0 0 1",cc2520),
            new SPICommand("SACKPEND 0 1 0 0 1 0 1 0",cc2520),
            new SPICommand("SNACK 0 1 0 0 1 0 1 1",cc2520),
            new SPICommand("SRXMASKBITSET 0 1 0 0 1 1 0 0",cc2520),
            new SPICommand("SRXMASKBITCLR 0 1 0 0 1 1 0 1",cc2520),
            new SPICommand("RXMASKAND 0 1 0 0 1 1 1 0 d d d d d d d d d d d d d d d d",cc2520),
            new SPICommand("RXMASKOR 0 1 0 0 1 1 1 1 d d d d d d d d d d d d d d d d",cc2520),
            new SPICommand("MEMCP 0 1 0 1 0 0 0 p c c c c c c c c a a a a e e e e a a a a a a a a e e e e e e e e",cc2520),
            new SPICommand("MEMCPR 0 1 0 1 0 0 1 p c c c c c c c c a a a a e e e e a a a a a a a a e e e e e e e e",cc2520),
            new SPICommand("MEMXCP 0 1 0 1 0 1 0 p c c c c c c c c a a a a e e e e a a a a a a a a e e e e e e e e",cc2520),
            new SPICommand("MEMXWR 0 1 0 1 0 1 1 0 0 0 0 0 a a a a a a a a a a a a d d d d d d d d ...",cc2520),
            new SPICommand("BCLR 0 1 0 1 1 0 0 0 a a a a a b b b",cc2520),
            new SPICommand("BSET 0 1 0 1 1 0 0 1 a a a a a b b b",cc2520),
            new SPICommand("CTR/UCTR 0 1 1 0 0 0 0 p k k k k k k k k 0 c c c c c c c n n n n n n n n a a a a e e e e a a a a a a a a e e e e e e e e",cc2520),
            new SPICommand("CBCMAC 0 1 1 0 0 1 0 p k k k k k k k k 0 c c c c c c c a a a a e e e e a a a a a a a a e e e e e e e e 0 0 0 0 0 mmm",cc2520),
            new SPICommand("UCBCMAC 0 1 1 0 0 1 1 p k k k k k k k k 0 c c c c c c c 0 0 0 0 a a a a a a a a a a a a 0 0 0 0 0 mmm",cc2520),
            new SPICommand("CCM 0 1 1 0 1 0 0 p k k k k k k k k 0 c c c c c c c n n n n n n n n a a a a e e e e a a a a a a a a e e e e e e e e 0 f f f f f f f 0 0 0 0 0 0 mm",cc2520),
            new SPICommand("UCCM 0 1 1 0 1 0 1 p k k k k k k k k 0 c c c c c c c n n n n n n n n a a a a e e e e a a a a a a a a e e e e e e e e 0 f f f f f f f 0 0 0 0 0 0 mm",cc2520),
            new SPICommand("ECB 0 1 1 1 0 0 0 p k k k k k k k k c c c c a a a a a a a a a a a a 0 0 0 0 e e e e e e e e e e e e",cc2520),
            new SPICommand("ECBO 0 1 1 1 0 0 1 p k k k k k k k k c c c c a a a a a a a a a a a a",cc2520),
            new SPICommand("ECBX 0 1 1 1 0 1 0 p k k k k k k k k c c c c a a a a a a a a a a a a 0 0 0 0 e e e e e e e e e e e e",cc2520),
            new SPICommand("INC 0 1 1 1 1 0 0 p 0 0 c c a a a a a a a a a a a a",cc2520),
            new SPICommand("ABORT 0 1 1 1 1 1 1 1 0 0 0 0 0 0 c c",cc2520),
            new SPICommand("REGRD 1 0 a a a a a a - - - - - - - - ...",cc2520) {
                BitField adr = getBitField("a");
                int cAdr = 0;
                public boolean dataReceived(int data) {
                    /* check if this is first byte*/
                    if (spiData.getSPIlen() == 0) { 
                        cAdr = adr.getValue();
                    } else {
                        spiData.outputSPI(memory[cAdr++]);
                    }
                    return true;
                }
                public void executeSPICommand() {}
            },
            new SPICommand("REGWR 1 1 a a a a a a d d d d d d d d ...",cc2520)};
        
        /* set up the commands */
        for (int i = 0; i < spiCommands.length; i++) {
            SPICommand c = spiCommands[i];
            int maxv = 1 << (8 - c.bitCount);
            int v = c.value;
            /* populate an array with the values for quick decoding */
            for (int j = 0; j < maxv; j++) {
                System.out.printf(c.name + " =>  Value: %x\n", (v + j));
                if (commands[v + j] != null) {
                    System.out.println("ERROR: command already registered: " + commands[v + j].name);
                }
                commands[v + j] = c;
            }
        }    
    }
    
    
    SPICommand getCommand(int cmd) {
        if (cmd < 256 && commands[cmd] != null)
            return commands[cmd];
        return null;
    }
    
    
//    public static void main(String[] args) {
//        CC2520SPI spi = new CC2520SPI();
//        SPICommand cmd = spi.getCommand(0xff);
//        /* commands that take infinite number of bytes have the bitfield ... */
//        System.out.println("Has ... => " + cmd.getBitField("...",cc2520));
//    }
    
}
