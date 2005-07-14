/*
 * Created on Jul 6, 2005
 */
package rice.tutorial.lesson0a;

import rice.Continuation;
import rice.p2p.past.PastContent;


class MyContinuation implements Continuation {
  public void receiveResult(Object result) {
    PastContent pc = (PastContent)result;
    System.out.println("Received a "+pc);
  }

  public void receiveException(Exception result) {
    System.out.println("There was an error: "+result);      
  }
}