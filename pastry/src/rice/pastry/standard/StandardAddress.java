
package rice.pastry.standard;

import rice.pastry.*;
import rice.pastry.messaging.*;

import java.util.Random;
import java.security.*;

/**
 * Constructs an address for a specific class and instance name.
 *
 * @version $Id$
 *
 * @author Alan Mislove
 */
public class StandardAddress implements Address {

  protected int myCode;
  
  protected String name;

  public StandardAddress(int port) {
    this.myCode = port;
  }
  
  public StandardAddress(Class c, String instance) {
    MessageDigest md = null;
    
    try {
      md = MessageDigest.getInstance("SHA");
    } catch ( NoSuchAlgorithmException e ) {
      System.err.println( "No SHA support!" );
    }
    
    name = c.toString() + "-" + instance;

    md.update(name.getBytes());
    byte[] digest = md.digest();

    myCode = (digest[0] << 24) + (digest[1] << 16) +
             (digest[2] << 8) + digest[3];

  }

  public int hashCode() {
    return myCode;
  }

  public boolean equals(Object obj) {
    if (obj instanceof StandardAddress) {
      return ((StandardAddress) obj).myCode == myCode;
    } else {
      return false;
    }
  }
  
  public String toString() {
    return "[StandardAddress: " + name + "]";
  }
}

