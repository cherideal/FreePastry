package rice.post.messaging;

import java.io.*;
import java.security.*;

import rice.post.messaging.*;
import rice.post.*;

import rice.p2p.commonapi.*;

/**
 * This class wraps an EncrypedNotificationMessage and is
 * used after the receipt of a PresenceMessage.
 */
public class DeliveryMessage extends PostMessage {

  private SignedPostMessage message;
  private Id location;
  private Id id;

  /**
    * Constructs a DeliveryMessage
   *
   * @param sender The sender of this delivery 
   * @param message The message to deliver, in encrypted state
   */
  public DeliveryMessage(PostEntityAddress sender,
                         Id location,
                         Id id,
                         SignedPostMessage message) {
    super(sender);
    this.location = location;
    this.id = id;
    this.message = message;
  }

  /**
   * Gets the location of the DRM in the ring
   *
   * @return The location 
   */
  public Id getId() {
    return id;
  }

  /**
    * Gets the location of the user.
   *
   * @return The location in the Pastry ring of the user.
   */
  public Id getLocation() {
    return location;
  }

  /**
   * Gets the EncryptedNotificationMessage which this is a Request for.
   * for.
   *
   * @return The internal message, in encrypted state
   */
  public SignedPostMessage getEncryptedMessage() {
    return message;
  }
}
