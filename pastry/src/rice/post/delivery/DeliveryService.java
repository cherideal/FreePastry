package rice.post.delivery;

import java.util.*;
import java.math.*;
import java.io.*;

import rice.*;
import rice.Continuation.*;
import rice.p2p.commonapi.*;
import rice.p2p.past.*;
import rice.p2p.past.gc.*;
import rice.p2p.scribe.*;

import rice.post.*;
import rice.post.messaging.*;
import rice.post.security.*;

/**
 * This class encapsulates the logic for the delivery of notification messages
 * to users in the Post system.
 * 
 * @version $Id$
 */
public class DeliveryService implements ScribeClient {
  
  /**
   * The default timeout for delivery requests and receipts
   */
  protected long timeoutInterval;

  /**
   * The address of the user running this storage service.
   */
  protected PostImpl post;
  
  /**
   * The PAST service used for storing pending notifications
   */
  protected DeliveryPast pending;
  
  /**
   * The PAST service used for storing delivery receipts
   */
  protected Past delivered;
  
  /**
   * The Scribe used for subscribing to pending groups
   */
  protected Scribe scribe;
  
  /**
   * The factory used for creating ids
   */
  protected IdFactory factory;
  
  /**
   * A cache of recently-received delivery message ids
   */
  protected HashSet cache;
  
  /**
   * Contructs a StorageService given a PAST to run on top of.
   *
   * @param past The PAST service to use.
   * @param credentials Credentials to use to store data.
   * @param keyPair The keypair to sign/verify data with
   */
  public DeliveryService(PostImpl post, DeliveryPast pending, Past delivered, Scribe scribe, IdFactory factory, long timeoutInterval) {
    this.post = post;
    this.pending = pending;
    this.delivered = delivered;
    this.scribe = scribe;
    this.factory = factory;
    this.timeoutInterval = timeoutInterval;
    this.cache = new HashSet();
  }
  
  /**
   * Internal method which returns what the timeout should be for an
   * object inserted now.  Basically, does System.currentTimeMillis() +
   * timeoutInterval.
   *
   * @return The default timeout period for an object inserted now
   */
  protected long getTimeout() {
    return System.currentTimeMillis() + timeoutInterval;
  }
  
  /**
   * Requests delivery of the given EncryptedNotificationMessage, which internally
   * contains the destination user.  This method inserts the object into Past, and
   * the replicas will automatically pick up the message.
   *
   * @param message The message to deliver
   * @param command The command to run once finished
   */
  public void deliver(SignedPostMessage message, Continuation command) {
    post.getLogger().finer(post.getEndpoint().getId() + ": Delivering message " + message);
    
    pending.insert(new Delivery(message, factory), getTimeout(), command);
  }
  
  /** 
   * Is called when a presence message is received.  This method causes the
   * delivery service to send the first ENM to the assoicated user.
   *
   * @param message The presence message that was received
   * @param command The command to call with the message to send, if there is one
   */
  public void presence(PresenceMessage message, Continuation command) {
    post.getLogger().finer(post.getEndpoint().getId() + ": Responding to presence message " + message);
    
    pending.getMessage(message.getSender(), new StandardContinuation(command) {
      public void receiveResult(Object o) {
        parent.receiveResult((o == null ? null : ((Delivery) o).getSignedMessage()));
      }
    });
  }
  
  /**
   * Determines whether or not the given ENM has been delivered before
   *
   * @param message The message in question
   * @param command The command to run once finished
   */
  public void check(SignedPostMessage message, Continuation command) {
    post.getLogger().finer(post.getEndpoint().getId() + ": Checking for existence of message " + message);
    Id id = (new Delivery(message, factory)).getId();
    
    if (cache.contains(id)) {
      command.receiveResult(new Boolean(false));
    } else {
      delivered.lookup(id, new StandardContinuation(command) {
        public void receiveResult(Object o) {
          parent.receiveResult(new Boolean(o == null));
        }
      });
    }
  }
  
  /**
   * Records delivery of the given message to the user.  THis method inserts the receipt
   * and the replicas holding the corresponding delivery request will notice the receipt 
   * and disregard the request.
   *
   * @param message The message that was delivered
   * @param signature The signature
   * @param command The command to run once finished
   */
  public void delivered(SignedPostMessage message, byte[] signature, Continuation command) { 
    post.getLogger().finer(post.getEndpoint().getId() + ": Inserting receipt for " + message);
    final Receipt receipt = new Receipt(message, factory, signature);
    
    cache.add(receipt.getId());
    
    if (delivered instanceof GCPast) {
      ((GCPast) delivered).insert(receipt, getTimeout(), new ErrorContinuation(command) {
        public void receiveException(Exception e) {
          cache.remove(receipt.getId());
          parent.receiveException(e);
        }
      });
    } else {
      delivered.insert(receipt, new ErrorContinuation(command) {
        public void receiveException(Exception e) {
          cache.remove(receipt.getId());
          parent.receiveException(e);
        }
      });
    }
  }
  
  /**
   * Records a message as being undeliverable, which will ensure that delivery won't be attempted
   * again, but does not provide a receipt.
   *
   * @param message The message that was delivered
   * @param command The command to run once finished
   */
  public void undeliverable(SignedPostMessage message, Continuation command) { 
    post.getLogger().finer(post.getEndpoint().getId() + ": Inserting undeliverable for " + message);
    final Undeliverable receipt = new Undeliverable(message, factory);
    
    cache.add(receipt.getId());
    
    if (delivered instanceof GCPast) {
      ((GCPast) delivered).insert(receipt, getTimeout(), new ErrorContinuation(command) {
        public void receiveException(Exception e) {
          cache.remove(receipt.getId());
          parent.receiveException(e);
        }
      });
    } else {
      delivered.insert(receipt, new ErrorContinuation(command) {
        public void receiveException(Exception e) {
          cache.remove(receipt.getId());
          parent.receiveException(e);
        }
      });
    }
  }
  
  /**
   * Method which periodically checks to see if we've got receipts for
   * any outstanding messages.  If so, then we remove the outstanding message
   * from our pending list.  Also, this method makes sure we are subscribed for
   * the correct Scribe groups by looking at the messages we are responsible for.
   */
  public void synchronize() { 
    pending.synchronize(new ListenerContinuation("Synchronization of Delivery Service") {
      public void receiveResult(Object o) {
        pending.getGroups(new StandardContinuation(this) {
          public void receiveResult(Object o) {
            PostEntityAddress[] addresses = (PostEntityAddress[]) o;
            
            for (int i=0; i<addresses.length; i++) 
              scribe.subscribe(new Topic(addresses[i].getAddress()), DeliveryService.this, null);
            
            Topic[] topics = scribe.getTopics(DeliveryService.this);
            
            for (int i=0; i<topics.length; i++) {
              boolean found = false;
              
              for (int j=0; j<addresses.length && !found; j++) {
                if (addresses[j].getAddress().equals(topics[i].getId()))
                  found = true;
              }
              
              if (! found) 
                scribe.unsubscribe(topics[i], DeliveryService.this);
            }
          }
        });
      }
    });
  }
  
  /**
    * Method by which Scribe delivers a message to this client.
   *
   * @param msg The incoming message.
   */
  public void deliver(Topic topic, ScribeContent content) {
    post.deliver(topic, content);
  }
  
  /**
    * This method is invoked when an anycast is received for a topic
   * which this client is interested in.  The client should return
   * whether or not the anycast should continue.
   *
   * @param topic The topic the message was anycasted to
   * @param content The content which was anycasted
   * @return Whether or not the anycast should continue
   */
  public boolean anycast(Topic topic, ScribeContent content) {
    return post.anycast(topic, content);
  }
  
  /**
    * Informs this client that a child was added to a topic in
   * which it was interested in.
   *
   * @param topic The topic to unsubscribe from
   * @param child The child that was added
   */
  public void childAdded(Topic topic, NodeHandle child) {
    post.childAdded(topic, child);
  }
  
  /**
    * Informs this client that a child was removed from a topic in
   * which it was interested in.
   *
   * @param topic The topic to unsubscribe from
   * @param child The child that was removed
   */
  public void childRemoved(Topic topic, NodeHandle child) {
    post.childRemoved(topic, child);
  }
  
  /**
    * Informs the client that a subscribe on the given topic failed
   * - the client should retry the subscribe or take appropriate
   * action.
   *
   * @param topic The topic which the subscribe failed on
   */
  public void subscribeFailed(Topic topic) {
    post.subscribeFailed(topic);
  }
}
