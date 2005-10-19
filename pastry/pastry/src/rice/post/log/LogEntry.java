package rice.post.log;

import java.security.*;

import rice.*;
import rice.p2p.commonapi.*;
import rice.post.*;
import rice.post.storage.*;

/**
 * Abstract class for all entries in the log. Each application using post should
 * implement a class hierarchy of log entries relevant to the semantics of that
 * system.
 * 
 * @version $Id$
 */
public abstract class LogEntry implements PostData {

  // the user in whose log this entry appears
  private PostEntityAddress user;
  
  // a reference to the previous entry in the log
  private LogEntryReference previousEntryReference;

  // the previous entry in the log
  private transient LogEntry previousEntry;

  // the local Post service
  private transient Post post;

  // this logentry's "parent" logentry (if it is, say, wrapped in an encLogEntry)
  private transient LogEntry parent;
  
  /**
   * Constructs a LogEntry
   */
  public LogEntry() {
  }

  /**
   * Sets the user of this log entry
   *
   * @param user The user who created this entry
   */
  public void setUser(PostEntityAddress user) {
    if (this.user == null) {
      this.user = user;
    } else {
      System.out.println("ERROR - Trying to set user on already-set log.");
      (new Exception()).printStackTrace();
    }
  }
  
  /**
   * Sets the reference to the previous entry in the log
   *
   * @param ref A reference to the previous log entry
   */
  public void setPreviousEntryReference(LogEntryReference ref) {
    if (previousEntryReference == null) {
      previousEntryReference = ref;
    } else {
      System.out.println("ERROR - Trying to set previous ref on already-set log.");
      (new Exception()).printStackTrace();
    }
  }

  /**
   * Returns the reference to the previous entry in the log
   *
   * @return A reference to the previous log entry
   */
  public void getPreviousEntry(final Continuation command) {
    if (parent != null) {
      parent.getPreviousEntry(command);
      return;
    }
    
    if ((previousEntry == null) && (previousEntryReference != null)) {
      Continuation fetch = new Continuation() {
        public void receiveResult(Object o) {
          try {
            previousEntry = (LogEntry) o;
            previousEntry.setPost(post);
            command.receiveResult(previousEntry);
          } catch (ClassCastException e) {
            command.receiveException(e);
          }
        }

        public void receiveException(Exception e) {
          command.receiveException(e);
        }
      };

      post.getStorageService().retrieveContentHash(previousEntryReference, fetch);
    } else {
      command.receiveResult(previousEntry);
    }
  }

  /**
   * Protected method which sets the post service
   */
  void setPost(Post post) {
    this.post = post;
  }

  /**
   * Method which set's this log entry's parent, if, for example, it is inside an
   * encrypted log entry.
   */
  void setParent(LogEntry parent) {
    this.parent = parent;
  }

  /**
    * Protected method which sets the post service
   *
   */
  void setPreviousEntry(LogEntry entry) {
    if (previousEntry == null) {
      previousEntry = entry;
    } else {
      System.out.println("ERROR - Attempting to set a previous entry with an existing one in LogEntry!");
    }
  }

  /**
   * This method is not supported (you CAN NOT store a log entry as a
   * public-key signed block).
   *
   * @param location The location of this object.
   * @throws IllegalArgument Always
   */
  public SignedReference buildSignedReference(Id location) {
    throw new IllegalArgumentException("Log entries are only stored as content-hash.");
  }

  /**
   * Builds a LogEntryReference object to this log, given a location and
   * the encryption key
   *
   * @param location The location of the stored data
   * @param key The key used to encrypt this object
   * @return A LogEntryReference to this object
   */
  public ContentHashReference buildContentHashReference(Id location, byte[] key) {
    return new LogEntryReference(location, key);
  }

  /**
   * This method is not supported (you CAN NOT store a log as a
   * secure block).
   *
   * @param location The location of the data
   * @param key The for the data
   * @throws IllegalArgumentException Always
   */
  public SecureReference buildSecureReference(Id location, byte[] key) {
    throw new IllegalArgumentException("Log entries are only stored as content-hash blocks.");
  }  
}
