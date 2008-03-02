/**
 * Copyright (c) 2008, Swedish Institute of Computer Science.
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
 * -----------------------------------------------------------------
 *
 * CommandParser
 *
 * Author  : Joakim Eriksson, Niclas Finne
 * Created : Sun Mar 2 19:41:00 2008
 * Updated : $Date: 2007/10/21 21:17:34 $
 *           $Revision: 1.3 $
 */
package se.sics.mspsim.util;
import java.util.ArrayList;

public class CommandParser {

  private static final int TEXT = 0;
  private static final int ARG = 1;
  private static final int QUOTE = 2;

  private CommandParser() {
    // Prevent instances of this class
  }

  public static String[][] parseLine(String line) {
    ArrayList<String[]> list = new ArrayList<String[]>();
    ArrayList<String> args = new ArrayList<String>();
    StringBuilder sb = null;
    int state = TEXT;
    int index = 0;
    char quote = 0;

    for (int i = 0, n = line.length(); i < n; i++) {
      char c = line.charAt(i);
      if (c <= 32) {
	// White space
	if (state == ARG) {
	  if (sb == null) {
	    args.add(line.substring(index, i));
	  } else {
	    args.add(sb.append(line.substring(index, i)).toString());
	    sb = null;
	  }
	  state = TEXT;
	}

      } else {
	switch (c) {
	case '\\':
	  i++;
	  if (i >= n) {
	    throw new IllegalArgumentException("unexpected end of line");
	  }
	  if (state == TEXT) {
	    state = ARG;
	  } else {
	    if (sb == null) {
	      sb = new StringBuilder();
	    }
	    sb.append(line.substring(index, i - 1));
	  }
	  index = i;
	  break;
	case '"':
	case '\'':
	  if (state == QUOTE) {
	    if (c == quote) {
	      // End of quote
	      if (sb == null) {
		args.add(line.substring(index, i));
	      } else {
		args.add(sb.append(line.substring(index, i)).toString());
		sb = null;
	      }
	      state = TEXT;
	    }
	  } else {
	    // Start new quote
	    if (state != TEXT) {
	      if (sb == null) {
		args.add(line.substring(index, i));
	      } else {
		args.add(sb.append(line.substring(index, i)).toString());
		sb = null;
	      }
	    }
	    index = i + 1;
	    state = QUOTE;
	    quote = c;
	  }
	  break;
	case '|':
	  if (state != QUOTE) {
	    // PIPE
	    if (state == ARG) {
	      if (sb == null) {
		args.add(line.substring(index, i));
	      } else {
		args.add(sb.append(line.substring(index, i)).toString());
		sb = null;
	      }
	    }
	    state = TEXT;
	    if (args.size() == 0) {
	      throw new IllegalArgumentException("empty command");
	    }
	    list.add(args.toArray(new String[args.size()]));
	    args.clear();
	  }
	  break;
	default:
	  if (state == TEXT) {
	    index = i;
	    state = ARG;
	  }
	  break;
	}
      }
    }
    if (state == QUOTE) {
      throw new IllegalArgumentException("unexpected end of line");
    }
    if (state == ARG) {
      if (sb == null) {
	args.add(line.substring(index));
      } else {
	args.add(sb.append(line.substring(index)).toString());
      }
    }
    if (args.size() > 0) {
      list.add(args.toArray(new String[args.size()]));
    }
    return list.toArray(new String[list.size()][]);
  }

//   public static void main(String[] args) {
//     StringBuilder sb = new StringBuilder();
//     for (int i = 0, n = args.length; i < n; i++) {
//       if (i > 0) sb.append(' ');
//       sb.append(args[0]);
//     }
//     String[][] list = parseLine(sb.toString());
//     for (int j = 0, m = list.length; j < m; j++) {
//       String[] a = list[j];
//       System.out.println("PARSED LINE:");
//       for (int i = 0, n = a.length; i < n; i++) {
// 	System.out.println("  ARG " + (i + 1) + ": '" + a[i] + '\'');
//       }
//     }
//   }

}
