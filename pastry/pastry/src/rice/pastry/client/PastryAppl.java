/*************************************************************************

"Free Pastry" Peer-to-Peer Application Development Substrate

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

package rice.pastry.client;

import rice.pastry.*;
import rice.pastry.messaging.*;
import rice.pastry.security.*;
import rice.pastry.standard.*;
import rice.pastry.routing.*;
import rice.pastry.leafset.*;

import java.util.*;

/**
 * A PastryAppl is an abstract class that every Pastry application
 * extends.  This is the external Pastry API.
 *
 * @version $Id$
 *
 * @author Peter Druschel
 */
public abstract class PastryAppl implements MessageReceiver
{
  // private block
  protected String instance;

  protected PastryNode thePastryNode;

  protected Address address;

  private class LeafSetObserver implements Observer {
    public void update(Observable o, Object arg) {
      NodeSetUpdate nsu = (NodeSetUpdate) arg;

      NodeHandle handle = nsu.handle();
      boolean wa = nsu.wasAdded();

      leafSetChange(handle, wa);
    }
  }

  private class RouteSetObserver implements Observer {
    public void update(Observable o, Object arg) {
      NodeSetUpdate nsu = (NodeSetUpdate) arg;

      NodeHandle handle = nsu.handle();
      boolean wa = nsu.wasAdded();

      routeSetChange(handle, wa);
    }
  }

  // constructor

  /**
   * Constructor. 
   *
   * @param pn the pastry node that client will attach to.
   */
  public PastryAppl(PastryNode pn) {
    thePastryNode = pn;

    thePastryNode.registerReceiver(getCredentials(), getAddress(), this);

    thePastryNode.addLeafSetObserver(new LeafSetObserver());
    thePastryNode.addRouteSetObserver(new RouteSetObserver());

    thePastryNode.registerApp(this); // just adds it to a list
  }
  
  /**
   * Constructor.  This constructor will perform the same tasks as the
   * above constructor, but will also create a Pastry address for this
   * application, which is dependent upon the given instance name and
   * the class name.
   *
   * @param pn the pastry node that client will attach to.
   * @param instance The instance name of this appl.
   */
  public PastryAppl(PastryNode pn, String instance) {
    this.instance = instance;
    this.address = new StandardAddress(this.getClass(), instance);

    thePastryNode = pn;
    thePastryNode.registerReceiver(getCredentials(), getAddress(), this);

    thePastryNode.addLeafSetObserver(new LeafSetObserver());
    thePastryNode.addRouteSetObserver(new RouteSetObserver());

    thePastryNode.registerApp(this); // just adds it to a list
     

  }

  /**
   * Returns the address of this application.
   *
   * @return the address.
   */
  public Address getAddress() {
    return address;
  }

  // internal methods

  /**
   * Registers a message receiver with the pastry node.  This binds the given address
   * to a message receiver.  This binding is certified by the given credentials.  Messages
   * that are delivered to this node with the given address as a destination are forwarded
   * to the supplied receiver.
   *
   * @param cred credentials which verify the binding
   * @param addr an address
   * @param mr a message receiver which will be bound the address.
   */
  public final void registerReceiver(Credentials cred, Address addr, MessageReceiver mr) {
    thePastryNode.registerReceiver(cred, addr, mr);
  }

  /**
   * Sends a message directly to the local pastry node.
   *
   * @param msg a message.
   */
  public final void sendMessage(Message msg) {
    if (Log.ifp(8)) System.out.println("[" + thePastryNode + "] send " + msg);
    thePastryNode.receiveMessage(msg);
  }

  /**
   * Called by pastry to deliver a message to this client.
   *
   * @param msg the message that is arriving.
   */
  public void receiveMessage(Message msg) {
    if (Log.ifp(8)) System.out.println("[" + thePastryNode + "] recv " + msg);
    if (msg instanceof RouteMessage) {
      RouteMessage rm = (RouteMessage) msg;

      if (enrouteMessage(rm.unwrap(), rm.getTarget(), rm.nextHop.getNodeId(), rm.getOptions()))
        rm.routeMessage(thePastryNode.getNodeId());
    }
    else messageForAppl(msg);
  }

  // useful API methods

  /**
   * Gets the node id associated with this client.
   *
   * @return the node id.
   */
  public final NodeId getNodeId() { return thePastryNode.getNodeId(); }

  /**
   * Gets the handle of the Pastry node associated with this client
   *
   * @return the node handle
   */
  public NodeHandle getNodeHandle() {
    return thePastryNode.getLocalHandle();
  }

  /**
   * Sends a message to the Pastry node identified by dest. If that
   * node has failed or no point-to-point connection can be
   * established to the node from the local node in the Internet,
   * the operation fails. Note that in this case, it may still be
   * possible to send the message to that node using routeMsg.
   *
   * @param dest the destination node
   * @param msg the message to deliver.
   * @param cred credentials that verify the authenticity of the message.
   * @param opt send options that describe how the message is to be routed.  */

  public boolean routeMsgDirect(NodeHandle dest, Message msg, Credentials cred, SendOptions opt) {
    if (Log.ifp(8)) System.out.println("[" + thePastryNode + "] routemsgdirect " + msg + " to " + dest);
    if (!dest.isAlive()) return false;
    //RouteMessage rm = new RouteMessage(dest, msg, cred, opt, getAddress());
    //thePastryNode.receiveMessage(rm);

    // XXX Does routeMsgDirect need credentials?
    // Arguably, leafset messages don't need credentials because
    // individual nodeids may be signed. (not entirely true..)
    // But routeMsgDirect messages *do* need credentials. So do we
    // go back to using options to differentiate from routeMsg?

    dest.receiveMessage(msg);
    return dest.isAlive();
  }

  /**
   * Routes a message to the live node D with nodeId numerically
   * closest to key (at the time of delivery).  The message is
   * delivered to the application with address addr at D, and at
   * each Pastry node encountered along the route to D.
   *
   * @param key the key
   * @param msg the message to deliver.
   * @param cred credentials that verify the authenticity of the message.
   * @param opt send options that describe how the message is to be routed.
   */
  public void routeMsg(Id key, Message msg, Credentials cred, SendOptions opt) {
    if (Log.ifp(8)) System.out.println("[" + thePastryNode + "] routemsg " + msg + " to " + key);
    RouteMessage rm = new RouteMessage(key, msg, cred, opt, getAddress());
    thePastryNode.receiveMessage(rm);
  }

  /**
   * Called by a layered Pastry application to obtain a copy of the leaf
   * set. The leaf set contains the nodeId to IP address binding of the
   * l/2 nodes with numerically closest counterclockwise and the l/2 nodes with
   * numerically closest clockwise nodeIds, relatively to the local node's
   * id.
   *
   * @return the local node's leaf set
   */
  public LeafSet getLeafSet() {
    return thePastryNode.getLeafSet();
  }

  /**
   * Called by a layered Pastry application to obtain a copy of the
   * routing table. The routing table contains the nodeId to IP
   * address bindings of R nodes that share the local node's id in
   * the first n digits, and differ in the n+1th digit, for 0 <= n
   * <= ceiling(log_2^b N), where N is the total number of currently
   * live nodes in the Pastry network. The routing table may be
   * incomplete, may contain nodes that cannot be reached from the
   * local node or have failed, and the table may change at any
   * time.
   */
  public RoutingTable getRoutingTable() {
    return thePastryNode.getRoutingTable();
  }

  /**
   * Called by the layered Pastry application to check if the local
   * pastry node is the one that is currently closest to the object key id.
   *
   * @param key
   * the object key id
   *
   * @return true if the local node is currently the closest to the key.
   */
  public boolean isClosest(NodeId key) {
    return thePastryNode.isClosest(key);
  }

  // abstract methods, to be overridden by the derived application object

  /**
   * Returns the credentials of this application.
   *
   * @return the credentials.
   */
  public abstract Credentials getCredentials();

  /**
   * Called by pastry when a message arrives for this application.
   *
   * @param msg the message that is arriving.
   */
  public abstract void messageForAppl(Message msg);

  /**
   * Called by pastry when a message is enroute and is passing through this node.  If this
   * method is not overridden, the default behaviour is to let the message pass through.
   *
   * @param msg the message that is passing through.
   * @param key the key
   * @param nextHop the default next hop for the message.
   * @param opt the send options the message was sent with.
   *
   * @return true if the message should be routed, false if the message should be cancelled.
   */
  public boolean enrouteMessage(Message msg, Id key, NodeId nextHop, SendOptions opt) {
    return true;
  }

  /**
   * Called by pastry when the leaf set changes.
   *
   * @param nh the handle of the node that was added or removed.
   * @param wasAdded true if the node was added, false if the node was removed.
   */
  public void leafSetChange(NodeHandle nh, boolean wasAdded) {}

  /**
   * Called by pastry when the route set changes.
   *
   * @param nh the handle of the node that was added or removed.
   * @param wasAdded true if the node was added, false if the node was removed.
   */
  public void routeSetChange(NodeHandle nh, boolean wasAdded) {}

  /**
   * Invoked when the Pastry node has joined the overlay network and
   * is ready to send and receive messages
   */
  public void notifyReady() {}

}

