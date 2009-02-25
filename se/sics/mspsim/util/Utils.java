/**
 * Copyright (c) 2007, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of MSPSim.
 *
 * $Id$
 *
 * -----------------------------------------------------------------
 *
 * Utils
 *
 * Author  : Joakim Eriksson
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date$
 *           $Revision$
 */
package se.sics.mspsim.util;

public class Utils {
  private static final String str16 = "0000000000000000";

  public static String binary8(int data) {
    String s = Integer.toString(data, 2);
    if (s.length() < 8) {
      s = str16.substring(0, 8 - s.length()) + s;
    }
    return s;
  }

  public static String binary16(int data) {
    String s = Integer.toString(data, 2);
    if (s.length() < 16) {
      s = str16.substring(0, 16 - s.length()) + s;
    }
    return s;
  }

  public static String hex8(int data) {
    String s = Integer.toString(data & 0xff, 16);
    if (s.length() < 2) {
      s = str16.substring(0, 2 - s.length()) + s;
    }
    return s;
  }

  public static String hex16(int data) {
    String s = Integer.toString(data & 0xffff, 16);
    if (s.length() < 4) {
      s = str16.substring(0, 4 - s.length()) + s;
    }
    return s;
  }

  public static Object[] add(Class<?> componentType, Object[] array, Object value) {
    Object[] tmp;
    if (array == null) {
      tmp = (Object[]) java.lang.reflect.Array.newInstance(componentType, 1);
    } else {
      tmp = (Object[]) java.lang.reflect.Array.newInstance(componentType, array.length + 1);
      System.arraycopy(array, 0, tmp, 0, array.length);
    }
    tmp[tmp.length - 1] = value;
    return tmp;
  }

  public static Object[] remove(Object[] array, Object value) {
    if (array != null) {
      for (int index = 0, n = array.length; index < n; index++) {
        if (value.equals(array[index])) {
          if (n == 1) {
            return null;
          }
          Object[] tmp = (Object[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), array.length - 1);
          if (index > 0) {
            System.arraycopy(array, 0, tmp, 0, index);
          }
          if (index < tmp.length) {
            System.arraycopy(array, index + 1, tmp, index, tmp.length - index);
          }
          return tmp;
        }
      }
    }
    return array;
  }

  public static int decodeInt(String value) throws NumberFormatException {
    int radix = 10;
    int index = 0;
    boolean negative = false;
    if (value.startsWith("-")) {
      index++;
      negative = true;
    }

    if (value.startsWith("$", index) || value.startsWith("#", index)) {
      radix = 16;
      index++;
    } else if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
      radix = 16;
      index += 2;
    } else if (value.startsWith("0", index) && value.length() > index + 1) {
      radix = 8;
      index++;
    } else if (value.startsWith("%", index)) {
      radix = 2;
      index++;
    }
    String intValue = value;
    if (radix != 10) {
      if (value.startsWith("-", index)) {
        throw new NumberFormatException("unexpected negative sign: " + value);
      }
      if (negative) {
        intValue = '-' + value.substring(index);
      } else {
        intValue = value.substring(index);
      }
    }
    return Integer.parseInt(intValue, radix);
  }

  public static long decodeLong(String value) throws NumberFormatException {
    int radix = 10;
    int index = 0;
    boolean negative = false;
    if (value.startsWith("-")) {
      index++;
      negative = true;
    }

    if (value.startsWith("$", index) || value.startsWith("#", index)) {
      radix = 16;
      index++;
    } else if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
      radix = 16;
      index += 2;
    } else if (value.startsWith("0", index) && value.length() > index + 1) {
      radix = 8;
      index++;
    } else if (value.startsWith("%", index)) {
      radix = 2;
      index++;
    }
    String longValue = value;
    if (radix != 10) {
      if (value.startsWith("-", index)) {
        throw new NumberFormatException("unexpected negative sign: " + value);
      }
      if (negative) {
        longValue = '-' + value.substring(index);
      } else {
        longValue = value.substring(index);
      }
    }
    return Long.parseLong(longValue, radix);
  }

  /* converts hexa-decimal data in a string to an array of bytes */
  public static byte[] hexconv(String line) {
    if (line != null) {
      byte[] data = new byte[line.length() / 2]; 
      int hpos = 0;
      int totVal = 0;
      int dataPos = 0;
      for (int i = 0, n = line.length(); i < n; i++) {
        int val = line.charAt(i);
        if (val >= '0' && val <= '9') {
          val = val - '0';
        } else if (val >= 'a' && val <= 'f') {
          val = val + 10 - 'a';  
        } else if (val >= 'A' && val <= 'F'){
          val = val + 10 - 'A';  
        } else {
          throw new IllegalArgumentException("Illegal format of string to convert: " + line);
        }
        
        if (hpos == 0) {
          totVal = val << 4;
          hpos++;
        } else {
          totVal = totVal + val;
          hpos = 0;
          data[dataPos++] = (byte) (totVal & 0xff);
        }
      }
      return data;
    }
    return null;
  }

//  public static void main(String[] args) {
//    System.out.println("Hex 47 = " + hex8(47));
//  }

}
