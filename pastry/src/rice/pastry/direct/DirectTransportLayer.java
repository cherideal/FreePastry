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
package rice.pastry.direct;

import java.io.IOException;
import java.util.Map;

import org.mpisws.p2p.transport.ErrorHandler;
import org.mpisws.p2p.transport.MessageCallback;
import org.mpisws.p2p.transport.MessageRequestHandle;
import org.mpisws.p2p.transport.P2PSocket;
import org.mpisws.p2p.transport.SocketCallback;
import org.mpisws.p2p.transport.SocketRequestHandle;
import org.mpisws.p2p.transport.TransportLayer;
import org.mpisws.p2p.transport.TransportLayerCallback;
import org.mpisws.p2p.transport.exception.NodeIsFaultyException;
import org.mpisws.p2p.transport.liveness.LivenessListener;
import org.mpisws.p2p.transport.liveness.LivenessProvider;
import org.mpisws.p2p.transport.util.MessageRequestHandleImpl;
import org.mpisws.p2p.transport.util.SocketRequestHandleImpl;

import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.Cancellable;
import rice.pastry.direct.DirectAppSocket.DirectAppSocketEndpoint;

public class DirectTransportLayer<Identifier, MessageType> implements TransportLayer<Identifier, MessageType> {
  protected boolean acceptMessages = true;
  protected boolean acceptSockets = true;

  protected Identifier localIdentifier;
  protected TransportLayerCallback<Identifier, MessageType> callback;
  protected GenericNetworkSimulator<Identifier, MessageType> simulator;
  protected ErrorHandler<Identifier> errorHandler;
  protected LivenessProvider<Identifier> livenessProvider;
  
  protected Environment environment;
  protected Logger logger;
  
  public DirectTransportLayer(Identifier local, 
      GenericNetworkSimulator<Identifier, MessageType> simulator, 
      LivenessProvider<Identifier> liveness, Environment env) {
    this.localIdentifier = local;
    this.simulator = simulator;
    this.livenessProvider = liveness;
    
    this.environment = env;
    this.logger = environment.getLogManager().getLogger(DirectTransportLayer.class, null);
  }
  
  public void acceptMessages(boolean b) {
    acceptMessages = b;
  }

  public void acceptSockets(boolean b) {
    acceptSockets = b;
  }

  public Identifier getLocalIdentifier() {
    return localIdentifier;
  }
  
  static class CancelAndClose implements Cancellable {
    DirectAppSocket closeMe;
    Cancellable cancelMe;
    
    public boolean cancel() {
      closeMe.connectorEndpoint.close();
      return cancelMe.cancel();
    }
    
  }

  public SocketRequestHandle<Identifier> openSocket(Identifier i, SocketCallback<Identifier> deliverSocketToMe, Map<String, Integer> options) {
    SocketRequestHandleImpl<Identifier> handle = new SocketRequestHandleImpl<Identifier>(i,options);
    DirectAppSocket<Identifier, MessageType> socket = new DirectAppSocket<Identifier, MessageType>(i, localIdentifier, deliverSocketToMe, simulator, handle, options);
    CancelAndClose cancelAndClose = new CancelAndClose();
    handle.setSubCancellable(cancelAndClose);
    cancelAndClose.cancelMe = simulator.enqueueDelivery(socket.getAcceptorDelivery(),
        (int)Math.round(simulator.networkDelay(localIdentifier, i)));
    return handle;
  }

  public MessageRequestHandle<Identifier, MessageType> sendMessage(
      Identifier i, MessageType m, 
      MessageCallback<Identifier, MessageType> deliverAckToMe, 
      Map<String, Integer> options) {
    
    MessageRequestHandleImpl<Identifier, MessageType> handle = new MessageRequestHandleImpl<Identifier, MessageType>(i, m, options);
    
    if (livenessProvider.getLiveness(i, null) >= LivenessListener.LIVENESS_DEAD) {
      if (logger.level <= Logger.FINE)
        logger.log("Attempt to send message " + m
            + " to a dead node " + i + "!");      
      
      if (deliverAckToMe != null) deliverAckToMe.sendFailed(handle, new NodeIsFaultyException(i));
    } else {
      int delay = (int)Math.round(simulator.networkDelay(localIdentifier, i));
//      simulator.notifySimulatorListenersSent(m, localIdentifier, i, delay);
      handle.setSubCancellable(simulator.deliverMessage(m, i, localIdentifier, delay));
      if (deliverAckToMe != null) deliverAckToMe.ack(handle);
    }
    return handle;
  }

  public void setCallback(TransportLayerCallback<Identifier, MessageType> callback) {
    this.callback = callback;
  }

  public void setErrorHandler(ErrorHandler<Identifier> handler) {
    this.errorHandler = handler;
  }

  public void destroy() {
    simulator.remove(getLocalIdentifier());
  }

  public boolean canReceiveSocket() {
    return acceptSockets;
  }

  public void finishReceiveSocket(P2PSocket<Identifier> acceptorEndpoint) {
    try {
      callback.incomingSocket(acceptorEndpoint);
    } catch (IOException ioe) {
      if (logger.level <= Logger.WARNING) logger.logException("Exception in "+callback,ioe);
    }
  }

  public Logger getLogger() {
    return logger;
  }

  int seq = Integer.MIN_VALUE;
  
  public synchronized int getNextSeq() {
    return seq++;
  }
    
  public void incomingMessage(Identifier i, MessageType m, Map<String, Integer> options) throws IOException {
    callback.messageReceived(i, m, options);
  }

  public void clearState(Identifier i) {
    // do nothing
  }
}