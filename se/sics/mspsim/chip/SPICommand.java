package se.sics.mspsim.chip;

import java.util.ArrayList;

public class SPICommand {
/*    
 * MEMXCP 0 1 0 1 0 1 0 p c c c c c c c c a a a a e e e e a a a a a a a a e e e e e e e e
 */
    public final static int DYNAMIC_LENGTH = 0xffff;
    
    String name;
    int mask;
    int value;
    int bitCount;
    int commandLen = 0;
        
    SPIData spiData;
    
    class BitField {
        String name;
        int startBit;
        int endBit;
        int firstMask;

        public BitField(String currentName, int start, int c) {
            name = currentName;
            startBit = start;
            endBit = c;
            firstMask = 0xff >> (startBit & 7);
            
            System.out.printf("First mask %x\n", firstMask);
        }
        
        public int getValue() {
            int value;
            int firstByte = startBit / 8;
            int lastByte = endBit / 8;
            int nrBitsRoll = 7 - endBit & 7;
                
            value = spiData.getSPIData()[firstByte] & firstMask;
            for (int i = firstByte + 1; i < lastByte; i++) {
                value = value << 8 + spiData.getSPIData()[firstByte];
            }
            value = value >> nrBitsRoll;

            return value;
        }
    }
    
    ArrayList<BitField> bitFields;
    
    SPICommand(String pattern, SPIData data) {
        spiData = data;
        String[] subs = pattern.split(" ");
        name = subs[0];
        System.out.println("Name:" + subs[0]);
        value = 0;
        mask = 0;
        int c = 0;
        int start = 0;
        String currentName = "-";
        bitCount = 0;
        for (int i = 1; i < subs.length; i++) {
            /* not more than first byte */
            if (subs[i].equals("1")) {
                if (c < 8) {
                    value = (value << 1) + 1;
                    mask = (mask << 1) | 1;
                    bitCount++;
                }
            } else if (subs[i].equals("0")) {                
                if (c < 8) {
                    value = (value << 1);
                    mask = (mask << 1) | 1;
                    bitCount++;
                }
            } else if (subs[i].equals(currentName)) {
                /* do nothing */
            } else {
                if (start != 0) {
                    System.out.println("Bitfield: " + currentName + ": [" +
                            start + " - " + (c - 1) + "]");
                    if (bitFields == null)
                        bitFields = new ArrayList<SPICommand.BitField>();
                    bitFields.add(new BitField(currentName, start, c - 1));
                } else {
                    System.out.printf("C: %d value: %x  mask: %x \n", c, value, mask);
                    if (c < 8) {
                        value = value << (8 - c);
                        mask = mask << (8 - c);
                    }
                }
                currentName = subs[i];
                start = c;
            }
            c++;            
        }
        if (start != 0) {
            System.out.println("Bitfield: " + currentName + ": [" +
                    start + " - " + (c - 1) + "]");
            if (bitFields == null)
                bitFields = new ArrayList<SPICommand.BitField>();
            bitFields.add(new BitField(currentName, start, c - 1));
        }
        commandLen = c / 8;
        if ("...".equals(currentName))
            commandLen = DYNAMIC_LENGTH;
        System.out.printf("Value %x\n", value);
        System.out.printf("Mask  %x\n", mask);
        System.out.println("Command len: " + commandLen);
    }

    /* return -1 if no match */
    /* or len of the rest of the arguments if any more */
    public int matchSPI(int spiData) {
        if ((spiData & mask) == value) {
            return commandLen;
        }
        return -1;
    }
    
    
    /* do nothing here...  - override if needed */
    public boolean dataReceived(int data) {
        return true;
    }
    
    /* for any command that is executable (finite commands) */
    public void executeSPICommand() {
        System.out.println("Command " + name + " not implemented...");
    }
    
    public BitField getBitField(String arg) {
        for (BitField b : bitFields) {
            if (b.name.equals(arg)) return b;
        }
        /* not existing ... */
        throw new IllegalArgumentException("No bitfield with name " + arg + " exists for " + name);
    }
}
