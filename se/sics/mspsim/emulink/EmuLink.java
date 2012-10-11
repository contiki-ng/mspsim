/*
 * Copyright (c) 2012, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
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
 * EmuLink
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : 11 oct 2012
 */

package se.sics.mspsim.emulink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

import se.sics.json.JSONArray;
import se.sics.json.JSONObject;
import se.sics.json.ParseException;
import se.sics.mspsim.Main;
import se.sics.mspsim.platform.GenericNode;

public class EmuLink {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean isConnected = false;

    int mode;
    int json = 0;
    StringBuilder buffer = new StringBuilder();

    Hashtable<String, GenericNode> nodes = new Hashtable<String, GenericNode>();
    
    public boolean isConnected() {
        return !isConnected;
    }

    String[] getNodes(JSONObject json) {
        JSONArray nodes;
        String node;
        String[] nString = null;
        if ((nodes = json.getJSONArray("node")) != null) {
            nString = new String[nodes.size()];
            for(int i = 0, n = nodes.size(); i < n; i++) {
                node = nodes.getAsString(i);
                nString[i] = node;
            }
        } else if ((node = json.getAsString("node")) != null) {
            nString = new String[1];
            nString[0] = node;
        }
        return nString;
    }
    
    private boolean createNode(String type, String id) {
        String nt = Main.getNodeTypeByPlatform(type);
        System.out.println("Creating node: " + id + " type: " + type + " => " + nt);
        GenericNode node = Main.createNode(nt);
        nodes.put(id, node);
        return true;
    }
    
    private boolean createNodes(JSONObject json) {
        String type = json.getAsString("type");
        System.out.println("Should create: " + type);

        String[] nodes = getNodes(json);
        if (nodes != null) {
            for (int i = 0; i < nodes.length; i++) {
                createNode(type, nodes[i]);
            }
        }
        return true;
    }

    protected void processInput(Reader input) throws IOException, ParseException {
        StringBuilder sb = new StringBuilder();
        int brackets = 0;
        boolean stuffed = false;
        boolean quoted = false;
        while (isConnected()) {
            int c = input.read();
            if (c < 0) {
                disconnect();
                break;
            }
            sb.append((char)c);
            if (stuffed) {
                stuffed = false;
            } else if (c == '\\') {
                stuffed = true;
            } else if (quoted) {
                if (c == '"') {
                    quoted = false;
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == '{') {
                brackets++;
            } else if (c == '}') {
                brackets--;
                if (brackets == 0) {
                    JSONObject json = JSONObject.parseJSONObject(sb.toString());
                    sb.setLength(0);
                    if (!handleMessage(json)) {
                        // This connection should no longer be kept alive
                        break;
                    }
                }
            }
        }
    };

    protected boolean handleMessage(JSONObject json) {
        System.out.println("EmuLink: RECV " + json.toJSONString());
        String event = json.getAsString("event");
        if ("emulation_control".equals(event)) {
            // TODO control emulation
            sendToSimulator("{\"response\":\"emulation_control\",\"data\":1}");

            if ("close".equals(json.getAsString("data"))) {
                // Time to close the connection
                return false;
            }
        } else if ("create".equals(event)) {
            // TODO setup emulated node
            createNodes(json);
            sendToSimulator("{\"response\":\"create\",\"data\":1}");
        } else if ("serial".equals(event)) {
            int data = json.getAsInt("data", -1);
            JSONArray nodes;
            String node;
            if (data < 0) {
                // No data - ignore serial event
            } else if ((nodes = json.getJSONArray("node")) != null) {
                for(int i = 0, n = nodes.size(); i < n; i++) {
                    node = nodes.getAsString(i);
                    if (node != null) {
                        sendSerialToNode(node, data);
                    }
                }
            } else if ((node = json.getAsString("node")) != null) {
                sendSerialToNode(node, data);
            } else {
                // No target node specified
            }
        } else {
            System.err.println("EmuLink: ignoring unhandled event '" + event + "'");
        }
        return true;
    }

    protected void sendToSimulator(String message) {
        if (out != null) {
            out.write(message);
            out.flush();
        }
    }

    protected void sendSerialToNode(String node, int data) {
    }

    protected void disconnect() {
        boolean wasConnected = isConnected;
        isConnected = false;
        try {
            if (wasConnected) {
                System.err.println("EmuLink: disconnecting...");
            }
            if (out != null) {
                out.close();
                out = null;
            }
            if (in != null) {
                in.close();
                in = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
            if (wasConnected) {
                System.err.println("EmuLink: disconnected");
            }
        } catch (IOException e) {
            System.err.println("EmuLink: failed to close emulation link connection");
            e.printStackTrace();
        }
    }

    public void run() throws IOException {
        try {
            ServerSocket serverSocket = new ServerSocket(8000);

            while(true) {

                System.out.println("EmuLink: Waiting for connection...");
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    System.out.println("Accept failed: 8000");
                    System.exit(-1);
                }
                System.out.println("EmuLink: Connection accepted...");

                try {
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    processInput(in);
                } catch (Exception e) {
                    System.err.println("EmuLink: emulator link connection failed");
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            }
        } catch (IOException e) {
            System.out.println("Could not listen on port: 8000");
            System.exit(-1);
        }
    }

    public static void main(String[] args) throws IOException {
        EmuLink el = new EmuLink();
        el.run();
    }
}
