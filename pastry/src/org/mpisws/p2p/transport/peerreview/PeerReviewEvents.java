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
package org.mpisws.p2p.transport.peerreview;

public interface PeerReviewEvents {

  public static final short EVT_SEND = 0; // Outgoing message (followed by SENDSIGN entry)
  public static final short EVT_RECV = 1; // Incoming message (followed by SIGN entry)
  public static final short EVT_SIGN = 2;                   /* Signature on incoming message */
  public static final short EVT_ACK = 3;                                  /* Acknowledgment */
  public static final short EVT_CHECKPOINT = 4;                 /* Checkpoint of the state machine */
  public static final short EVT_INIT = 5;                    /* State machine is (re)started */
  public static final short EVT_SENDSIGN = 6;                   /* Signature on outgoing message */
  
  public static final short EVT_SOCKET_OPEN_INCOMING = 9; 
  public static final short EVT_SOCKET_OPEN_OUTGOING = 10; 
  public static final short EVT_SOCKET_OPENED_OUTGOING = 18; 
  public static final short EVT_SOCKET_EXCEPTION = 19; 
  public static final short EVT_SOCKET_CLOSE = 11; 
  public static final short EVT_SOCKET_SHUTDOWN_OUTPUT = 20; 
  public static final short EVT_SOCKET_CLOSED = 12; 
  public static final short EVT_SOCKET_CAN_READ = 13; 
  public static final short EVT_SOCKET_CAN_WRITE = 14; 
  public static final short EVT_SOCKET_CAN_RW = 15; 
  public static final short EVT_SOCKET_READ = 16; 
  public static final short EVT_SOCKET_WRITE = 17;   
  
  public static final short EVT_MIN_SOCKET_EVT = EVT_SOCKET_OPEN_INCOMING;
  public static final short EVT_MAX_SOCKET_EVT = EVT_SOCKET_SHUTDOWN_OUTPUT;
  
  public static final short EX_TYPE_IO = 1;
  public static final short EX_TYPE_ClosedChannel = 2;
  public static final short EX_TYPE_Unknown = 0;
  
}