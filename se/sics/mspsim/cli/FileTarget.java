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
 * FileTarget
 *
 * Author  : Joakim Eriksson
 * Created : 14 mar 2008
 * Updated : $Date:$
 *           $Revision:$
 */
package se.sics.mspsim.cli;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * @author joakim
 */
public class FileTarget {

    private static final boolean DEBUG = false;

    private final Hashtable<String,FileTarget> fileTargets;
    private final String name;
    private final FileWriter out;
    private ArrayList<CommandContext> contexts = new ArrayList<CommandContext>();

    public FileTarget(Hashtable<String,FileTarget> fileTargets, String name,
            boolean append) throws IOException {
        this.fileTargets = fileTargets;
        this.out = new FileWriter(name, append);
        this.name = name;
        fileTargets.put(name, this);
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        synchronized (fileTargets) {
            if (contexts != null) {
                sb.append(" \tPIDs: [");
                for (int i = 0, n = contexts.size(); i < n; i++) {
                    int pid = contexts.get(i).getPID();
                    if (i > 0) {
                        sb.append(',');
                    }
                    if (pid < 0) {
                        sb.append('?');
                    } else {
                        sb.append(pid);
                    }
                }
                sb.append(']');
            }
        }
        return sb.toString();
    }

    public void lineRead(CommandContext context, String line) {
        if (line == null) {
            removeContext(context);
        } else {
            try {
                out.write(line);
                out.write('\n');
                out.flush();
            } catch (IOException e) {
                e.printStackTrace(context.err);
            }
        }
    }

    public void addContext(CommandContext context) {
        boolean added = false;
        synchronized (fileTargets) {
            if (contexts != null) {
                contexts.add(context);
                added = true;
                if (DEBUG) {
                    System.out.println("FileTarget: new writer to " + name
                            + " (" + contexts.size() + ')');
                }
            }
        }
        if (!added) {
            context.kill();
        }
    }

    public void removeContext(CommandContext context) {
        boolean close = false;
        synchronized (fileTargets) {
            if (contexts != null && contexts.remove(context)) {
                if (DEBUG) {
                    System.out.println("FileTarget: removed writer from "
                            + name + " (" + contexts.size() + ')');
                }
                if (contexts.size() == 0) {
                    close = true;
                }
            }
        }
        if (close) {
            close(false);
        }
    }

    public void close() {
        close(true);
    }

    private void close(boolean forceClose) {
        ArrayList<CommandContext> list;
        synchronized (fileTargets) {
            if (contexts == null) {
                // Already closed
                return;
            }
            if (contexts.size() > 0 && !forceClose) {
                // File still has connected writers.
                return;
            }
            list = contexts;
            contexts = null;
            if (fileTargets.get(name) == this) {
                fileTargets.remove(name);
                if (DEBUG) {
                    System.out.println("FileTarget: closed file " + name);
                }
            }
        }

        if (list != null) {
            // Close any connected writers
            for (CommandContext context : list) {
                context.kill();
            }
        }
        try {
            out.close();
        } catch (IOException e) {
            // Ignore close errors
        }
    }

}
