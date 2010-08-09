package se.sics.mspsim.debug;

import java.util.ArrayList;

import se.sics.mspsim.util.Utils;
import se.sics.mspsim.util.ELF;
import se.sics.mspsim.util.ELFSection;

public class DwarfReader {

    ELF elfFile;

    class Arange {
        int length;
        int version;
        int offset;
        int addressSize;
        int segmentSize;
    }

    private ArrayList<Arange> aranges = new ArrayList<Arange>();
   
    
    public DwarfReader(ELF elfFile) {
        this.elfFile = elfFile;
    }

    public void read() {
        for (int i = 0; i < elfFile.getSectionCount(); i++) {
            ELFSection sec = elfFile.getSection(i);
            String name = sec.getSectionName();
            if (".debug_aranges".equals(name)) {
                readAranges(sec);
            }
        }
    }

    /* DWARF - address ranges information */
    private void readAranges(ELFSection sec) {
        System.out.println("DWARF Aranges - ELF Section length: " + sec.getSize());
        int pos = 0;
        int index = 0;
        do {
            Arange arange = new Arange();
            /* here we should read the address data */
            arange.length = sec.readElf32(pos + 0); /* length not including the length field */
            arange.version = sec.readElf16(pos + 4); /* version */
            arange.offset = sec.readElf32(pos + 6); /* 4 byte offset into debug_info section (?)*/
            arange.addressSize = sec.readElf8(pos + 10); /* size of address */
            arange.segmentSize = sec.readElf8(pos + 11); /* size of segment descriptor */
            System.out.println("DWARF: aranges no " + index);
            System.out.println("DWARF: Length: " + arange.length);
            System.out.println("DWARF: Version: " + arange.version);
            System.out.println("DWARF: Offset: " + arange.offset);
            System.out.println("DWARF: Address size: " + arange.addressSize);

            index++;
            pos += 12;
            if (arange.addressSize == 2) {
                /* these needs to be added too! */
                int addr, len;
                do {
                    addr = sec.readElf16(pos);
                    len = sec.readElf16(pos + 2);
                    pos += 4;
                    System.out.println("DWARF: ($" + Utils.hex16(addr) + "," + len + ")");
                } while (addr != 0 || len != 0);
            }
        } while (pos < sec.getSize());
    }    
}
