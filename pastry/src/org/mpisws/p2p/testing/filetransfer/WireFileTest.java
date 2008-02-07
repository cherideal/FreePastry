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
package org.mpisws.p2p.testing.filetransfer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

import org.mpisws.p2p.filetransfer.FileTransfer;
import org.mpisws.p2p.filetransfer.FileTransferCallback;
import org.mpisws.p2p.filetransfer.FileTransferImpl;
import org.mpisws.p2p.transport.P2PSocket;
import org.mpisws.p2p.transport.SocketCallback;
import org.mpisws.p2p.transport.SocketRequestHandle;
import org.mpisws.p2p.transport.TransportLayerCallback;
import org.mpisws.p2p.transport.util.DefaultErrorHandler;
import org.mpisws.p2p.transport.wire.WireTransportLayer;
import org.mpisws.p2p.transport.wire.WireTransportLayerImpl;

import rice.environment.Environment;
import rice.environment.logging.Logger;
import rice.p2p.commonapi.appsocket.AppSocket;
import rice.pastry.transport.SocketAdapter;

public class WireFileTest {

  /**
   * @param args
   */
  public static void main(String[] args) throws IOException {
    // globals
    final Environment env = new Environment();
    InetAddress local = InetAddress.getLocalHost();
    final Logger logger = env.getLogManager().getLogger(WireFileTest.class, null);
    
    // this node will receive a file
    InetSocketAddress addr1 = new InetSocketAddress(local, 9001);
    WireTransportLayer wtl1 = new WireTransportLayerImpl(addr1,env,new DefaultErrorHandler<InetSocketAddress>(logger));
    wtl1.setCallback(new TransportLayerCallback<InetSocketAddress, ByteBuffer>() {
    
      public void messageReceived(InetSocketAddress i, ByteBuffer m,
          Map<String, Object> options) throws IOException {
        // TODO Auto-generated method stub
    
      }
    
      public void incomingSocket(P2PSocket<InetSocketAddress> s) throws IOException {
        // we got a socket, convert it to an AppSocket, then a FileTransfer
        logger.log("incomingSocket("+s+")");    
        AppSocket sock = new SocketAdapter<InetSocketAddress>(s, env);
        FileTransfer ft = new FileTransferImpl(sock,new FileTransferCallback() {
        
          public void messageReceived(ByteBuffer bb) {
            // TODO Auto-generated method stub
        
          }
        
          public void fileReceived(File f, String s) {
            logger.log("file received "+f+" named:"+s+" size:"+f.length());
          }
          public void receiveException(Exception ioe) {
            logger.logException("FTC.receiveException()", ioe);
          }

        },env);
      }
    
    });
    
    
    // this node will send a file
    InetSocketAddress addr2 = new InetSocketAddress(local, 9002);
    WireTransportLayer wtl2 = new WireTransportLayerImpl(addr2,env,new DefaultErrorHandler<InetSocketAddress>(logger));

    wtl2.openSocket(addr1, new SocketCallback<InetSocketAddress>() {
    
      public void receiveResult(SocketRequestHandle<InetSocketAddress> cancellable,
          P2PSocket<InetSocketAddress> s) {
        logger.log("opened Socket "+s);
        
        // we got the socket we requested, convert it to an AppSocket, then a FileTransfer
        AppSocket sock = new SocketAdapter<InetSocketAddress>(s, env);
        FileTransfer ft = new FileTransferImpl(sock, new FileTransferCallback() {
        
          public void messageReceived(ByteBuffer bb) {
            // TODO Auto-generated method stub
        
          }
        
          public void fileReceived(File f, String s) {
            // TODO Auto-generated method stub
        
          }
          public void receiveException(Exception ioe) {
            logger.logException("FTC.receiveException()", ioe);
          }
        
        }, env);       
        
        // send a file normal priority, don't worry about notification of completion
        try {
          ft.sendFile(new File("delme.txt"), "foo", (byte)0, null);
        } catch (IOException ioe) {
          logger.logException("Error sending file.", ioe);
        }
        
      }
    
      public void receiveException(SocketRequestHandle<InetSocketAddress> s,
          Exception ex) {
        // TODO Auto-generated method stub
    
      }    
    }, null);

  }

}
