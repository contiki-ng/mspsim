/**
 * 
 */
package se.sics.mspsim.net;
import se.sics.mspsim.util.Utils;

/**
 * @author joakim
 *
 */
public class NeighborTable {
  // currently supports max 64 neighbors...
  Neighbor[] neighbors = new Neighbor[64];
  int neighborCount = 0;
  
  Neighbor defrouter;
  
  public synchronized Neighbor addNeighbor(byte[] ipAddress, byte[] linkAddress) {
    Neighbor nb = getNeighbor(ipAddress);
    if (nb == null) {
      if (checkIPAddress(ipAddress)) {
        nb = new Neighbor();
        nb.ipAddress = ipAddress;
        nb.linkAddress = linkAddress;
        nb.state = checkLinkAddress(linkAddress) ? Neighbor.STALE : Neighbor.INCOMPLETE;
        if (neighborCount < neighbors.length) {
          neighbors[neighborCount++] = nb;
        } else {
          // TODO select suitable neighbor to replace
          neighbors[0] = nb;
        }
      }
    } else {
      /* Neighbor already in neighbor table */
      nb.linkAddress = linkAddress;
      nb.state = Neighbor.INCOMPLETE;
    }
    return nb;
  }
  
  private boolean checkLinkAddress(byte[] link) {
    if (link == null) return false;
    /* is there any other non-ok address ?? */
    return true;
  }
  private boolean checkIPAddress(byte[] ipAddress) {
    /* can not add unspecified IP addresses */
    if (Utils.equals(ipAddress, IPStack.UNSPECIFIED)) return false;
    /* are all other ok? */
    return true;
  }

  public Neighbor getDefrouter() {
    return defrouter;
  }
  
  public void setDefrouter(Neighbor neighbor) {
    defrouter = neighbor;
  }
  
  public synchronized boolean removeNeighbor(Neighbor nb) {
    for (int i = 0; i < neighborCount; i++) {
      if (nb == neighbors[i]) {
        // move last element forward to this position...
        neighbors[i] = neighbors[neighborCount - 1];
        neighborCount--;
        return true;
      }
    }
    return false;
  }
  
  public Neighbor getNeighbor(byte[] ipAddress) {
    int neighborCount0 = neighborCount;
    Neighbor[] neis = neighbors;
    for (int i = 0; i < neighborCount0; i++) {
      if (Utils.equals(ipAddress, neis[i].ipAddress)) {
        return neis[i];
      }
    }
    return null;
  }
}
