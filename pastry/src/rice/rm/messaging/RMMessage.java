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


package rice.rm.messaging;

import rice.pastry.*;
import rice.pastry.messaging.*;
import rice.pastry.security.*;

import rice.rm.*;

import java.util.Random;
import java.io.*;

/**
 * @(#) RMMessage.java
 *
 * A RM message. These messages are exchanged between the RM modules on the pastry nodes. 
 *
 * @version $Id$
 * @author Atul Singh
 * @author Animesh Nandi
 */
public abstract class RMMessage extends Message implements Serializable{


    /**
     * The credentials of the author for the object contained in this object
     */
    private Credentials _authorCred;
     
   
    /**
     * The ID of the source of this message.
     * Should be serializable.
     */
    protected NodeHandle _source;

    // for debugging purposes
    private int _seqno;


    /**
     * Constructor : Builds a new RM Message
     * @param address RM Application address
     */
    public RMMessage(NodeHandle source, Address address, Credentials authorCred, int seqno) {
	super(address);
	this._source = source; 
	this._authorCred = authorCred;
	this._seqno = seqno;
    }
    

     /**
     * This method is called whenever the rm node receives a message for 
     * itself and wants to process it. The processing is delegated by rm 
     * to the message.
     * 
     */
    public abstract void 
	handleDeliverMessage( RMImpl rm);
    

    public int getSeqno() {
	return _seqno;
    }

    public NodeHandle getSource() {
	return _source;
    }
    
    
    /**
     * Gets the author's credentials associated with this object
     * @return credentials
     */
    public Credentials getCredentials(){
	return _authorCred;
    }

    
}





