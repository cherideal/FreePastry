package rice.email;

import java.io.*;
import java.security.*;
import java.util.*;

import rice.*;
import rice.Continuation.*;
import rice.pastry.*;
import rice.post.storage.*;

/**
 * Represents the content of an email which is a single entry
 *
 * @author Alan Mislove
 */
public class EmailSinglePart extends EmailContentPart {

  /**
   * The actual content of this email part
   */
  protected transient EmailData content;

  /**
   * A reference to the content of this email part
   */
  protected EmailDataReference contentReference;

  /**
   * Constructor which takes in an EmailData
   */
  public EmailSinglePart(EmailData content) {
    this.content = content;
  }

  /**
   * Method which writes this part's content out to disk
   * and retrieves a reference to it.
   *
   * @param command The command to run once the data has been stored, and
   *   is returned the success or failure of this command
   */
  public void storeData(Continuation command) {
    if (contentReference != null) {
      command.receiveResult(new Boolean(true));
      return;
    }
    
    storage.storeContentHash(content, new StandardContinuation(command) {
      public void receiveResult(Object o) {
        contentReference = (EmailDataReference) o;

        parent.receiveResult(new Boolean(true));
      }
    });
  }

  /**
   * Method which retrieves and returns this content's EmailData
   *
   * @param command The command to run once the data is available
   */
  public void getContent(Continuation command) {
    if (content != null) {
      command.receiveResult(content);
      return;
    }

    storage.retrieveContentHash(contentReference, new StandardContinuation(command) {
      public void receiveResult(Object o) {
        content = (EmailData) o;

        parent.receiveResult(content);
      }
    });    
  }

  /**
   * Returns whether or not this emailSinglePart is equal to the
   * given object
   *
   * @param o The object to compare to
   * @return Whether or not this is equal to o
   */
  public boolean equals(Object o) {
    if (! (o instanceof EmailSinglePart)) {
      return false;
    }

    EmailSinglePart part = (EmailSinglePart) o;

    if (contentReference != null) {
      return contentReference.equals(part.contentReference);
    } else {
      return content.equals(part.content);
    }
  }
}
