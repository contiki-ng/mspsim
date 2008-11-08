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
 * $Id: $
 *
 * -----------------------------------------------------------------
 *
 * GDBStubs
 *
 * Author  : Joakim Eriksson
 * Created : 31 mar 2008
 * Updated : $Date:$
 *           $Revision:$
 */
package se.sics.mspsim.util;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import se.sics.mspsim.core.MSP430Core;

public class GDBStubs implements Runnable {

  private final static String OK = "OK";

  ServerSocket serverSocket;
  OutputStream output;
  MSP430Core cpu;
  
  public void setupServer(MSP430Core cpu, int port) {
    this.cpu = cpu;
    try {
      serverSocket = new ServerSocket(port);
      System.out.println("GDBStubs open server socket port: " + port);
      new Thread(this).start();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void run() {
    while (true) {
      try {
        Socket s = serverSocket.accept();

        DataInputStream input = new DataInputStream(s.getInputStream());
        output = s.getOutputStream();

        String cmd = "";
        boolean readCmd = false;
        while (s != null) {
          int c = input.read();
          if (c == -1) return;
          System.out.println("GDBStubs: Read  " + c + " => " + (char) c);
          if (c == '#') {
            readCmd = false;
            handleCmd(cmd);
            cmd = "";
          }
          if (readCmd) {
            cmd += (char) c;
          }
          if (c == '$') {
            readCmd = true;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void handleCmd(String cmd) throws IOException {
    System.out.println("cmd: " + cmd);
    char c = cmd.charAt(0);
    switch(c) {
    case 'H':
      sendResponse(OK);
      break;
    case 'q':
      if ("qC".equals(cmd)) {
        sendResponse("QC1");
      } else if ("qOffsets".equals(cmd)) {
        sendResponse("Text=000;Data=000;Bss=000;");
      }
      break;
    case '?':
      sendResponse("S01");
      break;
    case 'g':
      readRegisters();
      break;
    case 'k': // kill
      sendResponse(OK);
      break;
    case 'm':
      String cmd2 = cmd.substring(1);
      String parts[] = cmd2.split(",");
      int addr = Integer.decode("0x" + parts[0]);
      int len = Integer.decode("0x" + parts[1]);
      String data = "";
      for (int i = 0; i < len; i++) {
        data += Utils.hex8(cpu.memory[addr++] & 0xff);
      }
      sendResponse(data);
    }
  }

  private void readRegisters() throws IOException {
    String regs = "";
    for (int i = 0; i < 16; i++) {
      regs += Utils.hex8(cpu.reg[i] & 0xff) + Utils.hex8(cpu.reg[i] >> 8);
    }
    sendResponse(regs);
  }

  public void sendResponse(String resp) throws IOException {
    output.write('+');
    output.write('$');
    int cs = 0;
    if (resp != null) {
      for (int i = 0; i < resp.length(); i++) {
	output.write((char) resp.charAt(i));
	System.out.print(resp.charAt(i));
	cs += resp.charAt(i);
      }
    }
    output.write('#');
    System.out.print('#');
    int c = (cs & 0xff) >> 4;
    if (c < 10) c = c + '0';
    else c = c - 10 + 'a';
    output.write((char)c);
    System.out.print((char)c);
    c = cs & 15;
    if (c < 10) c = c + '0';
    else c = c - 10 + 'a';
    output.write((char)c);
    System.out.println((char)c);
  }
}
