/*
 * Created on Mar 25, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rice.pastry.socket.messaging;

import java.net.InetSocketAddress;

/**
 * @author jeffh
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class AddressMessage extends SocketControlMessage {

  public InetSocketAddress address;

	/**
	 * @param address
	 */
	public AddressMessage(InetSocketAddress address) {
		this.address = address;
	}

}