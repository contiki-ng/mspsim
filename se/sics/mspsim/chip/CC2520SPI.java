package se.sics.mspsim.chip;

public class CC2520SPI {
    SPICommand[] spiCommands = {
            new SPICommand("SNOP 0 0 0 0 0 0 0 0"),
            new SPICommand("IBUFLD 0 0 0 0 0 0 1 0 i i i i i i i i"),
            new SPICommand("SIBUFEX 0 0 0 0 0 0 1 1"),
            new SPICommand("SSAMPLECCA 0 0 0 0 0 1 0 0"),
            new SPICommand("SRES 0 0 0 0 1 1 1 1 - - - - - - - -"),
            new SPICommand("MEMRD 0 0 0 1 a a a a a a a a a a a a - - - - - - - - ..."),
            new SPICommand("MEMWR 0 0 1 0 a a a a a a a a a a a a d d d d d d d d ..."),
            new SPICommand("RXBUF 0 0 1 1 0 0 0 0 - - - - - - - - ..."),
            new SPICommand("RXBUFCP 0 0 1 1 1 0 0 0 0 0 0 0 a a a a a a a a a a a a - - - - - - - - ..."),
            new SPICommand("RXBUFMOV 0 0 1 1 0 0 1 p c c c c c c c c 0 0 0 0 a a a a a a a a a a a a"),
            new SPICommand("TXBUF 0 0 1 1 1 0 1 0 d d d d d d d d d d d d d d d d ..."),
            new SPICommand("TXBUFCP 0 0 1 1 1 1 1 p c c c c c c c c 0 0 0 0 a a a a a a a a a a a a"),
            new SPICommand("RANDOM 0 0 1 1 1 1 0 0 - - - - - - - - - - - - - - - - ..."),
            new SPICommand("SXOSCON 0 1 0 0 0 0 0 0"),
            new SPICommand("STXCAL 0 1 0 0 0 0 0 1"),
            new SPICommand("SRXON 0 1 0 0 0 0 1 0"),
            new SPICommand("STXON 0 1 0 0 0 0 1 1"),
            new SPICommand("STXONCCA 0 1 0 0 0 1 0 0"),
            new SPICommand("SRFOFF 0 1 0 0 0 1 0 1"),
            new SPICommand("SXOSCOFF 0 1 0 0 0 1 1 0"),
            new SPICommand("SFLUSHRX 0 1 0 0 0 1 1 1"),
            new SPICommand("SFLUSHTX 0 1 0 0 1 0 0 0"),
            new SPICommand("SACK 0 1 0 0 1 0 0 1"),
            new SPICommand("SACKPEND 0 1 0 0 1 0 1 0"),
            new SPICommand("SNACK 0 1 0 0 1 0 1 1"),
            new SPICommand("SRXMASKBITSET 0 1 0 0 1 1 0 0"),
            new SPICommand("SRXMASKBITCLR 0 1 0 0 1 1 0 1"),
            new SPICommand("RXMASKAND 0 1 0 0 1 1 1 0 d d d d d d d d d d d d d d d d"),
            new SPICommand("RXMASKOR 0 1 0 0 1 1 1 1 d d d d d d d d d d d d d d d d"),
            new SPICommand("MEMCP 0 1 0 1 0 0 0 p c c c c c c c c a a a a e e e e a a a a a a a a e e e e e e e e"),
            new SPICommand("MEMCPR 0 1 0 1 0 0 1 p c c c c c c c c a a a a e e e e a a a a a a a a e e e e e e e e"),
            new SPICommand("MEMXCP 0 1 0 1 0 1 0 p c c c c c c c c a a a a e e e e a a a a a a a a e e e e e e e e"),
            new SPICommand("MEMXWR 0 1 0 1 0 1 1 0 0 0 0 0 a a a a a a a a a a a a d d d d d d d d ..."),
            new SPICommand("BCLR 0 1 0 1 1 0 0 0 a a a a a b b b"),
            new SPICommand("BSET 0 1 0 1 1 0 0 1 a a a a a b b b"),
            new SPICommand("CTR/UCTR 0 1 1 0 0 0 0 p k k k k k k k k 0 c c c c c c c n n n n n n n n a a a a e e e e a a a a a a a a e e e e e e e e"),
            new SPICommand("CBCMAC 0 1 1 0 0 1 0 p k k k k k k k k 0 c c c c c c c a a a a e e e e a a a a a a a a e e e e e e e e 0 0 0 0 0 mmm"),
            new SPICommand("UCBCMAC 0 1 1 0 0 1 1 p k k k k k k k k 0 c c c c c c c 0 0 0 0 a a a a a a a a a a a a 0 0 0 0 0 mmm"),
            new SPICommand("CCM 0 1 1 0 1 0 0 p k k k k k k k k 0 c c c c c c c n n n n n n n n a a a a e e e e a a a a a a a a e e e e e e e e 0 f f f f f f f 0 0 0 0 0 0 mm"),
            new SPICommand("UCCM 0 1 1 0 1 0 1 p k k k k k k k k 0 c c c c c c c n n n n n n n n a a a a e e e e a a a a a a a a e e e e e e e e 0 f f f f f f f 0 0 0 0 0 0 mm"),
            new SPICommand("ECB 0 1 1 1 0 0 0 p k k k k k k k k c c c c a a a a a a a a a a a a 0 0 0 0 e e e e e e e e e e e e"),
            new SPICommand("ECBO 0 1 1 1 0 0 1 p k k k k k k k k c c c c a a a a a a a a a a a a"),
            new SPICommand("ECBX 0 1 1 1 0 1 0 p k k k k k k k k c c c c a a a a a a a a a a a a 0 0 0 0 e e e e e e e e e e e e"),
            new SPICommand("INC 0 1 1 1 1 0 0 p 0 0 c c a a a a a a a a a a a a"),
            new SPICommand("ABORT 0 1 1 1 1 1 1 1 0 0 0 0 0 0 c c"),
            new SPICommand("REGRD 1 0 a a a a a a - - - - - - - - ..."),
            new SPICommand("REGWR 1 1 a a a a a a d d d d d d d d ...")
    };
    
    SPICommand[] commands = new SPICommand[256];
    
    CC2520SPI() {
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
    
    
    public static void main(String[] args) {
        CC2520SPI spi = new CC2520SPI();
        SPICommand cmd = spi.getCommand(0xff);
        /* commands that take infinite number of bytes have the bitfield ... */
        System.out.println("Has ... => " + cmd.getBitField("..."));
    }
    
}
