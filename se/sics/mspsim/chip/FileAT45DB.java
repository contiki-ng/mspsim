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
 * $Id:  $
 *
 * -----------------------------------------------------------------
 *
 * FileAT45DB - File based implementation of external flash.
 *
 * Author  : Joakim Eriksson, Fredrik Osterlind
 * Created : Sun Oct 21 22:00:00 2007
 * Updated : $Date: 2008-05-12 18:10:17 +0000 (Mon, 12 May 2008) $
 *           $Revision: 280 $
 */

package se.sics.mspsim.chip;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import se.sics.mspsim.core.MSP430Core;

public class FileAT45DB extends AT45DB {

  private static final boolean DEBUG = true;

  // PAGE_SIZE and NUM_PAGES defined in AT45DB
  private static final int FLASH_SIZE = PAGE_SIZE * NUM_PAGES;
  
  private RandomAccessFile file;
  private FileChannel fileChannel;
  private FileLock fileLock;

  public FileAT45DB(MSP430Core cpu, String filename) {
    super(cpu);
    if (filename == null) {
      filename = "flash.bin";
    }

    // Open flash file for R/W
    if (!openFile(filename)) {
      // Failed to open/lock the specified file. Add a counter and try with next filename.
      Matcher m = Pattern.compile("(.+?)(\\d*)(\\.[^.]+)").matcher(filename);
      if (m.matches()) {
        String baseName = m.group(1);
        String c = m.group(2);
        String extName = m.group(3);
        int count = 1;
        if (c != null && c.length() > 0) {
          count = Integer.parseInt(c) + 1;
        }
        for (int i = 0; !openFile(baseName + count + extName) && i < 100; i++, count++);
      }
    }
    if (fileLock == null) {
      // Failed to open flash file
      throw new IllegalStateException("failed to open flash file '" + filename + '\'');
    }
    // Set size of flash
    try {
      file.setLength(FLASH_SIZE);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private boolean openFile(String filename) {
    // Open flash file for R/W
    try {
      file = new RandomAccessFile(filename, "rw");
      fileChannel = file.getChannel();
      fileLock = fileChannel.tryLock();
      if (fileLock != null) {
        // The file is now locked for use
        if (DEBUG) System.out.println("FileAT45DB: using flash file '" + filename + '\'');
        return true;
      } else {
        fileChannel.close();
        return false;
      }
    } catch (IOException e) {
      e.printStackTrace();
      closeFile();
      return false;
    }
  }

  private void closeFile() {
    try {
      if (fileLock != null) {
        fileLock.release();
        fileLock = null;
      }
      if (fileChannel != null) {
        fileChannel.close();
        fileChannel = null;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void seek(long pos) throws IOException {
    file.seek(pos);
  }

  public int read(byte[] b) throws IOException {
    return file.read(b);
  }

  public void write(byte[] b) throws IOException {
    file.write(b);
  }

} // FileAT45DB
