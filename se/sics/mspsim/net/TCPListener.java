package se.sics.mspsim.net;

public interface TCPListener {
  /* only for notifying if this is a listen connection and someone connect... */
  public void newConnection(TCPConnection connection);
  /* called whenever this connection was disconnected/closed */
  public void connectionClosed(TCPConnection connection);
  /* called when there is new TCP data on the connection */
  public void tcpDataReceived(TCPConnection source, TCPPacket packet);
}
