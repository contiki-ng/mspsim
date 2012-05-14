package se.sics.mspsim.chip;

import java.util.ArrayList;
import java.util.Iterator;

public class SPICommand {
/*    
 * MEMXCP 0 1 0 1 0 1 0 p c c c c c c c c a a a a e e e e a a a a a a a a e e e e e e e e
 */
    String name;
    int mask;
    int value;
    int bitCount;
    int commandLen = 0;
    
    private class BitField {
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
        
        public int getValue(int[] spiData) {
            int value;
            int firstByte = startBit / 8;
            int lastByte = endBit / 8;
            int nrBitsRoll = 7 - endBit & 7;
                
            value = spiData[firstByte] & firstMask;
            for (int i = firstByte + 1; i < lastByte; i++) {
                value = value << 8 + spiData[firstByte];
            }
            value = value >> nrBitsRoll;

            return value;
        }
    }
    
    ArrayList<BitField> bitFields;
    
    SPICommand(String pattern) {
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

        System.out.printf("Value %x\n", value);
        System.out.printf("Mask  %x\n", mask);
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
    public boolean dataReceived(int spiData) {
        return true;
    }
    
    /* for any command that is executable (finite commands) */
    public boolean executeSPICommand(int[] spiData) {
        System.out.println("Command " + name + " not implemented...");
        return true;
    }
    
    public BitField getBitField(String arg) {
        for (BitField b : bitFields) {
            if (b.name.equals(arg)) return b;
        }
        /* not existing ... */
        return null;
    }
    
    public static void main(String[] args) {
        SPICommand c = new SPICommand("MEMXCP 0 1 0 1 0 1 0 p c c c c c c c c a a a a e e e e a a a a a a a a e e e e e e e e") {
            public boolean executeSPICommand(int[] spiData) {
                System.out.println("Yes!");
                return true;
            }
        };
        int[] data = new int[]{1,0x13,0xab,0xaa,0xbb};
        for (int i = 0; i < c.bitFields.size(); i++) {
            System.out.printf("Data %s: %x\n", c.bitFields.get(i).name, c.bitFields.get(i).getValue(data));
        }
        c.executeSPICommand(data);

        System.out.println("Bitcount:" + c.bitCount);
        int maxv = 1 << (8 - c.bitCount);
        int v = c.value;

        /* populate an array with the values for quick decoding */
        for (int i = 0; i < maxv; i++) {
            System.out.printf("Value: %x\n", (v + i));
        }
    }
}
