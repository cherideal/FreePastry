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

package rice.p2p.scribe;

import java.util.*;
import java.util.logging.*;
import java.util.prefs.*;

import rice.*;
import rice.p2p.commonapi.*;
import rice.p2p.scribe.messaging.*;

/**
 * @(#) ScribeImpl.java Thie provided implementation of Scribe.
 *
 * @version $Id$
 * @author Alan Mislove
 */
public class ScribeImpl implements Scribe, Application {

  // the hashtable of topic -> TopicManager
  /**
   * DESCRIBE THE FIELD
   */
  public Hashtable topics;

  // this scribe's policy
  /**
   * DESCRIBE THE FIELD
   */
  protected ScribePolicy policy;

  // this application's endpoint
  /**
   * DESCRIBE THE FIELD
   */
  protected Endpoint endpoint;

  // the logger which we will use
  /**
   * DESCRIBE THE FIELD
   */
  protected Logger log = Logger.getLogger(this.getClass().getName());

  // the local node handle
  /**
   * DESCRIBE THE FIELD
   */
  protected NodeHandle handle;

  /**
   * Constructor for Scribe, using the default policy.
   *
   * @param node The node below this Scribe implementation
   * @param instance The unique instance name of this Scribe
   */
  public ScribeImpl(Node node, String instance) {
    this(node, new ScribePolicy.DefaultScribePolicy(), instance);
  }

  /**
   * Constructor for Scribe
   *
   * @param node The node below this Scribe implementation
   * @param policy The policy for this Scribe
   * @param instance The unique instance name of this Scribe
   */
  public ScribeImpl(Node node, ScribePolicy policy, String instance) {
    this.endpoint = node.registerApplication(this, instance);
    this.topics = new Hashtable();
    this.policy = policy;
    this.handle = endpoint.getLocalNodeHandle();

//   log.addHandler(new ConsoleHandler());
//   log.setLevel(Level.FINEST);
//   log.getHandlers()[0].setLevel(Level.FINEST);

    log.finer(endpoint.getId() + ": Starting up Scribe");
  }

  /**
   * Returns the Id of the local node
   *
   * @return The Id of the local node
   */
  public Id getId() {
    return endpoint.getId();
  }

  /**
   * Returns the list of children for a given topic
   *
   * @param topic The topic to return the children of
   * @return The children of the topic
   */
  public NodeHandle[] getChildren(Topic topic) {
    if (topics.get(topic) != null) {
      return ((TopicManager) topics.get(topic)).getChildren();
    }

    return new NodeHandle[0];
  }

  // ----- SCRIBE METHODS -----

  /**
   * Subscribes the given client to the provided topic. Any message published to the topic will be
   * delivered to the Client via the deliver() method.
   *
   * @param topic The topic to subscribe to
   * @param client The client to give messages to
   */
  public void subscribe(Topic topic, ScribeClient client) {
    log.finer(endpoint.getId() + ": Subscribing client " + client + " to topic " + topic);

    // if we don't know about this topic, subscribe
    // otherwise, we simply add the client to the list
    if (topics.get(topic) == null) {
      topics.put(topic, new TopicManager(topic, client));

      endpoint.route(topic.getId(), new SubscribeMessage(handle, topic), null);
    } else {
      ((TopicManager) topics.get(topic)).addClient(client);
    }
  }

  /**
   * Unsubscribes the given client from the provided topic.
   *
   * @param topic The topic to unsubscribe from
   * @param client The client to unsubscribe
   */
  public void unsubscribe(Topic topic, ScribeClient client) {
    log.finer(endpoint.getId() + ": Unsubscribing client " + client + " from topic " + topic);

    if (topics.get(topic) != null) {
      TopicManager manager = (TopicManager) topics.get(topic);

      // if this is the last client and there are no children,
      // then we unsubscribe from the topic
      if (manager.removeClient(client)) {
        topics.remove(topic);
        NodeHandle parent = manager.getParent();

        if (parent != null) {
          endpoint.route(parent.getId(), new UnsubscribeMessage(handle, topic), parent);
        }
      }
    } else {
      log.warning(endpoint.getId() + ": Attempt to unsubscribe client " + client + " from unknown topic " + topic);
    }
  }

  /**
   * Publishes the given message to the topic.
   *
   * @param topic The topic to publish to
   * @param content The content to publish
   */
  public void publish(Topic topic, ScribeContent content) {
    log.finer(endpoint.getId() + ": Publishing content " + content + " to topic " + topic);

    endpoint.route(topic.getId(), new PublishRequestMessage(handle, topic, content), null);
  }

  /**
   * Anycasts the given content to a member of the given topic
   *
   * @param topic The topic to anycast to
   * @param content The content to anycast
   */
  public void anycast(Topic topic, ScribeContent content) {
    log.finer(endpoint.getId() + ": Anycasting content " + content + " to topic " + topic);

    endpoint.route(topic.getId(), new AnycastMessage(handle, topic, content), null);
  }

  /**
   * Adds a child to the given topic
   *
   * @param topic The topic to add the child to
   * @param child The child to add
   */
  public void addChild(Topic topic, NodeHandle child) {
    log.finer(endpoint.getId() + ": Adding child " + child + " to topic " + topic);
    TopicManager manager = (TopicManager) topics.get(topic);

    // if we don't know about the topic, we subscribe, otherwise,
    // we simply add the child to the list
    if (manager == null) {
      manager = new TopicManager(topic, child);
      topics.put(topic, manager);

      log.finer(endpoint.getId() + ": Implicitly subscribing to topic " + topic);
      endpoint.route(topic.getId(), new SubscribeMessage(handle, topic), null);
    } else {
      manager.addChild(child);
    }

    // we send a confirmation back to the child
    endpoint.route(child.getId(), new SubscribeAckMessage(handle, topic), child);

    // and lastly notify all of the clients
    ScribeClient[] clients = manager.getClients();

    for (int i = 0; i < clients.length; i++) {
      clients[i].childAdded(topic, child);
    }
  }

  /**
   * Removes a child from the given topic
   *
   * @param topic The topic to remove the child from
   * @param child The child to remove
   */
  public void removeChild(Topic topic, NodeHandle child) {
    log.finer(endpoint.getId() + ": Removing child " + child + " from topic " + topic);

    if (topics.get(topic) != null) {
      TopicManager manager = (TopicManager) topics.get(topic);

      // if this is the last child and there are no clients, then
      // we unsubscribe, if we are not the root
      if (manager.removeChild(child)) {
        topics.remove(topic);
        NodeHandle parent = manager.getParent();

        if (parent != null) {
          endpoint.route(parent.getId(), new UnsubscribeMessage(handle, topic), parent);
        }
      }

      // and lastly notify all of the clients
      ScribeClient[] clients = manager.getClients();

      for (int i = 0; i < clients.length; i++) {
        clients[i].childRemoved(topic, child);
      }
    } else {
      log.warning(endpoint.getId() + ": Unexpected attempt to remove child " + child + " from unknown topic " + topic);
    }
  }


  // ----- COMMON API METHODS -----

  /**
   * This method is invoked on applications when the underlying node is about to forward the given
   * message with the provided target to the specified next hop. Applications can change the
   * contents of the message, specify a different nextHop (through re-routing), or completely
   * terminate the message.
   *
   * @param message The message being sent, containing an internal message along with a destination
   *      key and nodeHandle next hop.
   * @return Whether or not to forward the message further
   */
  public boolean forward(final RouteMessage message) {
    if (message.getMessage() instanceof AnycastMessage) {
      AnycastMessage aMessage = (AnycastMessage) message.getMessage();

      // if this is our own subscribe message, ignore it
      if (aMessage.getSource().getId().equals(endpoint.getId())) {
        return true;
      }

      // get the topic manager associated with this topic
      TopicManager manager = (TopicManager) topics.get(aMessage.getTopic());

      // if it's a subscribe message, we must handle it differently
      if (message.getMessage() instanceof SubscribeMessage) {
        SubscribeMessage sMessage = (SubscribeMessage) message.getMessage();

        ScribeClient[] clients = new ScribeClient[0];
        NodeHandle[] handles = new NodeHandle[0];

        if (manager != null) {
          clients = manager.getClients();
          handles = manager.getChildren();
        }

        // see if the policy will allow us to take on this child
        if (policy.allowSubscribe(sMessage, clients, handles)) {
          log.finer(endpoint.getId() + ": Hijacking subscribe message from " +
            sMessage.getSource() + " for topic " + sMessage.getTopic());

          // if so, add the child
          addChild(sMessage.getTopic(), sMessage.getSource());
          return false;
        }

        // otherwise, we are effectively rejecting the child
        log.finer(endpoint.getId() + ": Rejecting subscribe message from " +
          sMessage.getSource() + " for topic " + sMessage.getTopic());

        // if we are not associated with this topic at all, we simply let the subscribe go
        // closer to the root
        if (manager == null) {
          return true;
        }
      } else {
        // if we are not associated with this topic at all, let the
        // anycast continue
        if (manager == null) {
          return true;
        }

        ScribeClient[] clients = manager.getClients();

        // see if one of our clients will accept the anycast
        for (int i = 0; i < clients.length; i++) {
          if (clients[i].anycast(aMessage.getTopic(), aMessage.getContent())) {
            log.finer(endpoint.getId() + ": Accepting anycast message from " +
              aMessage.getSource() + " for topic " + aMessage.getTopic());

            return false;
          }
        }

        log.finer(endpoint.getId() + ": Rejecting anycast message from " +
          aMessage.getSource() + " for topic " + aMessage.getTopic());
      }

      // add the local node to the visited list
      aMessage.addVisited(endpoint.getLocalNodeHandle());

      // allow the policy to select the order in which the nodes are visited
      policy.directAnycast(aMessage, manager.getParent(), manager.getChildren());

      // reset the source of the message to be us
      aMessage.setSource(endpoint.getLocalNodeHandle());

      // get the next hop
      NodeHandle handle = aMessage.getNext();

      // make sure that the next node is alive
      while ((handle != null) && (!handle.isAlive())) {
        handle = aMessage.getNext();
      }

      log.finer(endpoint.getId() + ": Forwarding anycast message for topic " + aMessage.getTopic() + "on to " + handle);

      if (handle == null) {
        log.info(endpoint.getId() + ": Anycast " + aMessage + " failed.");
      } else {
        endpoint.route(handle.getId(), aMessage, handle);
      }

      return false;
    }

    return true;
  }

  /**
   * This method is called on the application at the destination node for the given id.
   *
   * @param id The destination id of the message
   * @param message The message being sent
   */
  public void deliver(Id id, Message message) {
    if (message instanceof AnycastMessage) {
      AnycastMessage aMessage = (AnycastMessage) message;

      // if we are the recipient to someone else's subscribe, then we should have processed
      // this message in the forward() method.
      // Otherwise, we received our own subscribe message, which means that we are
      // the root
      if (aMessage.getSource().getId().equals(endpoint.getId())) {
        if (aMessage instanceof SubscribeMessage) {
          log.info(endpoint.getId() + ": Received our own subscribe message for topic " +
            aMessage.getTopic() + " - we are the root.");
        } else {
          log.warning(endpoint.getId() + ": Received unexpected delivered anycast message for topic " +
            aMessage.getTopic() + " - was generated by us.");
        }
      } else {
        log.warning(endpoint.getId() + ": Received unexpected delivered anycast message for topic " +
          aMessage.getTopic() + " - not generated by us, but was expected to be.");
      }
    } else if (message instanceof SubscribeAckMessage) {
      SubscribeAckMessage saMessage = (SubscribeAckMessage) message;
      TopicManager manager = (TopicManager) topics.get(saMessage.getTopic());

      log.finer(endpoint.getId() + ": Received subscribe ack message from " +
        saMessage.getSource() + " for topic " + saMessage.getTopic());

      // if we don't know about this topic, then we unsubscribe
      // if we already have a parent, then we unsubscribe from the
      // parent in error
      if (manager != null) {
        if (manager.getParent() != null) {
          log.warning(endpoint.getId() + ": Received unexpected subscribe ack message (already have a parent) from " +
            saMessage.getSource() + " for topic " + saMessage.getTopic());
          endpoint.route(saMessage.getSource().getId(), new UnsubscribeMessage(handle, saMessage.getTopic()), saMessage.getSource());
        } else {
          manager.setParent(saMessage.getSource());
        }
      } else {
        log.warning(endpoint.getId() + ": Received unexpected subscribe ack message from " +
          saMessage.getSource() + " for unknown topic " + saMessage.getTopic());
        endpoint.route(saMessage.getSource().getId(), new UnsubscribeMessage(handle, saMessage.getTopic()), saMessage.getSource());
      }
    } else if (message instanceof PublishRequestMessage) {
      PublishRequestMessage prMessage = (PublishRequestMessage) message;
      TopicManager manager = (TopicManager) topics.get(prMessage.getTopic());

      log.finer(endpoint.getId() + ": Received publish request message with data " +
        prMessage.getContent() + " for topic " + prMessage.getTopic());

      // if message is for a non-existant topic, drop it on the floor (we are the root, after all)
      // otherwise, turn it into a publish message, and forward it on
      if (manager == null) {
        log.info(endpoint.getId() + ": Received publish request message for non-existent topic " +
          prMessage.getTopic() + " - dropping on floor.");
      } else {
        deliver(prMessage.getTopic().getId(), new PublishMessage(prMessage.getSource(), prMessage.getTopic(), prMessage.getContent()));
      }
    } else if (message instanceof PublishMessage) {
      PublishMessage pMessage = (PublishMessage) message;
      TopicManager manager = (TopicManager) topics.get(pMessage.getTopic());

      log.finer(endpoint.getId() + ": Received publish message with data " + pMessage.getContent() + " for topic " + pMessage.getTopic());

      // if we don't know about this topic, send an unsubscribe message
      // otherwise, we deliver the message to all clients and forward the
      // message to all children
      if (manager != null) {
        pMessage.setSource(handle);

        ScribeClient[] clients = manager.getClients();

        for (int i = 0; i < clients.length; i++) {
          log.finer(endpoint.getId() + ": Delivering publish message with data " + pMessage.getContent() + " for topic " +
            pMessage.getTopic() + " to client " + clients[i]);
          clients[i].deliver(pMessage.getTopic(), pMessage.getContent());
        }

        NodeHandle[] handles = manager.getChildren();

        for (int i = 0; i < handles.length; i++) {
          log.finer(endpoint.getId() + ": Forwarding publish message with data " + pMessage.getContent() + " for topic " +
            pMessage.getTopic() + " to child " + handles[i]);
          endpoint.route(handles[i].getId(), pMessage, handles[i]);
        }
      } else {
        log.warning(endpoint.getId() + ": Received unexpected publish message from " +
          pMessage.getSource() + " for unknown topic " + pMessage.getTopic());

        endpoint.route(pMessage.getSource().getId(), new UnsubscribeMessage(handle, pMessage.getTopic()), pMessage.getSource());
      }
    } else if (message instanceof UnsubscribeMessage) {
      UnsubscribeMessage uMessage = (UnsubscribeMessage) message;
      log.finer(endpoint.getId() + ": Received unsubscribe message from " +
        uMessage.getSource() + " for topic " + uMessage.getTopic());

      removeChild(uMessage.getTopic(), uMessage.getSource());
    }
  }

  /**
   * This method is invoked to inform the application that the given node has either joined or left
   * the neighbor set of the local node, as the set would be returned by the neighborSet call.
   *
   * @param handle The handle that has joined/left
   * @param joined Whether the node has joined or left
   */
  public void update(NodeHandle handle, boolean joined) {
  }

  /**
   * Class which keeps track of a given topic
   *
   * @version $Id$
   * @author amislove
   */
  public class TopicManager implements Observer {

    /**
     * DESCRIBE THE FIELD
     */
    protected Topic topic;

    /**
     * DESCRIBE THE FIELD
     */
    protected Vector clients;

    /**
     * DESCRIBE THE FIELD
     */
    protected Vector children;

    /**
     * DESCRIBE THE FIELD
     */
    protected NodeHandle parent;

    /**
     * Constructor for TopicManager.
     *
     * @param topic DESCRIBE THE PARAMETER
     * @param client DESCRIBE THE PARAMETER
     */
    public TopicManager(Topic topic, ScribeClient client) {
      this(topic);

      addClient(client);
    }

    /**
     * Constructor for TopicManager.
     *
     * @param topic DESCRIBE THE PARAMETER
     * @param child DESCRIBE THE PARAMETER
     */
    public TopicManager(Topic topic, NodeHandle child) {
      this(topic);

      addChild(child);
    }

    /**
     * Constructor for TopicManager.
     *
     * @param topic DESCRIBE THE PARAMETER
     */
    protected TopicManager(Topic topic) {
      this.topic = topic;
      this.clients = new Vector();
      this.children = new Vector();
    }

    /**
     * Gets the Parent attribute of the TopicManager object
     *
     * @return The Parent value
     */
    public NodeHandle getParent() {
      return parent;
    }

    /**
     * Gets the Clients attribute of the TopicManager object
     *
     * @return The Clients value
     */
    public ScribeClient[] getClients() {
      return (ScribeClient[]) clients.toArray(new ScribeClient[0]);
    }

    /**
     * Gets the Children attribute of the TopicManager object
     *
     * @return The Children value
     */
    public NodeHandle[] getChildren() {
      return (NodeHandle[]) children.toArray(new NodeHandle[0]);
    }

    /**
     * Sets the Parent attribute of the TopicManager object
     *
     * @param handle The new Parent value
     */
    public void setParent(NodeHandle handle) {
      if ((handle != null) && (parent != null)) {
        log.warning(endpoint.getId() + ": Unexpectedly changing parents for topic " + topic);
      }

      if (parent != null) {
        parent.deleteObserver(this);
      }

      parent = handle;

      if ((parent != null) && parent.isAlive()) {
        parent.addObserver(this);
      }
    }

    /**
     * DESCRIBE THE METHOD
     *
     * @param o DESCRIBE THE PARAMETER
     * @param arg DESCRIBE THE PARAMETER
     */
    public void update(Observable o, Object arg) {
      if (arg.equals(NodeHandle.DECLARED_DEAD)) {
        if (children.contains(o)) {
          log.fine(endpoint.getId() + ": Child " + o + " for topic " + topic + " has died - removing.");

          ScribeImpl.this.removeChild(topic, (NodeHandle) o);
        } else if (o.equals(parent)) {
          // if our parent has died, then we must resubscribe to the topic
          log.fine(endpoint.getId() + ": Parent " + parent + " for topic " + topic + " has died - resubscribing.");

          setParent(null);
          endpoint.route(topic.getId(), new SubscribeMessage(endpoint.getLocalNodeHandle(), topic), null);
        } else {
          log.warning(endpoint.getId() + ": Received unexpected update from " + o);
          o.deleteObserver(this);
        }
      }
    }

    /**
     * Adds a feature to the Client attribute of the TopicManager object
     *
     * @param client The feature to be added to the Client attribute
     */
    public void addClient(ScribeClient client) {
      if (!clients.contains(client)) {
        clients.add(client);
      }
    }

    /**
     * DESCRIBE THE METHOD
     *
     * @param client DESCRIBE THE PARAMETER
     * @return DESCRIBE THE RETURN VALUE
     */
    public boolean removeClient(ScribeClient client) {
      clients.remove(client);

      boolean unsub = ((clients.size() == 0) && (children.size() == 0));

      // if we're going to unsubscribe, then we remove ourself as
      // as observer
      if (unsub && (parent != null)) {
        parent.deleteObserver(this);
      }

      return unsub;
    }

    /**
     * Adds a feature to the Child attribute of the TopicManager object
     *
     * @param child The feature to be added to the Child attribute
     */
    public void addChild(NodeHandle child) {
      if ((!children.contains(child)) && child.isAlive()) {
        children.add(child);
        child.addObserver(this);
      }
    }

    /**
     * DESCRIBE THE METHOD
     *
     * @param child DESCRIBE THE PARAMETER
     * @return DESCRIBE THE RETURN VALUE
     */
    public boolean removeChild(NodeHandle child) {
      children.remove(child);
      child.deleteObserver(this);

      boolean unsub = ((clients.size() == 0) && (children.size() == 0));

      // if we're going to unsubscribe, then we remove ourself as
      // as observer
      if (unsub && (parent != null)) {
        parent.deleteObserver(this);
      }

      return unsub;
    }
  }
}

