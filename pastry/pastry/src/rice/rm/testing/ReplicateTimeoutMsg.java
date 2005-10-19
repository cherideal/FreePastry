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


package rice.rm.testing;

import rice.pastry.*;
import rice.pastry.security.*;
import rice.pastry.routing.*;
import rice.pastry.messaging.*;

import rice.rm.*;
import rice.rm.messaging.*;

import java.io.*;
import java.util.*;

/**
 *
 * 
 * @version $Id$ 
 * 
 * @author Animesh Nandi
 */


public class ReplicateTimeoutMsg extends TestMessage implements Serializable
{

    private Id objectKey;
    

    /**
     * Constructor
     */
    public 
	ReplicateTimeoutMsg(NodeHandle source, Address address, Id _objectKey, Credentials authorCred) {
	super(source, address, authorCred);
	objectKey = _objectKey;
	
    }
    
    public void handleDeliverMessage( RMRegrTestApp testApp) {
	System.out.println("Timeout message: at " + testApp.getNodeId());
	Id key = getObjectKey();
	RMRegrTestApp.ReplicateEntry entry;

	entry = testApp.getPendingObject(key);
	if(entry == null) {
	    // It means that we received all the Acks before the timeout
	    return;
	}
	// On the assumption that atleast one node in the replicaSet
	// is a good node and replies within the Timeout period
	if(entry.getNumAcks()==0) {
	    // Notify application of failure
	    testApp.replicateSuccess(key,false);
	}
	else {
	    // Notify application of success
	    testApp.replicateSuccess(key,true);
	}
	// We also get rid of the state associated with this object from
	// the pendingObjectList
	testApp.removePendingObject(key);
    }
   

    public Id getObjectKey() {
	return objectKey;
    }


    public String toString() {
	return new String( "REPLICATE_TIMEOUT  MSG:" );
    }
}








