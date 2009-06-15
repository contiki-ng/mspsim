package se.sics.mspsim.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

public class HttpServer implements TCPListener, Runnable{

    private IPStack ipStack;
    private TCPConnection serverConnection;    
    private Hashtable servlets = new Hashtable();
    private Vector pending = new Vector();
    
    public HttpServer(IPStack stack) {
	ipStack = stack;
	serverConnection = ipStack.listen(80);
	serverConnection.setTCPListener(this);
	new Thread(this).start();
    }

    public void connectionClosed(TCPConnection connection) {
    }

    public void newConnection(TCPConnection connection) {
	handleConnection(connection);
    }

    public void tcpDataReceived(TCPConnection source, TCPPacket packet) {
    }
    
    public void registerServlet(String path, HttpServlet servlet) {
	servlets.put(path, servlet);
    }
    
    private synchronized void handleConnection(TCPConnection connection) {
	/* add and notify worker thread */
	System.out.println("%%% HttpServer: gotten new connection, adding to pending...");
	pending.addElement(connection);
	notify();
    }
    
    private void handlePendingConnections() {
	while(true) {
	    TCPConnection connection = null;
	    synchronized(this) {
		while(pending.size() == 0)
		    try {
			System.out.println("%%% HttpServer: worker waiting...");
			wait();
			/* take first and handle... */
			System.out.println("%%% HttpServer: worker notified...");
		    } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		    connection = (TCPConnection) pending.firstElement();
		    pending.removeElementAt(0);
	    }
	    InputStream input = connection.getInputStream();
	    OutputStream output = connection.getOutputStream();
	    try {
		/* read a line */
		System.out.println("%%% HttpServer: reading req line from: " + input);
		String reqLine = readLine(input);
		reqLine = reqLine.trim();
		int space = reqLine.indexOf(' ');
		String method = reqLine.substring(0, space);
		String path = reqLine.substring(space + 1, reqLine.lastIndexOf(' '));
		System.out.println("Method: " + method);
		System.out.println("Path: " + path);
		int query = reqLine.indexOf('?');
		if (query > 0) {
		    path = path.substring(0, query);
		}
		HttpServlet servlet = (HttpServlet) servlets.get(path);
		if (servlet == null) {
		    output.write("HTTP/1.0 404 NOT FOUND\r\n\r\n".getBytes());
		    output.close();
		    input.close();
		    connection.close();
		} else {
		    String line = null;
		    while((line = readLine(input)) != null) {
			line = line.trim();
			System.out.println("/// HTTP Header: " + line);
			if (line.length() == 0) {
			    break;
			}
		    }
		    HttpServletRequest req = new HttpServletRequest(connection, method, path);
		    HttpServletResponse resp = new HttpServletResponse(connection);
		    servlet.service(req, resp);
		}
		
	    } catch (IOException e) {
		e.printStackTrace();
	    } finally {
		try {
		    output.close();
		} catch (IOException e) {
		}
		connection.close();
	    }
	}
    }
    
    public void run() {
	System.out.println("%%% HttpServer: worker thread started...");
	handlePendingConnections();
    }

    private String readLine(InputStream input) throws IOException {
	StringBuffer sb = new StringBuffer();
	int c;
	while(((c = input.read()) != -1)) {
	    if (c != '\r') sb.append((char) c);
	    if (c == '\n') return sb.toString();
	}
	return null;
    }
    
}
