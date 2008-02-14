/*******************************************************************************

"FreePastry" Peer-to-Peer Application Development Substrate

Copyright 2002-2007, Rice University. Copyright 2006-2007, Max Planck Institute 
for Software Systems.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

- Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.

- Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.

- Neither the name of Rice  University (RICE), Max Planck Institute for Software 
Systems (MPI-SWS) nor the names of its contributors may be used to endorse or 
promote products derived from this software without specific prior written 
permission.

This software is provided by RICE, MPI-SWS and the contributors on an "as is" 
basis, without any representations or warranties of any kind, express or implied 
including, but not limited to, representations or warranties of 
non-infringement, merchantability or fitness for a particular purpose. In no 
event shall RICE, MPI-SWS or contributors be liable for any direct, indirect, 
incidental, special, exemplary, or consequential damages (including, but not 
limited to, procurement of substitute goods or services; loss of use, data, or 
profits; or business interruption) however caused and on any theory of 
liability, whether in contract, strict liability, or tort (including negligence
or otherwise) arising in any way out of the use of this software, even if 
advised of the possibility of such damage.

*******************************************************************************/ 
package org.mpisws.p2p.transport.rendezvous;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.mpisws.p2p.transport.ClosedChannelException;
import org.mpisws.p2p.transport.ErrorHandler;
import org.mpisws.p2p.transport.MessageCallback;
import org.mpisws.p2p.transport.MessageRequestHandle;
import org.mpisws.p2p.transport.P2PSocket;
import org.mpisws.p2p.transport.P2PSocketReceiver;
import org.mpisws.p2p.transport.SocketCallback;
import org.mpisws.p2p.transport.SocketRequestHandle;
import org.mpisws.p2p.transport.TransportLayer;
import org.mpisws.p2p.transport.TransportLayerCallback;
import org.mpisws.p2p.transport.sourceroute.Forwarder;
import org.mpisws.p2p.transport.util.InsufficientBytesException;
import org.mpisws.p2p.transport.util.MessageRequestHandleImpl;
import org.mpisws.p2p.transport.util.OptionsFactory;
import org.mpisws.p2p.transport.util.SocketInputBuffer;
import org.mpisws.p2p.transport.util.SocketRequestHandleImpl;

import rice.Continuation;
import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.environment.random.RandomSource;
import rice.p2p.commonapi.rawserialization.InputBuffer;
import rice.p2p.util.rawserialization.SimpleOutputBuffer;
import rice.p2p.util.tuples.MutableTuple;
import rice.p2p.util.tuples.Tuple;
import rice.selector.SelectorManager;
import rice.selector.TimerTask;

/**
 * The trick here is that this layer is at some level, say InetSocketAddress, but must pass around very High-Level
 * Identifiers, such as a NodeHandle for the rendezvous strategy to do its job, but maybe this can just be the RendezvousContact, and it can be casted.
 * 
 * protocol:
 * byte CONNECTOR_SOCKET
 *   HighIdentifier target = serializer.deserialize(sib);
 *   HighIdentifier opener = serializer.deserialize(sib);
 *   int uid = sib.readInt();
 * 
 * byte ACCEPTOR_SOCKET
 *   HighIdentifier target = serializer.deserialize(sib);
 *   HighIdentifier opener = serializer.deserialize(sib);
 *   int uid = sib.readInt();
 * 
 * @author Jeff Hoye
 *
 * @param <Identifier>
 */
public class RendezvousTransportLayerImpl<Identifier, HighIdentifier extends RendezvousContact> implements 
    TransportLayer<Identifier, ByteBuffer>, TransportLayerCallback<Identifier, ByteBuffer>, PilotManager<HighIdentifier>,
    RendezvousTransportLayer<HighIdentifier> {
  
  public static final byte NORMAL_SOCKET = 0; // used when normally opening a channel (bypassing rendezvous)
  public static final byte CONNECTOR_SOCKET = 1; // sent to the rendezvous server
  public static final byte ACCEPTOR_SOCKET = 2; // used when openChannel() is called
  public static final byte PILOT_SOCKET = 3; // forms a pilot connection 
  

  public static final byte CONNECTION_RESPONSE_FAILURE = 0; // forms a pilot connection 
  public static final byte CONNECTION_RESPONSE_SUCCESS = 1; // forms a pilot connection 
  
  /**
   * Value should be a HighIdentifier
   */
  public static final String OPTION_USE_PILOT = "USE_PILOT";
  
  /**
   * options.get(RENDEZVOUS_CONTACT_STRING) returns a RendezvousContact
   */
  public String RENDEZVOUS_CONTACT_STRING;  // usually: commonapi_destination_identity 
  
  protected TransportLayer<Identifier, ByteBuffer> tl;
  protected TransportLayerCallback<Identifier, ByteBuffer> callback;
  protected RendezvousGenerationStrategy<HighIdentifier> rendezvousGenerator;
  protected PilotFinder<HighIdentifier> pilotFinder;
  protected RendezvousStrategy<HighIdentifier> rendezvousStrategy;
  protected ResponseStrategy<Identifier> responseStrategy;
  protected HighIdentifier localNodeHandle;
  protected Logger logger;
  protected ContactDeserializer<Identifier, HighIdentifier> serializer;
  protected SelectorManager selectorManager;
  protected RandomSource random;
  
  public RendezvousTransportLayerImpl(
      TransportLayer<Identifier, ByteBuffer> tl, 
      String RENDEZVOUS_CONTACT_STRING, 
      HighIdentifier myRendezvousContact,
      ContactDeserializer<Identifier, HighIdentifier> deserializer,
      RendezvousGenerationStrategy<HighIdentifier> rendezvousGenerator,
      PilotFinder<HighIdentifier> pilotFinder,
      RendezvousStrategy<HighIdentifier> rendezvousStrategy, 
      ResponseStrategy<Identifier> responseStrategy,
      Environment env) {
    this.random = env.getRandomSource();
    this.selectorManager = env.getSelectorManager();
    this.tl = tl;
    this.localNodeHandle = myRendezvousContact;
    this.serializer = deserializer;
    this.RENDEZVOUS_CONTACT_STRING = RENDEZVOUS_CONTACT_STRING;
    this.rendezvousGenerator = rendezvousGenerator;
    this.pilotFinder = pilotFinder;
    this.rendezvousStrategy = rendezvousStrategy;
    this.responseStrategy = responseStrategy;
    
    this.logger = env.getLogManager().getLogger(RendezvousTransportLayerImpl.class, null);
    tl.setCallback(this);
  }
  
  public SocketRequestHandle<Identifier> openSocket(final Identifier i, final SocketCallback<Identifier> deliverSocketToMe, final Map<String, Object> options) {
    if (logger.level <= Logger.FINEST) logger.log("openSocket("+i+","+deliverSocketToMe+","+options+")");

    final SocketRequestHandle<Identifier> handle = new SocketRequestHandleImpl<Identifier>(i,options,logger);
    
    // TODO: throw proper exception if options == null, or !contains(R_C_S)
    final HighIdentifier contact = getHighIdentifier(options);

    if (contact == null || contact.canContactDirect()) {
      // write NORMAL_SOCKET and continue
      tl.openSocket(i, new SocketCallback<Identifier>(){
        public void receiveResult(SocketRequestHandle<Identifier> cancellable, P2PSocket<Identifier> sock) {
          sock.register(false, true, new ByteWriter(NORMAL_SOCKET, new Continuation<P2PSocket<Identifier>, Exception>() {
            public void receiveResult(P2PSocket<Identifier> socket) {
              deliverSocketToMe.receiveResult(handle, socket);              
            }          
            
            public void receiveException(Exception exception) {
              deliverSocketToMe.receiveException(handle, exception);
            }
          }));
        }
        
        public void receiveException(SocketRequestHandle<Identifier> s, Exception ex) {
          deliverSocketToMe.receiveException(handle, ex);
        }
      }, options);
      return handle;
    } else {
      if (options.containsKey(OPTION_USE_PILOT)) {
        HighIdentifier middleMan = (HighIdentifier)options.get(OPTION_USE_PILOT);
        // this is normally used when a node is joining, wo you can't route to
        logger.log("Opening socket to "+contact+" OPTION_USE_PILOT->"+middleMan);        
        if (logger.level <= Logger.FINER) logger.log("Opening socket to "+contact+" OPTION_USE_PILOT->"+middleMan);        
        openSocketViaPilot(contact, middleMan, handle, deliverSocketToMe, options);
        return handle;
      } else {
        if (localNodeHandle.canContactDirect()) {
          // see if the node already has a pilot to me
          if (openSocketUsingPilotToMe(contact, handle, deliverSocketToMe, options)) return handle;
          // see if the node should have a pilot to a node I know
          if (openSocketUsingPilotFinder(contact, handle, deliverSocketToMe, options)) return handle;
          openSocketUsingRouting(contact, handle, deliverSocketToMe, options);
          return handle;          
        } else {
          if (openSocketUsingPilotFinder(contact, handle, deliverSocketToMe, options)) return handle;
          
          // pick random outgoing pilot, and openConnection via him
          ArrayList<HighIdentifier> myPilots = new ArrayList<HighIdentifier>(outgoingPilots.keySet());
          if (myPilots.isEmpty()) deliverSocketToMe.receiveException(handle, new IllegalStateException("No available outgoing pilots."));
          HighIdentifier middleMan = myPilots.get(random.nextInt(myPilots.size()));
          openSocketViaPilot(contact, middleMan, handle, deliverSocketToMe, options);
          return handle;
        }
      }
    }
  }
  
  private void openSocketUsingRouting(final HighIdentifier contact,
      final SocketRequestHandle<Identifier> handle,
      final SocketCallback<Identifier> deliverSocketToMe, 
      final Map<String, Object> options) {
//  if (true) throw new RuntimeException("Not Implemented.");
    // route to the node to open the socket to me
    final int uid = random.nextInt();
    rendezvousStrategy.openChannel(contact, localNodeHandle, localNodeHandle, uid, new Continuation<Integer, Exception>() {
    
      public void receiveResult(Integer result) {
        // don't need to do anything
      }          
      
      public void receiveException(Exception exception) {
        logger.logException("openSocket("+contact+","+deliverSocketToMe+","+options+")", exception);
        deliverSocketToMe.receiveException(handle, exception);
      }          
    }, options);
  }

  private boolean openSocketUsingPilotFinder(HighIdentifier contact,
      SocketRequestHandle<Identifier> handle,
      SocketCallback<Identifier> deliverSocketToMe, Map<String, Object> options) {
    HighIdentifier middleMan = pilotFinder.findPilot(contact);          
    if (middleMan == null) {
      return false;
    } else {
      // use middleman
      if (logger.level <= Logger.FINER) logger.log("opening a socket to "+contact+" via "+middleMan); 
      openSocketViaPilot(contact, middleMan, handle, deliverSocketToMe, options);
      return true;
    }
  }

  /**
   * Return true there was a pilot to me.
   * 
   * @param contact
   * @param handle
   * @param deliverSocketToMe
   * @return
   */
  protected boolean openSocketUsingPilotToMe(HighIdentifier contact,
      SocketRequestHandle<Identifier> handle,
      SocketCallback<Identifier> deliverSocketToMe, Map<String, Object> options) {
    int uid = random.nextInt();
    putExpectedIncomingSocket(contact, uid, deliverSocketToMe, handle);

    if (incomingPilots.containsKey(contact)) {
      // use the pilot if possible
      logger.log("Opening socket to firewalled node that I have a pilot to: "+contact+" uid:"+uid);
      try {
        incomingPilots.get(contact).requestSocket(localNodeHandle, uid);
      } catch (IOException ioe) {
        removeExpectedIncomingSocket(contact, uid);
        deliverSocketToMe.receiveException(handle, ioe);
      }
      return true;
    }
    return false;
  }

  protected void openSocketViaPilot(
      final HighIdentifier dest, 
      final HighIdentifier middleMan, 
      final SocketRequestHandle<Identifier> handle, 
      final SocketCallback<Identifier> deliverSocketToMe, 
      final Map<String, Object> options) {
    
    if (middleMan.equals(localNodeHandle)) {
      throw new IllegalArgumentException("openSocketViaPilot("+dest+","+middleMan+","+handle+","+deliverSocketToMe+","+options+") can't use self as rendezvous.");
    }
    
    final int uid = random.nextInt();
    if (logger.level <= Logger.FINE) logger.log("openSocketViaPilot<"+uid+">("+dest+","+middleMan+","+handle+","+deliverSocketToMe+","+options+")");

    // build header
    SimpleOutputBuffer sob = new SimpleOutputBuffer();
    try {
      sob.writeByte(CONNECTOR_SOCKET);
      serializer.serialize(dest, sob);
      serializer.serialize(localNodeHandle, sob);
      sob.writeInt(uid);
    } catch (IOException ioe) {
      deliverSocketToMe.receiveException(handle, ioe);
    }

    final ByteBuffer writeBuffer = sob.getByteBuffer(); // to write all the connection info
    final ByteBuffer readBuffer = ByteBuffer.allocate(1);  // to read success
    
    // open the socket
    tl.openSocket(serializer.convert(middleMan), new SocketCallback<Identifier>() {
      public void receiveResult(SocketRequestHandle<Identifier> cancellable,
          P2PSocket<Identifier> sock) {

        try {
          new P2PSocketReceiver<Identifier>() {
  
            public void receiveSelectResult(P2PSocket<Identifier> socket,
                boolean canRead, boolean canWrite) throws IOException {
              if (writeBuffer.hasRemaining()) {
                // write the header
                long bytesWritten = socket.write(writeBuffer); 
                if (bytesWritten < 0) {
                  deliverSocketToMe.receiveException(handle, new ClosedChannelException("Channel closed detected to <"+uid+"> "+dest+" via "+middleMan+" in "+RendezvousTransportLayerImpl.this));
                  return;
                }
                if (writeBuffer.hasRemaining()) {
                  socket.register(false, true, this);
                  return;
                }
              }
              if (!writeBuffer.hasRemaining()) {
                // read for the response
                if (readBuffer.hasRemaining()) {
                  long bytesRead = socket.read(readBuffer);                  
                  if (bytesRead < 0) {
                    deliverSocketToMe.receiveException(handle, new ClosedChannelException("Channel closed detected to <"+uid+"> "+dest+" via "+middleMan+" in "+RendezvousTransportLayerImpl.this));
                    return;
                  }
                  if (readBuffer.hasRemaining()) {
                    socket.register(true, false, this);
                    return;
                  }
                }
                
                // interpret the response
                readBuffer.flip();
                byte response = readBuffer.get();
                switch(response) {
                case CONNECTION_RESPONSE_SUCCESS:
                  if (logger.level <= Logger.FINE) logger.log("success in openSocketViaPilot<"+uid+">("+dest+","+middleMan+","+handle+","+deliverSocketToMe+","+options+")");
                  deliverSocketToMe.receiveResult(handle, socket);                    
                  return;
                default:
                  deliverSocketToMe.receiveException(handle, new ClosedChannelException("Failed to connect to <"+uid+"> "+dest+" via "+middleMan+" in "+RendezvousTransportLayerImpl.this+" response:"+response));
                  return;  
                }
              }              
            }
          
            public void receiveException(P2PSocket<Identifier> socket,
                Exception ioe) {
              deliverSocketToMe.receiveException(handle, ioe);
            }
          }.receiveSelectResult(sock, false, true);
        } catch (IOException ioe) {
          deliverSocketToMe.receiveException(handle, ioe);
        }
      }
      public void receiveException(SocketRequestHandle<Identifier> s,
          Exception ex) {
        deliverSocketToMe.receiveException(handle, ex);
      }
    }, options);
  }
  
  protected void routeForSocket() {
    throw new RuntimeException("Not implemented.");    
  }
  
  protected HighIdentifier getHighIdentifier(Map<String, Object> options) {
    if (options == null) return null;
    return (HighIdentifier)options.get(RENDEZVOUS_CONTACT_STRING);
  }

  public void incomingSocket(P2PSocket<Identifier> s) throws IOException {
//    logger.log("incomingSocket("+s+")");
    if (logger.level <= Logger.FINEST) logger.log("incomingSocket("+s+")");

    new P2PSocketReceiver<Identifier>() {

      public void receiveSelectResult(P2PSocket<Identifier> socket, boolean canRead, boolean canWrite) throws IOException {
        if (logger.level <= Logger.FINEST) logger.log("incomingSocket("+socket+").rSR("+canRead+","+canWrite+")");
        // read byte, switch on it
        ByteBuffer buf = ByteBuffer.allocate(1);
        long bytesRead = socket.read(buf);
        
        if (bytesRead == 0) {
          // try again
          socket.register(true, false, this);
          return;
        }
        
        if (bytesRead < 0) {
          // input was closed
          socket.close();
          return;
        }
        
        // could check that bytesRead == 1, but we know it is
        buf.flip();
        byte socketType = buf.get();
        switch(socketType) {
        case NORMAL_SOCKET:          
          if (logger.level <= Logger.FINEST) logger.log("incomingSocket("+socket+").rSR("+canRead+","+canWrite+"):NORMAL");          
          callback.incomingSocket(socket);
          return;
        case CONNECTOR_SOCKET:
          readConnectHeader(socket);
          return;
        case ACCEPTOR_SOCKET:
          readAcceptHeader(socket);
          return;
        case PILOT_SOCKET:
          new IncomingPilot(socket);
          return;
        }
      }
      
      public void receiveException(P2PSocket<Identifier> socket, Exception ioe) {
        // TODO Auto-generated method stub
        
      }
    }.receiveSelectResult(s, true, false);
  }

  protected void readConnectHeader(P2PSocket<Identifier> socket) throws IOException {
    if (logger.level <= Logger.FINEST) logger.log("readConnectHeader("+socket+")");
    final SocketInputBuffer sib = new SocketInputBuffer(socket,1024);                   
    P2PSocketReceiver<Identifier> receiver = new P2PSocketReceiver<Identifier>() {

      public void receiveSelectResult(P2PSocket<Identifier> socket,
          boolean canRead, boolean canWrite) throws IOException {
        // TODO: read the requested target, etc, and route to it to establish a connection, which will respond as an ACCEPTOR
        // TODO: make this recover from errors when sib doesn't have enough data, needs to reset(), reregister to read, probably should just do this in its own class
        
        try {
          HighIdentifier target = serializer.deserialize(sib);
          HighIdentifier opener = serializer.deserialize(sib);
          int uid = sib.readInt();
          
          if (logger.level <= Logger.FINEST) logger.log("readConnectHeader("+socket+","+target+","+opener+","+uid+")");
          // TODO: make a timeout for this structure...
          putConnectSocket(opener, target, uid, socket);
          
          if (incomingPilots.containsKey(target)) {
            if (logger.level <= Logger.FINER) logger.log("I'm the rendezevous for "+opener+" to "+target+" and I have a pilot.");            
            // TODO: send connect-request down pilot,, including uid, wait for incoming socket, then send SUCCESS down this socket
            IncomingPilot pilot = incomingPilots.get(target);
            pilot.requestSocket(opener,uid);
          } else {          
//            logger.log("I'm the rendezevous for "+opener+" to "+target+" and I don't have a pilot.");            
            if (logger.level <= Logger.INFO) logger.log("I'm the rendezevous for "+opener+" to "+target+" and I don't have a pilot.");            
            rendezvousStrategy.openChannel(target, localNodeHandle, opener, uid, null, socket.getOptions());
          }
        } catch (InsufficientBytesException ibe) {
          sib.reset();
          socket.register(true, false, this);
        }
      }
    
      public void receiveException(P2PSocket<Identifier> socket, Exception ioe) {
        // what to do here?  close the socket?
        if (logger.level <= Logger.WARNING) logger.logException("error in readConnectHeader("+socket+") closing.",ioe);
        socket.close();
      }
    };
    
    receiver.receiveSelectResult(socket, true, false);
  }
  
  protected void readAcceptHeader(P2PSocket<Identifier> acceptorSocket) throws IOException {
    if (logger.level <= Logger.FINEST) logger.log("readAcceptHeader("+acceptorSocket+")");
    final SocketInputBuffer sib = new SocketInputBuffer(acceptorSocket,1024);                   
    P2PSocketReceiver<Identifier> receiver = new P2PSocketReceiver<Identifier>() {

      public void receiveSelectResult(final P2PSocket<Identifier> acceptorSocket,
          boolean canRead, boolean canWrite) throws IOException {
        // TODO: read the requested target, etc, and route to it to establish a connection, which will respond as an ACCEPTOR
        // TODO: make this recover from errors when sib doesn't have enough data, needs to reset(), reregister to read, probably should just do this in its own class
        
        try {
          final HighIdentifier target = serializer.deserialize(sib);
          final HighIdentifier opener = serializer.deserialize(sib);
          final int uid = sib.readInt();

          if (opener.equals(localNodeHandle)) {
            // I requested this, look up in expectedIncomingSockets
            final Tuple<SocketCallback<Identifier>, SocketRequestHandle<Identifier>> deliverSocketToMe = removeExpectedIncomingSocket(target, uid);
            if (deliverSocketToMe == null) {
              if (logger.level <= Logger.WARNING) logger.log("Got accept socket to me, that I'm not expecting: t:"+target+" o:"+opener+" uid:"+uid+" "+acceptorSocket);
              new ByteWriter(CONNECTION_RESPONSE_FAILURE,new Continuation<P2PSocket<Identifier>, Exception>(){

                public void receiveException(Exception exception) {
                  // just ignore this, it's not important
//                  if (logger.level <= Logger.WARNING) logger.logException(message)
                }

                public void receiveResult(P2PSocket<Identifier> result) {
                  result.close();
                }}).receiveSelectResult(acceptorSocket, false, true);
              // send failure
              return;
            }
            // send success, then deliverSocketToMe.receiveResult();

            new ByteWriter(CONNECTION_RESPONSE_SUCCESS,new Continuation<P2PSocket<Identifier>, Exception>() {
              public void receiveException(Exception exception) {
                deliverSocketToMe.a().receiveException(deliverSocketToMe.b(), exception);
              }

              public void receiveResult(P2PSocket<Identifier> result) {
                deliverSocketToMe.a().receiveResult(deliverSocketToMe.b(), result);
              }}).receiveSelectResult(acceptorSocket, false, true);
          }
          
          if (logger.level <= Logger.FINEST) logger.log("readAcceptHeader("+acceptorSocket+","+target+","+opener+","+uid+")");

          // TODO: make a timeout for this structure...
          final P2PSocket<Identifier> connectorSocket = removeConnectSocket(opener, target, uid);
          
          if (connectorSocket == null) {
            if (logger.level <= Logger.FINE) logger.log("writing failed bytes in readAcceptHeader("+acceptorSocket+","+target+","+opener+","+uid+")");
            // write failed to socket
            P2PSocketReceiver<Identifier> acceptorFailed = new ByteWriter(CONNECTION_RESPONSE_FAILURE, 
                new Continuation<P2PSocket<Identifier>, Exception>() {
                  public void receiveResult(P2PSocket<Identifier> result) {
                    // send the failure, then close
                    result.close();
                  }              
                  public void receiveException(Exception exception) {
                    if (logger.level <= logger.WARNING) logger.logException("Error writing failed bytes in readAcceptHeader("+acceptorSocket+","+target+","+opener+","+uid+")", exception);
                    acceptorSocket.close();
                  }
                });
            acceptorFailed.receiveSelectResult(acceptorSocket, false, true);
          } else {          
            // when both sockets set themselves in this structure, then we can begin forwarding.
            final MutableTuple<P2PSocket<Identifier>, P2PSocket<Identifier>> forwardSockets = new MutableTuple<P2PSocket<Identifier>, P2PSocket<Identifier>>();
            
            if (logger.level <= Logger.FINEST) logger.log("writing success bytes in readAcceptHeader("+acceptorSocket+","+target+","+opener+","+uid+")");
            // write success to connectorSocket/acceptorSocket
            // when both writes succeed, bridge them
            P2PSocketReceiver<Identifier> connectorSuccess = new ByteWriter(CONNECTION_RESPONSE_SUCCESS, 
                new Continuation<P2PSocket<Identifier>, Exception>(){
              public void receiveResult(P2PSocket<Identifier> result) {
                // done, set up the forwarder
                if (logger.level <= Logger.FINEST) logger.log("Connector socket complete, setting up forwarding. readAcceptHeader("+acceptorSocket+","+target+","+opener+","+uid+")");
                
                forwardSockets.setA(result);
                if (forwardSockets.b() != null) createForwarder(forwardSockets.a(),forwardSockets.b(),opener,target,uid);
              }
              public void receiveException(Exception exception) {
                if (logger.level <= logger.WARNING) logger.logException("Error writing failed bytes in readAcceptHeader("+acceptorSocket+","+target+","+opener+","+uid+")", exception);
                // the connector is automatically closed
                acceptorSocket.close();
              }    
            });
            connectorSuccess.receiveSelectResult(connectorSocket, false, true);
            
            P2PSocketReceiver<Identifier> acceptorSuccess = new ByteWriter(CONNECTION_RESPONSE_SUCCESS, 
                new Continuation<P2PSocket<Identifier>, Exception>(){
              public void receiveResult(P2PSocket<Identifier> result) {
                // done, set up the forwarder
                if (logger.level <= Logger.FINEST) logger.log("Acceptor socket complete, setting up forwarding. readAcceptHeader("+acceptorSocket+","+target+","+opener+","+uid+")");                
                
                forwardSockets.setB(result);
                if (forwardSockets.a() != null) createForwarder(forwardSockets.a(), forwardSockets.b(),opener,target,uid);
              }
              public void receiveException(Exception exception) {
                if (logger.level <= logger.WARNING) logger.logException("Error writing failed bytes in readAcceptHeader("+acceptorSocket+","+target+","+opener+","+uid+")", exception);
                // the connector is automatically closed
                connectorSocket.close();
              }    
            });
            acceptorSuccess.receiveSelectResult(acceptorSocket, false, true);
                        
          }
        } catch (InsufficientBytesException ibe) {
          sib.reset();
          acceptorSocket.register(true, false, this);
        }
      }
    
      public void receiveException(P2PSocket<Identifier> socket, Exception ioe) {
        // what to do here?  close the socket?
        if (logger.level <= Logger.WARNING) logger.logException("error in readConnectHeader("+socket+") closing.",ioe);
        socket.close();
      }
    };
    
    receiver.receiveSelectResult(acceptorSocket, true, false);
  }

  Map<HighIdentifier, Map<Integer, Tuple<SocketCallback<Identifier>, SocketRequestHandle<Identifier>>>> expectedIncomingSockets = 
    new HashMap<HighIdentifier, Map<Integer, Tuple<SocketCallback<Identifier>, SocketRequestHandle<Identifier>>>>();
  
  protected void putExpectedIncomingSocket(HighIdentifier contact, int uid,
      SocketCallback<Identifier> deliverSocketToMe, SocketRequestHandle<Identifier> requestHandle) {
    Map<Integer, Tuple<SocketCallback<Identifier>, SocketRequestHandle<Identifier>>> one = expectedIncomingSockets.get(contact);
    if (one == null) {
      one = new HashMap<Integer, Tuple<SocketCallback<Identifier>, SocketRequestHandle<Identifier>>>();
      expectedIncomingSockets.put(contact, one);
    }
    
    if (one.containsKey(uid)) {
      throw new IllegalStateException("putExpectedIncomingSockets("+contact+","+uid+","+deliverSocketToMe+") already contains "+one.get(uid));
    }
    
    one.put(uid, new Tuple<SocketCallback<Identifier>, SocketRequestHandle<Identifier>>(deliverSocketToMe, requestHandle));
  }

  protected Tuple<SocketCallback<Identifier>, SocketRequestHandle<Identifier>> removeExpectedIncomingSocket(HighIdentifier target, int uid) {
    Map<Integer, Tuple<SocketCallback<Identifier>, SocketRequestHandle<Identifier>>> one = expectedIncomingSockets.get(target);
    if (one == null) {
      return null;
    }    
    Tuple<SocketCallback<Identifier>, SocketRequestHandle<Identifier>> ret = one.get(uid);
    
    if (ret != null) one.remove(uid);
    if (one.isEmpty()) expectedIncomingSockets.remove(target);
    
    return ret;
  }
  
  

  protected void createForwarder(P2PSocket<Identifier> a, P2PSocket<Identifier> b, HighIdentifier connector, HighIdentifier acceptor, int uid) {
    if (logger.level <= Logger.FINE) logger.log("createForwarder("+a+","+b+","+connector+","+acceptor+","+uid+")");
    new Forwarder(null,a,b,logger);
  }
  
  /**
   * requestor, target, uid -> socket
   */ 
  Map<HighIdentifier, Map<HighIdentifier, Map<Integer, P2PSocket<Identifier>>>> connectSockets = 
    new HashMap<HighIdentifier, Map<HighIdentifier, Map<Integer, P2PSocket<Identifier>>>>();
  
  /**
   * This map stores the connect socket until the corresponding accept socket arrives
   * 
   * @param socket
   * @param requestor
   * @param target
   * @param uid
   */
  public void putConnectSocket(HighIdentifier requestor, HighIdentifier target, int uid, P2PSocket<Identifier> socket) {        
    Map<HighIdentifier, Map<Integer, P2PSocket<Identifier>>> one = connectSockets.get(requestor);
    if (one == null) {
      one = new HashMap<HighIdentifier, Map<Integer, P2PSocket<Identifier>>>();
      connectSockets.put(requestor, one);
    }
    
    Map<Integer, P2PSocket<Identifier>> two = one.get(target);
    if (two == null) {
      two = new HashMap<Integer, P2PSocket<Identifier>>();
      one.put(target, two);
    }
    
    P2PSocket<Identifier> three = two.get(uid);
    if (three != null) {
      // error, we have a problem, because there is already a socket registered here with the same uid!!!
      if (logger.level <= Logger.WARNING) logger.log("error in storeConnectSocket() there is already a connector with the same UID!!!, dropping the new one.  Old:"+three+" new:"+socket);
      socket.close();
      return;
    }
    
    two.put(uid, socket);
        
    // TODO: make a timeout to clear up this structure
  }
  
  public P2PSocket<Identifier> removeConnectSocket(HighIdentifier requestor, HighIdentifier target, int uid) {
    Map<HighIdentifier, Map<Integer, P2PSocket<Identifier>>> one = connectSockets.get(requestor);
    if (one == null) {
      return null;
    }
    
    Map<Integer, P2PSocket<Identifier>> two = one.get(target);
    if (two == null) {
      return null;
    }
    
    // clean this up
    P2PSocket<Identifier> three = two.remove(uid);
    if (two.isEmpty()) one.remove(target);
    if (one.isEmpty()) connectSockets.remove(requestor);
    
    return three; 
  }
  
  public void openChannel(HighIdentifier requestor, HighIdentifier middleMan, int uid) {
    logger.log("openChannel("+requestor+","+middleMan+","+uid+")");
//    if (logger.level <= Logger.INFO) logger.log("openChannel("+requestor+","+middleMan+","+uid+")");
    openAcceptSocket(requestor, middleMan, uid);
  }
  
  /**
   * We are a firewalled node and got a connect request, now time to respond to it
   * 
   * @param requestor
   * @param i
   * @param sib
   */
  protected void openAcceptSocket(final HighIdentifier requestor, final HighIdentifier middleMan, final int uid) {
    if (logger.level <= Logger.FINER) logger.log("openAcceptSocket("+requestor+","+middleMan+","+uid+")");
    // TODO: there is a case where the requestor can be contacted directly, in this case, just do that, but may have to chage
    // some other parts of the code:
      // 1) Send message to middleman, rather than open socket
      // 2) Change Pilot Request to include the middleman and requestor, or, better yet, make PILOT_CONNECT_DIRECT
      // 3) Set accept the socket directly
//    if (requestor.canContactDirect() && requestor != middleMan) {
//      
//    }
    
    if (!middleMan.canContactDirect()) {
      throw new IllegalArgumentException("openAcceptSocket("+requestor+","+middleMan+","+uid+") middleMan is firewalled.");      
    }

    
    // build header
    SimpleOutputBuffer sob = new SimpleOutputBuffer();
    try {
      sob.writeByte(ACCEPTOR_SOCKET);
      serializer.serialize(localNodeHandle, sob);
      serializer.serialize(requestor, sob);
      sob.writeInt(uid);
    } catch (IOException ioe) {
      if (logger.level <= Logger.WARNING) logger.logException("Error serializing in openAcceptSocket("+requestor+","+middleMan+","+uid+")",ioe);      
      return;
    }

    final ByteBuffer writeBuffer = sob.getByteBuffer(); // to write all the connection info
    final ByteBuffer readBuffer = ByteBuffer.allocate(1);  // to read success
    

    tl.openSocket(serializer.convert(middleMan), new SocketCallback<Identifier>() {
      public void receiveResult(SocketRequestHandle<Identifier> cancellable,
          P2PSocket<Identifier> sock) {
        try {
          new P2PSocketReceiver<Identifier>() {
  
            public void receiveSelectResult(P2PSocket<Identifier> socket,
                boolean canRead, boolean canWrite) throws IOException {
              if (writeBuffer.hasRemaining()) {
                // write the header
                long bytesWritten = socket.write(writeBuffer); 
                if (bytesWritten < 0) {
                  if (logger.level <= Logger.WARNING) logger.log("Channel closed in openAcceptSocket("+requestor+","+middleMan+","+uid+")");
                  return;
                }
                if (writeBuffer.hasRemaining()) {
                  socket.register(false, true, this);
                  return;
                }
              }
              if (!writeBuffer.hasRemaining()) {
                // read for the response
                if (readBuffer.hasRemaining()) {
                  long bytesRead = socket.read(readBuffer);                  
                  if (bytesRead < 0) {
                    if (logger.level <= Logger.WARNING) logger.log("Channel closed in openAcceptSocket("+requestor+","+middleMan+","+uid+")");
                    return;
                  }
                  if (readBuffer.hasRemaining()) {
                    socket.register(true, false, this);
                    return;
                  }
                }
                
                // interpret the response
                readBuffer.flip();
                byte response = readBuffer.get();
                switch(response) {
                case CONNECTION_RESPONSE_SUCCESS:
                  if (logger.level <= Logger.FINER) logger.log("success in openAcceptSocket("+requestor+","+middleMan+","+uid+")");
                  callback.incomingSocket(socket);
                  return;
                default:
                  if (logger.level <= Logger.WARNING) logger.log("Failed to connect in openAcceptSocket("+requestor+","+middleMan+","+uid+")");
                  return;  
                }
              }
            }        
            
            public void receiveException(P2PSocket<Identifier> s,
                Exception ex) {
              if (logger.level <= Logger.WARNING) logger.logException("Failure opening socket in openAcceptSocket("+requestor+","+middleMan+","+uid+")", ex);
            }
          }.receiveSelectResult(sock, false, true);
        } catch (IOException ioe) {
          if (logger.level <= Logger.WARNING) logger.logException("Exception in openAcceptSocket("+requestor+","+middleMan+","+uid+")",ioe);                
        }
      }
    
      public void receiveException(SocketRequestHandle<Identifier> s,
          Exception ex) {
        if (logger.level <= Logger.WARNING) logger.logException("Failure opening socket in openAcceptSocket("+requestor+","+middleMan+","+uid+")", ex);
      }
    }, null);
    
  }

  
  /**
   * What to do if firewalled?
   *   ConnectRequest UDP only?  For now always use UDP_AND_TCP
   */
  public MessageRequestHandle<Identifier, ByteBuffer> sendMessage(Identifier i, ByteBuffer m, final MessageCallback<Identifier, ByteBuffer> deliverAckToMe, Map<String, Object> options) {
    if (logger.level <= Logger.FINEST) logger.log("sendMessage("+i+","+m+","+deliverAckToMe+","+options+")");

    HighIdentifier high = getHighIdentifier(options);
//    logger.log("sendMessage("+i+","+m+","+deliverAckToMe+","+options+"):"+high);
    if (high == null || high.canContactDirect() || responseStrategy.sendDirect(i, m, options)) {
      // pass-through, need to allow for null during bootstrap, we assume pass-through works
      responseStrategy.messageSent(i, m, options);
      return tl.sendMessage(i, m, deliverAckToMe, options);
    } else {
      // rendezvous
      final MessageRequestHandleImpl<Identifier, ByteBuffer> ret = new MessageRequestHandleImpl<Identifier, ByteBuffer>(i, m, options);
      MessageCallback<HighIdentifier, ByteBuffer> ack;
      if (deliverAckToMe == null) {
        ack = null;
      } else {
        ack = new MessageCallback<HighIdentifier, ByteBuffer>(){
          public void ack(MessageRequestHandle<HighIdentifier, ByteBuffer> msg) {
            deliverAckToMe.ack(ret);
          }
          public void sendFailed(MessageRequestHandle<HighIdentifier, ByteBuffer> msg, Exception reason) {
            deliverAckToMe.sendFailed(ret, reason);
          }
        };
      }
      ret.setSubCancellable(rendezvousStrategy.sendMessage(high, m, ack, options));
      return ret;
    }
  }
  
  public String toString() {
    return "RendezvousTL{"+localNodeHandle+"}";
  }

  public static final String FROM_OVERLAY = "rendezvous.from_overlay";
  
  /**
   * Usually called from the higher level app, who probably used routing to get the message here.
   * 
   * @param i
   * @param m
   * @param options
   * @throws IOException
   */
  public void messageReceivedFromOverlay(HighIdentifier i, ByteBuffer m, Map<String, Object> options) throws IOException {
    if (logger.level <= Logger.FINER) logger.log("messageReceivedFromOverlay("+i+","+m+","+options+")");
    messageReceived(serializer.convert(i),m,OptionsFactory.addOption(options, FROM_OVERLAY, true));
  }
  
  public void messageReceived(Identifier i, ByteBuffer m, Map<String, Object> options) throws IOException {
    if (logger.level <= Logger.FINE) logger.log("messageReceived("+i+","+m+","+options+")");
    if (options.containsKey(FROM_OVERLAY) && ((Boolean)options.get(FROM_OVERLAY)) == true) {
      // do nothing
    } else {
      responseStrategy.messageReceived(i, m, options);
    }
    callback.messageReceived(i, m, options);
  }
  
  public void acceptMessages(boolean b) {
    tl.acceptMessages(b);
  }
  public void acceptSockets(boolean b) {
    tl.acceptSockets(b);
  }
  public Identifier getLocalIdentifier() {
    return tl.getLocalIdentifier();
  }
  public void setCallback(TransportLayerCallback<Identifier, ByteBuffer> callback) {
    this.callback = callback;
  }
  public void setErrorHandler(ErrorHandler<Identifier> handler) {
    // TODO Auto-generated method stub    
  }
  public void destroy() {
    tl.destroy();
  }

  // *************Pilot Sockets (used to connect leafset members) ******************
  // *************** outgoing Pilots, only used by NATted nodes ********************
  Map<HighIdentifier, OutgoingPilot> outgoingPilots = 
    new HashMap<HighIdentifier, OutgoingPilot>();
  
  /**
   * Only used by NATted node.
   * 
   * Opens a pilot socket to a "lifeline" node.  These are usually nodes near the local node in the id space. 
   */
  public SocketRequestHandle<HighIdentifier> openPilot(final HighIdentifier i, 
      final Continuation<SocketRequestHandle<HighIdentifier>, Exception> deliverAckToMe) {    
    logger.log("openPilot("+i+")");
    if (logger.level <= Logger.FINE) logger.log("openPilot("+i+")");
    if (outgoingPilots.containsKey(i)) {
      return outgoingPilots.get(i); 
    }

    Map<String, Object> options = serializer.getOptions(i);
    final OutgoingPilot o = new OutgoingPilot(i,options);
    outgoingPilots.put(i, o);
    
    o.setCancellable(tl.openSocket(serializer.convert(i), new SocketCallback<Identifier>(){
      public void receiveResult(SocketRequestHandle<Identifier> cancellable, P2PSocket<Identifier> sock) {
        o.setSocket(sock);
        if (deliverAckToMe != null) deliverAckToMe.receiveResult(o);
      }
    
      public void receiveException(SocketRequestHandle<Identifier> s, Exception ex) {
        o.receiveException(ex);
        if (deliverAckToMe != null) deliverAckToMe.receiveException(ex);
      }
    }, options));    
    
    return o;
  }  
  
  public void closePilot(HighIdentifier i) {
    if (logger.level <= Logger.FINE) logger.log("closePilot("+i+")");
    OutgoingPilot closeMe = outgoingPilots.remove(i);
    if (closeMe != null) {
      closeMe.cancel();
    }
  }
  
  public static final byte PILOT_PING = 1;
  public static final byte PILOT_PONG = 2;
  public static final byte PILOT_REQUEST = 3;

  public static final byte[] PILOT_PING_BYTES = {PILOT_PING};
  public static final byte[] PILOT_PONG_BYTES = {PILOT_PONG};
  public static final byte[] PILOT_SOCKET_BYTES = {PILOT_SOCKET};

  public static final int PILOT_PING_PERIOD = 5000; //60000;
  
  abstract class AbstractPilot extends TimerTask implements P2PSocketReceiver<Identifier> {
    protected P2PSocket<Identifier> socket;

    /**
     * Used to read in ping responses.
     */
    protected SocketInputBuffer sib;
    protected HighIdentifier i;
    private LinkedList<ByteBuffer> queue = new LinkedList<ByteBuffer>();

    protected void enqueue(ByteBuffer bb) {
      if (logger.level <= Logger.FINEST) logger.log(this+".enqueue("+bb+")");
      queue.add(bb);
      socket.register(false, true, this);
    }
    
    protected void write() throws IOException {
      if (queue.isEmpty()) return;
      long ret = socket.write(queue.getFirst());
      if (logger.level <= Logger.FINEST) logger.log(this+" wrote "+ret+" bytes of "+queue.getFirst());
      if (ret < 0) cancel();
      if (queue.getFirst().hasRemaining()) {        
        socket.register(false, true, this);
        return;
      } else {
        queue.removeFirst();
        write();
      }
    }
    
    public void receiveSelectResult(P2PSocket<Identifier> socket,
        boolean canRead, boolean canWrite) throws IOException {
      // write the high identifier
      if (canWrite) {
        write();
      }
      if (canRead) {
        read();
      }
    }
    
    public String toString() {
      return ""+i;
    }
    
    abstract void read() throws IOException;    
  }
  
  class OutgoingPilot extends AbstractPilot implements SocketRequestHandle<HighIdentifier> {
    
    protected SocketRequestHandle<Identifier> cancellable;
    
    protected Map<String, Object> options;
    
    public OutgoingPilot(HighIdentifier i, Map<String, Object> options) {
      this.i = i;
      this.options = options;
      selectorManager.schedule(this, PILOT_PING_PERIOD, PILOT_PING_PERIOD);
    }

    public void receiveException(Exception ex) {
      cancel();
    }

    public void setCancellable(SocketRequestHandle<Identifier> cancellable) {
      this.cancellable = cancellable;
    }

    public void setSocket(P2PSocket<Identifier> socket) {
      if (cancelled) {
        socket.close();
        return;
      }
      this.cancellable = null;
      this.socket = socket;
      try {
        enqueue(ByteBuffer.wrap(PILOT_SOCKET_BYTES));
        enqueue(serializer.serialize(localNodeHandle));
        sib = new SocketInputBuffer(socket,1024);
        receiveSelectResult(socket, true, true);
      } catch (IOException ioe) {
        cancel();
      }
    }
    
    public boolean ping() {
      if (logger.level <= Logger.FINEST) logger.log(this+".ping "+socket);
      if (socket == null) return false;
      enqueue(ByteBuffer.wrap(PILOT_PING_BYTES));
      return true;
    }
    
    
    public void receiveException(P2PSocket<Identifier> socket, Exception ioe) {
      cancel();
    }

    /**
     * Can read a pong or request
     * Can write the initiation or ping
     */
    protected void read() throws IOException {
      try {
        byte msgType = sib.readByte();
        switch(msgType) {
        case PILOT_PONG:
          if (logger.level <= Logger.FINEST) logger.log(this+" received pong");          
          sib.clear();
          read(); // read the next thing, or re-register if there isn't enough to read
          break;
        case PILOT_REQUEST:
          HighIdentifier requestor = serializer.deserialize(sib);
          int uid = sib.readInt();
          if (logger.level <= Logger.FINER) logger.log("Received socket request: requestor:"+requestor+" middleman:"+i+" uid:"+uid);
          openAcceptSocket(requestor, i, uid);
          sib.clear();
          read();
          break;
        }        
      } catch (InsufficientBytesException ibe) {
        socket.register(true, false, this);
        return;
      } catch (IOException ioe) {
//      } catch (ClosedChannelException cce) {
        cancel();
      }
    }
    
    public HighIdentifier getIdentifier() {
      return i;
    }

    public Map<String, Object> getOptions() {
      return options;
    }

    public boolean cancel() {
      super.cancel();
      if (socket == null) {
        if (cancellable != null) {
          cancellable.cancel();
          cancellable = null;
        }
      } else {
        socket.close();        
      }
      outgoingPilots.remove(i);
      return true;
    }

    @Override
    public void run() {
      ping();
    }

  }
  
  // ********* incoming Pilots, only used by non-NATted nodes *************
  Map<HighIdentifier, IncomingPilot> incomingPilots = new HashMap<HighIdentifier, IncomingPilot>();
  
  class IncomingPilot extends AbstractPilot {
    /**
     * Used to read the initial connection information, then re-constructed each time to read pings.
     * Always ready to read the pings.
     */
    public IncomingPilot(P2PSocket<Identifier> socket) throws IOException {
      this.socket = socket;
      sib = new SocketInputBuffer(socket,1024);
      receiveSelectResult(socket, true, true);
    }

    protected void requestSocket(HighIdentifier requestor, int uid) throws IOException {
      if (logger.level <= Logger.FINEST) logger.log("Requesting socket from: "+i+"requestor:"+requestor+" uid:"+uid);

      SimpleOutputBuffer sob = new SimpleOutputBuffer();      
      sob.writeByte(PILOT_REQUEST);
      serializer.serialize(requestor,sob);
      sob.writeInt(uid);
      
      enqueue(sob.getByteBuffer());
    }
    
    protected void read() throws IOException {
//      logger.log(this+".read()");
      if (i == null) {
        // only do this the first time
        try {
          i = serializer.deserialize(sib);
          if (logger.level <= Logger.FINER) logger.log("Received incoming Pilot from "+i);
        } catch (InsufficientBytesException ibe) {
          socket.register(true, false, this);
          return;
        }
        sib.clear();
        incomingPilots.put(i,this);                
        
        // NOTE, it's not important to put a return here, because maybe the node sent a ping while waiting for this step, 
        // just rely on the recovery to properly re-register this
      }

      try {
//        logger.log(this+" reading byte");
        byte msgType = sib.readByte();
        switch(msgType) {
        case PILOT_PING:
          if (logger.level <= Logger.FINER) logger.log(this+" received ping");
          sib.clear();          
          enqueue(ByteBuffer.wrap(PILOT_PONG_BYTES));
          read();  // read the next thing, or re-register if there isn't enough to read
          break;
        }
      } catch (InsufficientBytesException ibe) {
//        logger.log(this+" InsufficientBytesException");
        socket.register(true, false, this);
        return;
      } catch (IOException ioe) {
//      } catch (ClosedChannelException cce) {
        cancel();
      }
    }
      
    public boolean cancel() {
      return super.cancel();
    }

    public void receiveException(P2PSocket<Identifier> socket, Exception ioe) {
      if (i != null) incomingPilots.remove(i);
      socket.close();
    }

    @Override
    public void run() {
      // nothing for now, not scheduled
      
      // TODO Auto-generated method stub
      
    }    
  }

  private static byte[] makeByteArray(byte writeMe) {
    byte[] foo = new byte[1];
    foo[0] = writeMe;
    return foo;
  }
  
  /**
   * Writes a byte then notifies the continuation.
   * @author Jeff Hoye
   */
  class ByteWriter implements P2PSocketReceiver<Identifier> {
    ByteBuffer bytesToWrite;
    Continuation<P2PSocket<Identifier>, Exception> callMeWhenDone;
    
    public ByteWriter(byte writeMe, Continuation<P2PSocket<Identifier>, Exception> callMeWhenDone) {
      this(makeByteArray(writeMe), callMeWhenDone);
    }
    
    public ByteWriter(byte[] writeMe, Continuation<P2PSocket<Identifier>, Exception> callMeWhenDone) {
      this.bytesToWrite = ByteBuffer.wrap(writeMe);
      this.callMeWhenDone = callMeWhenDone;
    }

    public void receiveSelectResult(P2PSocket<Identifier> socket,
        boolean canRead, boolean canWrite) throws IOException {
      long bytesWritten = socket.write(bytesToWrite);
      if (bytesWritten < 0) {
        socket.close();
        callMeWhenDone.receiveException(new ClosedChannelException("Socket "+socket+" closed."));
        return;
      }
      if (bytesToWrite.hasRemaining()) {
        socket.register(false, true, this);
        return;
      }
      
      // done, call the continuation
      callMeWhenDone.receiveResult(socket);
    }
    
    public void receiveException(P2PSocket<Identifier> socket,
        Exception ioe) {      
      socket.close();
      callMeWhenDone.receiveException(ioe);
    }                        
  }
}