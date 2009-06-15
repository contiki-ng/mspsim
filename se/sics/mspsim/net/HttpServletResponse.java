package se.sics.mspsim.net;

import java.io.OutputStream;

public class HttpServletResponse {

    private TCPConnection tcpConnection;
    
    public HttpServletResponse(TCPConnection c) {
	tcpConnection = c;
    }
    
    public OutputStream getOutputStream() {
	return tcpConnection.getOutputStream();
    }
    
    
}
