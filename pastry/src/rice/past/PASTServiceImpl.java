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

package rice.past;

import rice.past.messaging.*;

import rice.*;
import rice.pastry.*;
import rice.pastry.client.*;
import rice.pastry.security.*;
import rice.pastry.messaging.*;
import rice.pastry.routing.*;
import rice.rm.*;
import rice.persistence.*;

import java.io.*;
import java.util.Hashtable;

/**
 * @(#) PASTServiceImpl.java
 *
 * Implementation of PAST, which allows users to store replicated
 * copies of documents on the Pastry network.
 *
 * @version $Id$
 * @author Charles Reis
 * @author Alan Mislove
 * @author Ansley Post
 */
public class PASTServiceImpl extends PastryAppl implements PASTService, RMClient {
  
  /**
   * Whether to print debugging statements.
   */
  public static boolean DEBUG = false;
  
  /**
   * PastryNode this service is running on.
   */
  private PastryNode pastry;
  
  /**
   * Storage used to store objects (persistedly).
   */
  private StorageManager storage;
    
  /**
   * Credentials for this application
   */
  private Credentials credentials;
  
  /**
   * SendOptions to be used on the Pastry messages.
   */
  private SendOptions sendOptions;
  
  /**
   * The table used to store commands waiting for a response.
   * Maps PASTMessageID to Continuation.
   */
  protected Hashtable commandTable;
  
  /**
   * Timeout to use while waiting for response messages, in milliseconds.
   */
  private long timeout = 5000;

  /**
   * Replication factor to use with the replication manager 
   * Should probably make this slightly more configurable
   */
  private static int REPLICATION_FACTOR = 4;
 
  /**
   * Replication Manager to use
   */
  private RM replicationManager; 
  /**
   * Builds a new PASTService to run on the given PastryNode, given a
   * Storage object (to persistedly store objects) and a cache used
   * to cache objects.
   *
   * @param pastry PastryNode to run on
   * @param storage The Storage object to use for storage and caching
   */
  public PASTServiceImpl(PastryNode pastry, StorageManager storage) {
    super(pastry);
    this.pastry = pastry;
    this.storage = storage;
    credentials = new PermissiveCredentials();
    sendOptions = new SendOptions();
    commandTable = new Hashtable();
    replicationManager = new RMImpl(pastry);
    replicationManager.register(this.getAddress(), this);
  }

  /**
   * Returns the StorageManager object
   *
   * @return This PAST's StorageManager object
   */
  public StorageManager getStorage() {
    return storage;
  }

  /**
   * Returns the PastryNode 
   *
   * @return This PAST's Pastry Node
   */
  public PastryNode getPastryNode() {
    return pastry;
  }
  
  // ---------- PastryAppl Methods ----------
  
  /**
   * Returns the address of this application.
   *
   * @return the address.
   */
  public Address getAddress() {
    return PASTAddress.instance();
  }
  
  /**
   * Returns the credentials of this application.
   *
   * @return the credentials.
   */
  public Credentials getCredentials() {
    return credentials;
  }
  
  /**
   * Called by pastry when a message arrives for this application.
   *
   * @param msg the message that is arriving.
   */
  public void messageForAppl(Message msg) {
    if (msg instanceof PASTMessage) {
      PASTMessage pmsg = (PASTMessage) msg;
      
      if (pmsg.getType() == PASTMessage.REQUEST) {
        pmsg.performAction(this);
      } else {
        _handleResponseMessage(pmsg);
      }
    } else {
      System.err.println("PAST Error: Received a non-PAST message:" + msg + " - dropping on floor.");
    }
  }
  
  /**
   * Sends a message to a remote PAST node (either a request or response).
   *
   * @param msg PASTMessage to send
   */
  public void sendMessage(PASTMessage msg) {
    if (msg.getType() == PASTMessage.REQUEST) {
      routeMsg(msg.getFileId(), msg, credentials, sendOptions);
    } else {
      routeMsg(msg.getSource(), msg, credentials, sendOptions);
    }
  }
  
  /**
   * Sends a request message and stores the given command to be
   * executed when the response is received.
   * @param msg Request to send
   * @param command Command to execute when the result is received
   */
  protected void _sendRequestMessage(PASTMessage msg, 
                                     Continuation command)
  {
    // Update the command table so that this command can collect its
    // response when it arrives.
    commandTable.put(msg.getID(), command);
    
    // Route the request to the remote node
    sendMessage(msg);
  }
  
  /**
   * Receives a response message after a request has been sent
   * and gives it to the appropriate command.
   */
  protected void _handleResponseMessage(PASTMessage msg) {
    // Look up the command waiting for this response
    Continuation command = 
      (Continuation) commandTable.get(msg.getID());
        
    if (command != null) {
      // Give response to the command
      command.receiveResult(msg);
    }
    else {
      // We don't recognize this response message, so ignore it.
    }
  }
  
  // ---------- PASTService Methods ----------
  
  /**
   * Inserts an object with the given ID into distributed storage.
   * Asynchronously returns a boolean as the result to the provided
   * InsertResultCommand, indicating whether the insert was successful.
   * 
   * @param id Pastry key identifying the object to be stored
   * @param obj Persistable object to be stored
   * @param authorCred Author's credentials
   * @param command Command to be performed when the result is received
   */
  public void insert(NodeId id, Serializable obj, Credentials authorCred,
                     final Continuation command)
  {
    NodeId nodeId = pastry.getNodeId();
    debug("Insert request for file " + id + " at node " + nodeId);
    MessageInsert request = new MessageInsert(nodeId, id, obj, authorCred);
    
    // Send the request
    _sendRequestMessage(request, new Continuation() {
      public void receiveResult(Object result) {
        if (result == null) {
          System.out.println("ERROR: Recieved null result in PAST.insert");
          
          // Not successful
          command.receiveResult(new Boolean(false));
        }
        else if (result instanceof MessageInsert) {
          // Return whether successful
          boolean success = ((MessageInsert)result).getSuccess();
          if (! success) {
            System.out.println("ERROR: Recieved bad result in PAST.insert");
          }
          
          command.receiveResult(new Boolean(success));
        }
        else {
          // Should have gotten a MessageInsert
          command.receiveException(new IllegalArgumentException("Expected a MessageInsert result, got " + result));
        }
      }
      public void receiveException(Exception result) {
        command.receiveException(result);
      }
    });
    replicationManager.replicate(getAddress(), id, obj, REPLICATION_FACTOR - 1);
  }
    
  /**
   * Retrieves the object and all associated updates with the given ID.
   * Asynchronously returns a StorageObject as the result to the provided
   * Continuation.
   * 
   * @param id Pastry key of original object
   * @param command Command to be performed when the result is received
   */
  public void lookup(NodeId id, final Continuation command) {
    NodeId nodeId = pastry.getNodeId();
    debug("Request to look up file " + id + " at node " + nodeId);
    MessageLookup request = new MessageLookup(nodeId, id);
    
    // Send the request
    _sendRequestMessage(request, new Continuation() {
      public void receiveResult(Object result) {
        if (result == null) {
          // Null result
          command.receiveResult(null);
        }
        else if (result instanceof MessageLookup) {
          // Return the content discovered
          command.receiveResult(((MessageLookup)result).getContent());
        }
        else {
          // Should have gotten a MessageLookup
          command.receiveException(new IllegalArgumentException("Expected a MessageLookup result, got " + result));
        }
      }
      public void receiveException(Exception result) {
        command.receiveException(result);
      }
    });
  }
  
  /**
   * Determines whether an object is currently stored at the given ID.
   * Asynchronously returns a boolean as the result to the provided
   * Continuation, indicating whether the object exists.
   * 
   * @param id Pastry key of original object
   * @param command Command to be performed when the result is received
   */
  public void exists(NodeId id, final Continuation command) {
    NodeId nodeId = pastry.getNodeId();
    debug("Request to determine if file " + id + " exists, at node " + nodeId);
    MessageExists request = new MessageExists(nodeId, id);
    
    // Send the request
    _sendRequestMessage(request, new Continuation() {
      public void receiveResult(Object result) {
        if (result == null) {
          // Not successful
          command.receiveResult(new Boolean(false));
        }
        else if (result instanceof MessageExists) {
          // Return whether object exists
          boolean exists = ((MessageExists)result).exists();
          command.receiveResult(new Boolean(exists));
        }
        else {
          // Should have gotten a MessageExists
          command.receiveException(new IllegalArgumentException("Expected a MessageExists result, got " + result));
        }
      }
      public void receiveException(Exception result) {
        command.receiveException(result);
      }
    });
  }
 
  /**
   * Reclaims the storage used by the object with the given ID.
   * Asynchronously returns a boolean as the result to the provided
   * Continuation, indicating whether the delete was successful.
   * 
   * @param id Pastry key of original object
   * @param authorCred Author's credentials
   */
  public void delete(NodeId id, Credentials authorCred,
                     final Continuation command) {
    NodeId nodeId = pastry.getNodeId();
    System.out.println("Deleting the file with ID: " + id);
    MessageReclaim request = 
      new MessageReclaim(pastry.getNodeId(), id, authorCred);
    
    // Send the request
    _sendRequestMessage(request, new Continuation() {
      public void receiveResult(Object result) {
        if (result == null) {
          // Not successful
          command.receiveResult(new Boolean(false));
        }
        else if (result instanceof MessageReclaim) {
          // Return whether successful
          boolean success = ((MessageReclaim)result).getSuccess();
          command.receiveResult(new Boolean(success));
        }
        else {
          // Should have gotten a MessageReclaim
          command.receiveException(new IllegalArgumentException("Expected a MessageReclaim result, got " + result));
        }
      }
      public void receiveException(Exception result) {
        command.receiveException(result);
      }
    });
    replicationManager.remove(getAddress(), id, REPLICATION_FACTOR - 1);
  }

 // ---------- RMClient Methods ----------

 /* This upcall is invoked by the Replica Manager to notify the application 
  * that it is responsible for the object with this particular objectKey. It
  * is the duty of the application to decide what action to take. For instance,
  * in PAST, the application will need to store the file in it
  * local storage unit.
  *
  * @param objectKey the object key of the object
  * @param object the object
  */
  public void responsible(NodeId objectKey, Object object){

       final Continuation insert = new Continuation(){
         public void receiveResult(Object o){}
         public void receiveException(Exception e){
               e.printStackTrace();
         }
       };
         
       getStorage().store(objectKey, (Serializable) object, insert);
  }

 /* This upcall is invoked by the Replica Manager to notify the application 
  * that it is no longer responsible for the object with this particular
  * objectKey. It is the duty of the application to decide what action to take.
  * For instance, in PAST, the application will need to delete the file from
  * its local storage unit.
  *
  * @param objectKey the object key of the object
  */
  public void notresponsible(NodeId objectKey){

      final Continuation delete = new Continuation(){
         public void receiveResult(Object o){}
         public void receiveException(Exception e){
               e.printStackTrace();
         }
       };
         
       getStorage().unstore(objectKey, delete);

  }
 
 /* This upcall is invoked by the Replica Manager to notify the application
  * that it should continue to hold the object. If it was not holding the 
  * object then the RMClient should treat this upcall as implicit notification
  * that it is responsible and so should take steps to get the object. On the 
  * other hand if the RMClient does not get this upcall for a long time(several 
  * Timeperiods after, where it is assumed that the underlying RM layer is ASKED
  * by the RMClient layer to send a  refresh in one timeperiod), then the
  * application can get rid of the object.
  */
  public void refresh(NodeId objectKey){

  }

 /* This upcall is simply to denote that the underlying replica manager
  * is ready.
  */
  public void rmIsReady(){
     System.out.println("Replica Manager is Ready");

  }

  public int getReplicationFactor(){
    return REPLICATION_FACTOR;
  }
  
 // ---------- Debug Methods ---------------   
  /**
   * Prints a debugging message to System.out if the
   * DEBUG flag is turned on.
   */
  protected void debug(String message) {
    if (DEBUG) {
      System.out.println("PASTService:  " + message);
    }
  }
}
