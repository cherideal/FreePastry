package rice.email;

import rice.*;
import rice.post.*;
import rice.post.log.*;
import rice.post.messaging.*;
import rice.post.storage.*;
import rice.email.log.*;
import rice.email.messaging.*;

/**
 * Represents a notion of a message in the POST system.  This class is designed 
 * to be a small representation of an Email, with pointers to all of the content.
 */
public class Email implements java.io.Serializable {

  private PostUserAddress sender;
  private PostEntityAddress[] recipients;
  private String subject;
  private transient EmailData body;
  private transient EmailData[] attachments;
  private EmailDataReference bodyRef;
  private EmailDataReference[] attachmentRefs;
  private transient StorageService storage;
  
  /**
   * Constructs an Email.
   *
   * @param sender The address of the sender of the mail.
   * @param recipientUsers The addresses of the recipient users of the mail.
   * @param recipientGroups The addresses of the recipient groups of the mail.
   * @param subject The subject of the message.
   * @param body The body of the message.
   * @param attachments The attachments to the message (could be zero-length.)
   */
  public Email(PostUserAddress sender, 
               PostEntityAddress[] recipients, 
               String subject, 
               EmailData body, 
               EmailData[] attachments) {
    
    this.sender = sender;
    this.recipients = recipients;
    this.subject = subject;
    this.body = body;
    this.attachments = attachments;
    this.bodyRef = null;
    this.attachmentRefs = null;
  }
    
  /**
   * Returns the sender of this message.
   *
   * @return The sender of this email.
   */
  public PostUserAddress getSender() {
    return this.sender;
  }
    
  /**
   * Returns the recipient users of this message.
   *
   * @return The recipient users of this email.
   */
  public PostEntityAddress[] getRecipients() {
    return this.recipients;
  }

  /**
   * Sets the storage service for the email.  I (JM) added this method
   * so that the EmailService can set the Email's storage whenever the
   * email is sent or received, which lets the EmailClient be
   * effectively ignorant of the storage service (which is good, since
   * this Service is part of the POST layer).
   *
   * @param s the StorageService the email is to use
   */
  protected void setStorage(StorageService s) {
    storage = s;
  }
  
  /**
   * Returns the subject of this message.
   *
   * @return The subject of this email.
   */
  public String getSubject() {
    return this.subject;
  }
     
  /**
   * Returns the  body of this message.  Should be text.
   *
   * @return The body of this email.
   */
  public void getBody(ReceiveResultCommand command) {
    // if the body has not been fetched already, fetch it
    if (this.body == null) {
      // make a new command to store the returned body and 
      // then return the body once it has been stored
      EmailGetBodyTask preCommand = new EmailGetBodyTask(command);
      // start the fetching process
      storage.retrieveContentHash(bodyRef, preCommand);
    }
  }
     
  /**
   * Returns the attachments of this message.
   *
   * @return The attachments of this email.
   */
  public void getAttachments(ReceiveResultCommand command) {
    // if the attachments have not been fetched already, and there are refs to the attachments, 
    // fetch the attachments
    if ((this.attachments == null) && (this.attachmentRefs != null)) {
      // make a new command to store the returned attachment and start to fetch the next attachment.
      // Once the attachments have all been fetch, call the user's command
      EmailGetAttachmentsTask preCommand = new EmailGetAttachmentsTask(0, command);
      // start the fetching process
      storage.retrieveContentHash(attachmentRefs[0], preCommand);
    }
  }

  /**
   * Stores the content of the Email into PAST and 
   * saves the references to the content in the email.  
   * Should be called before the Email is sent over the wire.
   */
  protected void storeData() {   
    if (this.attachmentRefs == null) {
      attachmentRefs = new EmailDataReference[attachments.length];
      // insert the email attachments into PAST, store their references
      for (int i = 0; i < attachments.length; i++) {
	try {
	attachmentRefs[i] = (EmailDataReference)storage.storeContentHash(attachments[i]); 
	} catch (StorageException s) {
	// JM do something sensible here
	}
      }
    }

    // if the body has not already been inserted into PAST
    // JM try replacing this with "if (bodyRef == null) { " for a laugh
    if (!(this.bodyRef instanceof EmailDataReference)) {
      // insert the email body into PAST, store the reference
      try {
	bodyRef = (EmailDataReference)storage.storeContentHash(body);
	} catch (StorageException s) {
	// JM do something sensible here
      }
    }
  }

  /**
   * This class is used to fetch an email body, and then store the result.  
   * To return the result to the user, the user's given command is called once
   * the body has been stored.
   */
  protected class EmailGetBodyTask implements ReceiveResultCommand {
    private ReceiveResultCommand _command;
    
    /**
     * Constructs a EmailGetBodyTask given a user-command.
     */
    public EmailGetBodyTask(ReceiveResultCommand command) {
      _command = command;
    }

    /**
     * Starts the processing of this task.
     */
    public void start() {
      // JM I don't believe anything needs to be done here
    }

    /**
     * Stores the result, and then calls the user's command.
     */
    public void receiveResult(Object o) {
      // store the fetched body
      try {
	body = (EmailData)o;      
      } catch (Exception e) {
	System.out.println("The email body was fetched, but had problems " + o);
      }
      // pass the result along to the caller
      _command.receiveResult(o);
    }

    /**
     * Simply prints out the error,
     */  
    public void receiveException(Exception e) {
      System.out.println("Exception " + e + "  occured while trying to fetch an email body");
    }
  }

  /**
   * This class is used to fetch an email attachment, and store the result,
   * and then fetch the next attachment.  Once each of the attachments have
   * been fetched and stored, calls the user's given command.
   */
  protected class EmailGetAttachmentsTask implements ReceiveResultCommand {
    private int _index;
    private ReceiveResultCommand _command;
    
    /**
     * Constructs a EmailGetAttachmentsTask given a user-command.
     */
    public EmailGetAttachmentsTask(int i, ReceiveResultCommand command) {
      _index = i;
      _command = command;
    }

    /**
     * Starts the processing of this task.
     */
    public void start() {
      // JM I don't believe anything needs to be done here
    }

    /**
     * Stores the result, and then fetches the next attachment.  If there are no more
     * attachments, calls the user's provided command.
     */
    public void receiveResult(Object o) {
      // store the fetched attachment
      try {
	attachments[_index] = (EmailData)o;  
	// if there are more attachments, fetch the next one
	if (_index < attachmentRefs.length) {
	  _index = _index + 1;
	  EmailGetAttachmentsTask preCommand = new EmailGetAttachmentsTask(_index, _command);
	  storage.retrieveContentHash(attachmentRefs[_index], preCommand);
	// otherwise pass the result along to the caller
	} else {
	  _command.receiveResult(attachments);
	}    
      } catch (Exception e) {
	System.out.println("The email attachment " + _index + " was fetched, but had problems. " + o);
      }      
    }

    /**
     * Simply prints out the error,
     */  
    public void receiveException(Exception e) {
      System.out.println("Exception " + e + "  occured while trying to fetch email attachment " + _index);
    }
  }
}
