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

package rice.pastry.multiring;

import rice.pastry.*;
import rice.pastry.leafset.*;
import rice.pastry.routing.*;
import rice.pastry.messaging.*;

import java.net.*;

/**
 * This class represents a factory for MultiRing pastry nodes.  In order to
 * use this class, one should use the static method getMultiRingFactory(),
 * which will return a new factory ready for use.
 *
 * @version $Id$
 *
 * @author Alan Mislove
 */
public class MultiRingPastryNodeFactory extends PastryNodeFactory {

  private PastryNodeFactory factory;
  
  public MultiRingPastryNodeFactory(PastryNodeFactory factory) {
    this.factory = factory;
  }

  public PastryNode newNode(NodeHandle bootstrap) {
    MultiRingPastryNode node = new MultiRingPastryNode(factory.newNode(bootstrap));

    node.setBootstrap(bootstrap);
    node.setMessageDispatch(new MultiRingMessageDispatch(node, node.getMessageDispatch()));
    
    return node;
  }

  public PastryNode newNode(NodeHandle bootstrap, NodeId nodeId) {
    return newNode(bootstrap, nodeId, true);
  }

  private PastryNode newNode(NodeHandle bootstrap, NodeId nodeId, boolean setDone) {
    MultiRingPastryNode node = new MultiRingPastryNode( factory.newNode(bootstrap, new RingNodeId(nodeId.copy(), null)));

    node.setMessageDispatch(new MultiRingMessageDispatch(node, node.getMessageDispatch()));
    
    if (setDone) {
      node.setBootstrap(bootstrap);
    }
    
    return node;
  }

  public PastryNode joinRing(MultiRingPastryNode parentNode, NodeHandle bootstrap) {
    MultiRingPastryNode childNode = (MultiRingPastryNode) newNode(bootstrap, parentNode.getNodeId(), false);

    parentNode.addChildPastryNode(childNode);
    childNode.setParentPastryNode(parentNode);

    childNode.setBootstrap(bootstrap);
    
    return childNode;
  }

  /**
   * This method returns the remote leafset of the provided handle
   * to the caller, in a protocol-dependent fashion.  Note that this method
   * may block while sending the message across the wire.
   *
   * @param handle The node to connect to
   * @return The leafset of the remote node
   */
  public LeafSet getLeafSet(NodeHandle handle) {
    return factory.getLeafSet(handle);
  }

  /**
   * This method returns the remote route row of the provided handle
   * to the caller, in a protocol-dependent fashion.  Note that this method
   * may block while sending the message across the wire.
   *
   * @param handle The node to connect to
   * @param row The row number to retrieve
   * @return The route row of the remote node
   */
  public RouteSet[] getRouteRow(NodeHandle handle, int row) {
    return factory.getRouteRow(handle, row);
  }

  /**
   * This method determines and returns the proximity of the current local
   * node the provided NodeHandle.  This will need to be done in a protocol-
   * dependent fashion and may need to be done in a special way.
   *
   * @param handle The handle to determine the proximity of
   * @return The proximity of the provided handle
   */
  public int getProximity(NodeHandle local, NodeHandle handle) {
    return factory.getProximity(local, handle);
  }
}

