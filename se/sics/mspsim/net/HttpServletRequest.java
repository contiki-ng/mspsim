package se.sics.mspsim.net;

import java.io.InputStream;

public class HttpServletRequest {

    private TCPConnection connection;
    private String method;
    private String path;
    
    public HttpServletRequest(TCPConnection connection, String method, String path) {
	this.connection = connection;
	this.method = method;
	this.path = path;
    }
    
    public String getMethod() {
	return method;
    }
    
    public String getPath() {
	return path;
    }
    
    public InputStream getInputStream() {
	return connection.getInputStream();
    }
}
