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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.soap.Node;

import se.sics.mspsim.core.EmulationException;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.Memory;
import se.sics.mspsim.core.MemoryMonitor;
import se.sics.mspsim.core.Memory.AccessMode;
import se.sics.mspsim.core.Memory.AccessType;
import se.sics.mspsim.platform.GenericNode;

public class GDBStubs implements Runnable {

	private static final int SIGTRAP = 5; // Trace/breakpoint trap.
	private static final int SIGCONT = 25; // Continue stopped process

	private final static String OK = "OK";

	private MemoryMonitor monitor;
	ServerSocket serverSocket;
	OutputStream output;
	MSP430 cpu;
	GenericNode node;
	ELF elf;

	public void setupServer(GenericNode node, MSP430 cpu, int port, ELF elf) {
		this.cpu = cpu;
		this.node = node;
		this.elf = elf;

		try {
			serverSocket = new ServerSocket(port);
			System.out.println("GDBStubs open server socket port: " + port);
			setMonitor();
			new Thread(this).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	List<Integer> buffer = new ArrayList<Integer>();
	Socket s;

	public void run() {
		while (true) {
			try {
				s = serverSocket.accept();

				node.stop();
				cpu.reset();

				DataInputStream input = new DataInputStream(s.getInputStream());
				output = s.getOutputStream();

				String cmd = "";
				boolean readCmd = false;
				int c;
				while (s != null && ((c = input.read()) != -1)) {
					// System.out.print((char)c);
					if (readCmd) {
						if (c == '#') {
							readCmd = false;
							output.write('+');
							handleCmd(cmd, (Integer [])buffer.toArray(new Integer[buffer.size()]), buffer.size());
							cmd = "";
							buffer.clear();
						} else {
							cmd += (char) c;
							buffer.add(c & 0xff);
							if (c == '$') {
								System.out.println("GDBStubs: server send start $ without end #");
							}

						}
					} else {
						if (c == '$') {
							readCmd = true;
						} else if (c == '-') {
							System.out.println("GDBStubs: server send - (NAK) ");
						} else if (c == 3) {
							output.write('+');
							handleCmd("3", new Integer [0], 0);
						}

					}
				}
			} catch (IOException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			} catch (EmulationException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private void stepSource() {
		node.stop();
		node.step(1);
		}

	/**
	 * @param cmd
	 * @param cmdBytes
	 * @param cmdLen
	 * @throws IOException
	 * @throws EmulationException
	 */
	/**
	 * @param cmd
	 * @param cmdBytes
	 * @param cmdLen
	 * @throws IOException
	 * @throws EmulationException
	 */
	private void handleCmd(String cmd, Integer[] cmdBytes, int cmdLen)
			throws IOException, EmulationException {
//		System.out.println("cmd: " + cmd);
		char c = cmd.charAt(0);
		String parts[];
		switch (c) {
		case 'H':
			sendResponse("", "command unknown");
			break;
		case 'v':
			if ("vCont?".equals(cmd)) {
				sendResponse("", "vCont not supported (only needed for multithreading)");
			} else {
				sendResponse("", "command unknown");
			}
			break;
		case 'q':
			if ("qC".equals(cmd)) {
				sendResponse("", "command unknown");
			} else if ("qOffsets".equals(cmd)) {
				sendResponse("", "command unknown");
			} else if ("qfThreadInfo".equals(cmd)) {
				sendResponse("<?xml version\"1.0\"?><threads></threads>");
			} else if ("qsThreadInfo".equals(cmd)) {
				sendResponse("l");
			} else if ("qAttached".equals(cmd)) {
				sendResponse("", "command unknown");
			} else if ("qSymbol::".equals(cmd)) {
				sendResponse("", "command unknown");
			} else if ("qTStatus".equals(cmd)) {
				sendResponse("", "command unknown");
			} else if (cmd.contains("qRcmd,")) {
				String Text = hexTostring(cmd.substring(6));
				if (Text.equals("erase all")) {
					sendResponse(stringToHex("Erasing..."), "Erasing...");
				} else if (Text == "reset") {
					cpu.reset();
					sendResponse(stringToHex("Resetting..."), "Resetting...");
				} else if (Text == "erase") {
					sendResponse(stringToHex("Erasing..."), "Erasing...");
				} else {
					sendResponse(stringToHex("Unbekannt..."), "Unbekannt...");
				}
			} else if (cmd.contains("qSupported:")) {
				sendResponse("PacketSize=4000", "packet size 4000 bytes");
			} else {
				sendResponse("", "command unknown");
			}
			break;
		case '?':
			if(cpu.isRunning()){
				sendRegisters2(SIGCONT);
			} else {
				sendRegisters2(SIGTRAP);
			}
			break;
		case 'G':
			writeRegisters(cmd.substring(1));
			break;
		case 'g':
			sendRegisters();
			break;
		case 'P': // write Register
			sendResponse("", "command unknown");
/*			parts = cmd.split("=");
			if (parts.length == 2) {
				String Val = parts[1].substring(2, 4) + parts[1].substring(0, 2);
				cpu.writeRegister(Integer.parseInt(parts[0].substring(1)),
						Integer.parseInt(Val, 16));
				sendResponse(OK, "register written");
			}*/
			break;
		case 'k': // kill
			sendResponse(OK, "kill task");
			s.close();
			s = null;
			System.exit(0); 
			break;
		case '3': // Pause
		case 's':
		case 'S':
			stepSource();
			sendRegisters2(SIGTRAP);
			break;
		case 'c':
		case 'C':
			node.start();
			break;
		case 'Z':
			parts = cmd.split(",");
			setBreakpoint(Integer.parseInt(parts[1], 16));
			sendResponse(OK, "set breakpoint at 0x" + parts[1]);
			break;
		case 'z':
			parts = cmd.split(",");
			removeBreakpoint(Integer.parseInt(parts[1], 16));
			sendResponse(OK, "remove breakpoint at 0x" + parts[1]);
			break;
		case 'X':
			sendResponse("", "command unknown");
			break;
		case 'm':
		case 'M':
			String cmd2 = cmd.substring(1);
			String wdata[] = cmd2.split(":");
			int cPos = cmd.indexOf(':');
			if (cPos > 0) {
				/* only until length in first part */
				cmd2 = wdata[0];
			}
			parts = cmd2.split(",");
			
			int addr = Integer.decode("0x" + parts[0]);
			int len = Integer.decode("0x" + parts[1]);
			String data = "";
			Memory mem = cpu.getMemory();

			if (c == 'm') {
				System.out.println("Returning memory from: 0x" + Integer.toHexString(addr) + " len = " + len);
				/* This might be wrong - which is the correct byte order? */
				for (int i = 0; i < len; i+=2) {
					String data2 = Utils.hex16(mem.get(addr+i, Memory.AccessMode.WORD));
					data+=data2.substring(2);
					data+=data2.substring(0, 2);
				}
				sendResponse(data);
			} else {
				System.out.println("Writing to memory at: " + Integer.toHexString(addr) + " len = " + len + " with: "
						+ ((wdata.length > 1) ? wdata[1] : ""));
				
				for(int i=0;i<wdata[1].length()/2;i++){
					int high=Integer.decode("0x"+wdata[1].substring(2*i, 2*i+1));
					int low=Integer.decode("0x"+wdata[1].substring(2*i+1, 2*i+2));
					int Val=low+high*16;
			         cpu.memory[addr+i]=Val;
				}
				sendResponse(OK);
			}
			break;
		default:
			sendResponse("", "Command unknown");
		}
	}

	private static String hexTostring(String hexValue) {
		StringBuilder output = new StringBuilder("");
		for (int i = 0; i < hexValue.length(); i += 2) {
			String str = hexValue.substring(i, i + 2);
			output.append((char) Integer.parseInt(str, 16));
		}
		return output.toString();
	}

	private void setBreakpoint(int pos) {
		if (!cpu.hasWatchPoint(pos)) {
			cpu.addWatchPoint(pos, monitor);
		}
	}

	private void removeBreakpoint(int pos) {
		if (cpu.hasWatchPoint(pos)) {
			cpu.removeWatchPoint(pos, monitor);
		}
	}

	private void setMonitor() {
		monitor = new MemoryMonitor.Adapter() {
			private long lastCycles = -1;

			@Override
			public void notifyReadBefore(int address, AccessMode mode, AccessType type) {
				if (type == AccessType.EXECUTE && cpu.cycles != lastCycles) {
					cpu.triggBreakpoint();
					lastCycles = cpu.cycles;
					try {
						sendRegisters2(SIGTRAP);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
	}

	
	private void writeRegisters(String data) throws IOException {
		System.err.println(data);
		if(data.length()!=(8*16)){
			sendResponse("", "Wrong length for write register");
			return;
		}
		
		for(int i=0;i<16;i++){
			String packet=data.substring(8*i, 8*i+7);
			int low = Integer.decode("0x" + packet.substring(0,1));
			int high = Integer.decode("0x" + packet.substring(2,3));
			cpu.reg[i]=high*256+low;
		}
		sendResponse(OK, "write register");
	}	
	
	private void sendRegisters() throws IOException {
		String regs = "";
		for (int i = 0; i < 16; i++) {
			regs += Utils.hex8(cpu.reg[i] & 0xff) + Utils.hex8(cpu.reg[i] >> 8) + "0000";
		}
		sendResponse(regs);
	}

	// synchronized because of asynchron breakpoint call
	private synchronized void sendRegisters2(int Signal) throws IOException {
		String regs = "T" + String.format("%02d", Signal);
		for (int i = 0; i < 16; i++) {
			regs += Utils.hex8(i) + ":" + Utils.hex8(cpu.reg[i] & 0xff)
			+ Utils.hex8(cpu.reg[i] >> 8) + "0000;";
		}
		sendResponse(regs);
	}

	private static String stringToHex(String asciiValue) {
		char[] chars = asciiValue.toCharArray();
		StringBuffer hex = new StringBuffer();
		for (int i = 0; i < chars.length; i++) {
			hex.append(Integer.toHexString((int) chars[i]));
		}
		return hex.toString();
	}

	public void sendResponse(String resp) throws IOException {
		sendResponse(resp, "");
	}

	public void sendResponse(String resp, String info) throws IOException {
//		System.out.print("ans2: ");
		String a = "";
		a += '$';
		int cs = 0;
		if (resp != null) {
			for (int i = 0; i < resp.length(); i++) {
				a += resp.charAt(i);
//				System.out.print(resp.charAt(i));
				cs += resp.charAt(i);
			}
		}
		a += '#';
		int c = (cs & 0xff) >> 4;
			if (c < 10) {
				c = c + '0';
			} else {
				c = c - 10 + 'a';
			}
			a += (char) c;

			c = cs & 15;
			if (c < 10) {
				c = c + '0';
			} else {
				c = c - 10 + 'a';
			}
			a += (char) c;
/*			if (info == "") {
				System.out.println(" (" +  bytesToHexString(a.getBytes()) +")");
			} else {
				System.out.println(" (" + info + "::" + bytesToHexString(a.getBytes()) +")");
			}
*/			output.write(a.getBytes());

	}



	public static String bytesToHexString(byte[] bytes){
		StringBuilder sb = new StringBuilder();
		for(byte b : bytes){
			sb.append(String.format("%02x ", b&0xff));
		}
		return sb.toString();
	}  


}