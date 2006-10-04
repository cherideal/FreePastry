/*
 * Created on Feb 21, 2006
 */
package rice.p2p.util.rawserialization;

import java.io.*;

import rice.environment.logging.Logger;
import rice.p2p.commonapi.*;
import rice.p2p.commonapi.rawserialization.*;
import rice.pastry.PastryNode;

/**
 * Handles "old" java serialized messages for programming convienience
 * and reverse compatability.
 * 
 * @author Jeff Hoye
 */
public class JavaSerializedDeserializer implements MessageDeserializer {

  protected Endpoint endpoint;
  private boolean deserializeOnlyTypeZero = true;
  
  public JavaSerializedDeserializer(Endpoint endpoint) {
    this.endpoint = endpoint;
  }
  
  public void setAlwaysUseJavaSerialization(boolean val) {
    deserializeOnlyTypeZero = !val; 
  }
  
  public Message deserialize(InputBuffer buf, short type, byte priority, NodeHandle sender) throws IOException {
    if (deserializeOnlyTypeZero && (type != 0)) throw new IllegalArgumentException("Type must be zero, was "+type+".  See http://freepastry.org/FreePastry/extendingRawMessages.html for more information."); 
    

    Object o = null;
    try {
      byte[] array = new byte[buf.bytesRemaining()];
      buf.read(array);
      
      ObjectInputStream ois = new JavaDeserializer(new ByteArrayInputStream(array), endpoint);
      
      o = ois.readObject();
      Message ret = (Message)o;

      return ret;
    } catch (StreamCorruptedException sce) {
      if (!deserializeOnlyTypeZero)
        throw new RuntimeException("Not a java serialized message!  See http://freepastry.org/FreePastry/extendingRawMessages.html for more information.", sce);
      else 
        throw sce;
//    } catch (ClassCastException e) {
//      if (logger.level <= Logger.SEVERE) logger.log(
//          "PANIC: Serialized message was not a pastry message!");
//      throw new IOException("Message recieved " + o + " was not a pastry message - closing channel.");
    } catch (ClassNotFoundException e) {
//      if (logger.level <= Logger.SEVERE) logger.log(
//          "PANIC: Unknown class type in serialized message!");
      throw new RuntimeException("Unknown class type in message - closing channel.", e);
//    } catch (InvalidClassException e) {
//      if (logger.level <= Logger.SEVERE) logger.log(
//          "PANIC: Serialized message was an invalid class! " + e.getMessage());
//      throw new IOException("Invalid class in message - closing channel.");
//    } catch (IllegalStateException e) {
//      if (logger.level <= Logger.SEVERE) logger.log(
//          "PANIC: Serialized message caused an illegal state exception! " + e.getMessage());
//      throw new IOException("Illegal state from deserializing message - closing channel.");
////    } catch (NullPointerException e) {
////      if (logger.level <= Logger.SEVERE) logger.logException(
////          "PANIC: Serialized message caused a null pointer exception! " , e);
////      
////      return null;
//    } catch (Exception e) {
//      if (logger.level <= Logger.SEVERE) logger.log(
//          "PANIC: Serialized message caused exception! " + e.getMessage());
//      throw new IOException("Exception from deserializing message - closing channel.");
    }
  }

  
  
}