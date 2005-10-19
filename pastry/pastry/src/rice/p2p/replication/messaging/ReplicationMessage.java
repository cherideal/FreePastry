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

package rice.p2p.replication.messaging;

import rice.p2p.commonapi.*;
import rice.p2p.replication.*;

/**
 * @(#) ReplicationMessage.java
 *
 * This class the abstraction of a message used internally by replication.
 *
 * @version $Id$
 *
 * @author Alan Mislove
 */
public abstract class ReplicationMessage implements Message {
  
  // the source of this message
  protected NodeHandle source;
  
  /**
   * Constructor which takes a unique integer Id
   *
   * @param source The source address
   * @param topic The topic
   */
  protected ReplicationMessage(NodeHandle source) {
    this.source = source;
  }
  
  /**
    * Method which returns this messages' source address
   *
   * @return The source of this message
   */
  public NodeHandle getSource() {
    return source;
  }
  
}
