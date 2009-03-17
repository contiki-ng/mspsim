package se.sics.mspsim.net;

import java.awt.print.Printable;

public class HC01Packet extends IPv6Packet {
  public final static int IPHC_TTL_1 =  0x08;
  public final static int IPHC_TTL_64 = 0x10;
  public final static int IPHC_TTL_255 = 0x18;
  public final static int IPHC_TTL_I    = 0x00;

  /* Values of fields within the IPHC encoding second byte */
 public final static int IPHC_SAM_I =  0x00;
 public final static int IPHC_SAM_64 =  0x40;
 public final static int IPHC_SAM_16 =  0x80;
 public final static int IPHC_SAM_0 = 0xC0;
 public final static int IPHC_DAM_I = 0x00;
 public final static int IPHC_DAM_64 = 0x04;
 public final static int IPHC_DAM_16 = 0x08;
 public final static int IPHC_DAM_0 = 0x0C;


 public static final int HC01_DISPATCH = 0x03;

 private static class AddrContext {
   int used;
   int number;
   long prefix;
 }

 private AddrContext[] contexts = new AddrContext[4];

 public HC01Packet() {
   // setut some fake contexts just to get started...
   contexts[0] = new AddrContext();
   contexts[1] = new AddrContext();
   contexts[2] = new AddrContext();
   contexts[3] = new AddrContext();
  
   for (int i = 0; i < contexts.length; i++) {
     contexts[i].prefix = 0xaaaa000000000000L | ((i + 1)<< 8) | i + 1;
   }
 }
 
 public void setPacketData(byte[] data, int len) {
   int pos = 3;
   if (data[0] != HC01_DISPATCH) return;
   if ((data[1] & 0x40) == 0) {
     if ((data[1] & 0x80) == 0) {
       version = (data[pos] & 0xf0) >> 4;
       trafficClass = ((data[pos] & 0x0f)<<4) + ((data[pos + 1] & 0xff) >> 4);
       flowLabel = (data[pos + 1] & 0x0f) << 16 + (data[pos + 2] & 0xff) << 8 +
       data[pos + 3] & 0xff;
       pos += 4;
     } else {
       version = 6;
       trafficClass = 0;
       flowLabel = (data[pos] & 0x0f) << 16 
       + (data[pos + 1] & 0xff) << 8 + data[pos + 2] & 0xff;;
       pos += 3;
     }
   } else {
     version = 6;
     flowLabel = 0;
     if ((data[1] & 0x80) == 0) {
       trafficClass = (data[pos] & 0xff);
       pos++;
     } else {
       trafficClass = 0;
     }
   }
   /* encoding of TTL */
   switch (data[1] & 0x18) {
   case IPHC_TTL_1:
     hopLimit = 1;
     break;
   case IPHC_TTL_64:
     hopLimit = 64;
     break;
   case IPHC_TTL_255:
     hopLimit = 0xff;
     break;
   case IPHC_TTL_I:
     hopLimit = data[pos++];
     break;
   }

   /* 0, 1, 2, 3 as source address ??? */
   int srcAddress = (data[2] & 0x30) >> 4;
       AddrContext context = lookupContext(srcAddress);
       switch (data[2] & 0xc0) {
       case IPHC_SAM_0:
         if(context == null) {
           System.out.println("sicslowpan uncompress_hdr: error context not found\n");
           return;
         }
         /* set hi address as prefix from context */
         sourceAddressHi = context.prefix;
         /* infer IID from L2 address */

         /* should figure out a way to get the link layer address here!!! */
         sourceAddressLo = 0;
         break;
       case IPHC_SAM_16:
         if((data[pos] & 0x80) == 0) {
           /* unicast address */
           if(context == null) {
             System.out.println("sicslowpan uncompress_hdr: error context not found\n");
             return;
           }
           /* set hi address as prefix from context */
           sourceAddressHi = context.prefix;
           /* copy 6 NULL bytes then 2 last bytes of IID */
           sourceAddressLo = (data[pos] & 0xff) << 8+ data[pos + 1];
           pos += 2;
         } else {
           /* [ignore] multicast address check the 9-bit group-id is known */
           sourceAddressHi = (0xff << 56) + 
           ((((data[pos] & 0xff) >> 1) & 0x0F) << 48);
           sourceAddressLo = data[pos + 1] & 0xff;
           pos += 2;
         }
         break;
       case IPHC_SAM_64:
         if(context == null) {
           System.out.println("sicslowpan uncompress_hdr: error context not found\n");
           return;
         }
         /* copy prefix from context */
         sourceAddressHi = context.prefix;
         /* copy IID from packet */
         sourceAddressLo = getLong(data, pos);
         pos += 8;
         break;
       case IPHC_SAM_I:
         /* copy whole address from packet */
         sourceAddressHi = getLong(data, pos);
         pos += 8;
         sourceAddressLo = getLong(data, pos);
         pos += 8;
         break;
       }

       /* Destination address */
       context = lookupContext(data[2] & 0x03);

       switch(data[2] & 0x0C) {
       case IPHC_DAM_0:
         if(context == null) {
           System.out.println("sicslowpan uncompress_hdr: error context not found\n");
           return;
         }
         /* copy prefix from context */
         destAddressHi = context.prefix;
         /* infer IID from L2 address */
         /* figure out a way to pick this up from link-layer !!! */
         destAddressLo = 0;
         break;
       case IPHC_DAM_16:
         if((data[pos] & 0x80) == 0) {
           /* unicast address */
           if(context == null) {
             System.out.println("sicslowpan uncompress_hdr: error context not found\n");
             return;
           }
           destAddressHi = context.prefix;
           /* copy 6 NULL bytes then 2 last bytes of IID */
           destAddressLo = ((data[pos] & 0xff) << 8) + data[pos + 1] & 0xff;
           pos += 2;
         } else {
           /* multicast address check the 9-bit group-id is known */
           sourceAddressHi = (0xff << 56) + 
           ((((data[pos] & 0xff) >> 1) & 0x0F) << 48);
           sourceAddressLo = data[pos + 1] & 0xff;
           pos += 2;
         }
         break;
       case IPHC_DAM_64:
         if(context == null) {
           System.out.println("sicslowpan uncompress_hdr: error context not found\n");
           return;
         }
         /* copy prefix from context */
         destAddressHi = context.prefix;
         /* copy IID from packet */
         destAddressLo = getLong(data, pos);
         pos += 8;
         break;
       case IPHC_DAM_I:
         /* copy whole address from packet */
         destAddressHi = getLong(data, pos);
         pos += 8;
         destAddressLo = getLong(data, pos);
         pos += 8;
         break;
       }
       
       
       System.out.println("TTL: " + hopLimit);
       System.out.print("Src Addr: ");
       printAddress(System.out, sourceAddressHi, sourceAddressLo);
       System.out.print(" Dest Addr: ");
       printAddress(System.out, destAddressHi, destAddressLo);
       System.out.println();
 }
  
  private AddrContext lookupContext(int index) {
    if (index < contexts.length)
      return contexts[index];
    return null;
  }

}
