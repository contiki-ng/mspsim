package se.sics.mspsim.debug;

import java.util.ArrayList;
import se.sics.mspsim.util.Utils;
import se.sics.mspsim.util.ELF;
import se.sics.mspsim.util.ELFSection;

public class DwarfReader {

    /* Operands for lines */
    public static final int    DW_LNS_EXT = 0;
    
    public static final int    DW_LNS_copy = 1;
    public static final int    DW_LNS_advance_pc = 2;
    public static final int    DW_LNS_advance_line = 3;
    public static final int    DW_LNS_set_file = 4;
    public static final int    DW_LNS_set_column = 5;
    public static final int    DW_LNS_negate_stmt = 6;
    public static final int    DW_LNS_set_basic_block = 7;
    public static final int    DW_LNS_const_add_pc = 8;
    public static final int    DW_LNS_fixed_advance_pc = 9;

    /* Extended operands (preceded by DW_LNS_EXT + len) */
    public static final int    DW_LNE_end_sequence = 1;
    public static final int    DW_LNE_set_address = 2;    
    public static final int    DW_LNE_define_file = 3;
    
    ELF elfFile;

    class Arange {
        int length;
        int version;
        int offset;
        int addressSize;
        int segmentSize;
    }

    /* some state for the line number handling */
    private int lineAddress;
    private int lineFile;
    private int lineLine;
    private int lineColumn;
    
    private ArrayList<Arange> aranges = new ArrayList<Arange>();
   
    
    public DwarfReader(ELF elfFile) {
        this.elfFile = elfFile;
    }

    public void read() {
        for (int i = 0; i < elfFile.getSectionCount(); i++) {
            ELFSection sec = elfFile.getSection(i);
            String name = sec.getSectionName();
            System.out.println("DWARF Section: " + name);
            if (".debug_aranges".equals(name)) {
                readAranges(sec);
            } else if (".debug_line".equals(name)) {
                readLines(sec);
            }
        }
    }

    private void readLines(ELFSection sec) {
        System.out.println("DWARF Line - ELF Section length: " + sec.getSize());
        int pos = 0;
        sec.reset();
        int totLen = sec.readElf32();
        int version = sec.readElf16();
        int proLen = sec.readElf32();
        int minOpLen = sec.readElf8();
        
        int isStmt = sec.readElf8();
        int lineBase = sec.readElf8();
        int lineRange = sec.readElf8();
        int opcodeBase = sec.readElf8();

        
        System.out.println("Line total length: " + totLen);
        System.out.println("Line pro length: " + proLen);
        System.out.println("Line version: " + version);
        
        /* first char of includes (skip opcode lens)... */
        for (int i = 0; i < opcodeBase - 1; i++) {
            sec.readElf8();
        }
        
//        pos = pos + 15 + opcodeBase - 1;
//        System.out.println("Line pos = " + pos + " sec-pos = " + sec.getPosition());
        System.out.println("Line --- include files ---");
        ArrayList<String> directories = new ArrayList<String>();
        directories.add("./");
        ArrayList<String> files = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        /* if first char is zero => no more include directories... */
        int c;
        while ((c = sec.readElf8()) != 0) {
            sb.append((char)c);
            while((c = sec.readElf8()) != 0) sb.append((char) c);
            System.out.println("Line: include file: " + sb.toString());
            directories.add(sb.toString());
            sb.setLength(0);
        }

        System.out.println("Line --- source files ---");
        long dirIndex = 0;
        long time = 0;
        long size = 0;
        while ((c = sec.readElf8()) != 0) {
            sb.append((char)c);
            while((c = sec.readElf8()) != 0) sb.append((char) c);
            dirIndex = sec.readLEB128();
            time = sec.readLEB128();
            size = sec.readLEB128();
            
            System.out.println("Line: source file: " + sb.toString() + "  dir: " + dirIndex + " size: " + size);
            files.add(directories.get((int) dirIndex) + sb.toString());
            sb.setLength(0);
        }
        
        /* Now we should have entered the position of the "code" for generating the
         * line <=> address table
         */
        System.out.println("Line: position: " + sec.getPosition());
        System.out.println("Line: first bytes of the machine: ");
        System.out.print("Line: ");
        for (int i = 0; i < 20; i++) {
           int ins =  sec.readElf8();
           System.out.print(Utils.hex8(ins) + " ");
           switch(ins) {
           case DW_LNS_EXT:
               /* extended instruction */
               break;
           case DW_LNS_advance_line:
               break;
           case DW_LNS_advance_pc:
               break;
           }
        }
        System.out.println();        
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
