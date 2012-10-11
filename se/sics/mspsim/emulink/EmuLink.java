package se.sics.mspsim.emulink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EmuLink {

    
    PrintWriter out;
    boolean close = false;

    
    int mode;
    int json = 0;
    StringBuffer buffer = new StringBuffer();
    
    void processInput(int data) {
        System.out.println("Data: " + data);
        /* create JSON here */
    };
    
    public void run() throws IOException {
        try {
            ServerSocket serverSocket = null;
            serverSocket = new ServerSocket(8000);

            while(true) {

                System.out.println("EmuLink: Waiting for connection...");
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                } 
                catch (IOException e) {
                    System.out.println("Accept failed: 8000");
                    System.exit(-1);
                }
                System.out.println("EmuLink: Connection accepted...");

                out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                int c;
                out.println("EmuLink - mspsim");
                while (!close && (c = in.read()) != -1) {   
                    processInput(c);
                }
                
                in.close();
                out.close();
                socket.close();
                close = false;
            }
        } 
        catch (IOException e) {
            System.out.println("Could not listen on port: 8000");
            System.exit(-1);
        }
    }
    
    public static void main(String[] args) throws IOException {
        EmuLink el = new EmuLink();
        el.run();
    }
}
