package rice.email.proxy.imap.commands;

import rice.email.proxy.imap.ImapState;

import rice.email.proxy.mailbox.MailFolder;
import rice.email.proxy.mailbox.MailboxException;
import rice.email.proxy.mailbox.MsgFilter;


/**
 * EXAMINE command.
 * 
 * <p>
 * <a href="http://asg.web.cmu.edu/rfc/rfc2060.html#sec-6.3.2">
 * http://asg.web.cmu.edu/rfc/rfc2060.html#sec-6.3.2 </a>
 * </p>
 */
public class ExamineCommand extends AbstractImapCommand {

  String _folder;

  public ExamineCommand() {
    super("EXAMINE");
  }

  public boolean isValidForState(ImapState state) {
    return state.isAuthenticated();
  }

  public ExamineCommand(String name) {
    super(name);
  }

  public void execute() {
    try {
      if (_folder != null) {
        MailFolder fold = getState().getFolder(_folder);

        untaggedResponse(fold.getExists() + " EXISTS");
        untaggedResponse(fold.getRecent() + " RECENT");
        untaggedSuccess("[UIDVALIDITY " + fold.getUIDValidity() + "] UIDs valid ");
        untaggedSuccess("[UIDNEXT " + fold.getNextUID() + "] Predicted next UID ");
        untaggedResponse("FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)");

        if (isWritable()) {
          untaggedSuccess("[PERMANENTFLAGS (\\* \\Answered \\Flagged \\Deleted \\Seen \\Draft)] Permanent flags");
        } else {
          untaggedSuccess("[PERMANENTFLAGS ()] No permanent flags permitted");
        }
      }

      String writeStatus = isWritable() ? "[READ-WRITE]" : "[READ-ONLY]";

      taggedSuccess(writeStatus + " " + getCmdName() + " completed");
    } catch (MailboxException e) {
      taggedExceptionFailure(e);
    }
  }

  protected boolean isWritable() {
    return false;
  }

  public String getFolder() {
    return _folder;
  }

  public void setFolder(String mailbox) {
    _folder = mailbox;
  }
}