/*************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate 

Copyright 2002, Rice University. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither  the name  of Rice  University (RICE) nor  the names  of its
contributors may be  used to endorse or promote  products derived from
this software without specific prior written permission.

This software is provided by RICE and the contributors on an "as is"
basis, without any representations or warranties of any kind, express
or implied including, but not limited to, representations or
warranties of non-infringement, merchantability or fitness for a
particular purpose. In no event shall RICE or contributors be liable
for any direct, indirect, incidental, special, exemplary, or
consequential damages (including, but not limited to, procurement of
substitute goods or services; loss of use, data, or profits; or
business interruption) however caused and on any theory of liability,
whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even
if advised of the possibility of such damage.

********************************************************************************/

package rice.pastry.routing;

import rice.pastry.*;
import rice.pastry.messaging.*;

import java.io.*;

/**
 * The address of the route protocol at a pastry node.
 *
 * @version $Id$
 *
 * @author Andrew Ladd
 */

public class RouteProtocolAddress implements Address {
    private static final int myCode = 0x89ce110e;

    /**
     * Constructor.
     */
    
    public RouteProtocolAddress() {}

    public boolean equals(Object obj) {
	return (obj instanceof RouteProtocolAddress);
    }

    public int hashCode() {
	return myCode;
    }
    
    private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException 
    {}

    private void writeObject(ObjectOutputStream out)
	throws IOException, ClassNotFoundException 
    {}

    public String toString() { return "[RouteProtocolAddress]"; }
}