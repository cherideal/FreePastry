package rice.email.proxy.imap.commands.search;

import java.util.*;

import rice.email.proxy.imap.ImapConnection;
import rice.email.proxy.mail.StoredMessage;
import rice.email.proxy.mailbox.*;
import rice.email.proxy.mail.*;


public class StringArgSearchPart extends SearchPart {

  String argument;
  
  public boolean includes(StoredMessage msg) {
    if (getType().equals("BCC")) {
      return handleHeader(msg, "Bcc");
    } else if (getType().equals("BODY")) {
      return handleBody(msg);
    } else if (getType().equals("CC")) {
      return handleHeader(msg, "Cc");
    } else if (getType().equals("FROM")) {
      return handleHeader(msg, "From");
    } else if (getType().equals("KEYWORD")) {
      return handleFlag(msg, true);
    } else if (getType().equals("SUBJECT")) {
      return handleHeader(msg, "Subject");
    } else if (getType().equals("TEXT")) {
      return handleBody(msg);
    } else if (getType().equals("TO")) {
      return handleHeader(msg, "To");
    } else if (getType().equals("UNKEYWORD")) {
      return handleFlag(msg, false);
    } else {
      return false;
    }
  }

  protected boolean handleFlag(StoredMessage msg, boolean set) {
    String flags = msg.getFlagList().toFlagString().toLowerCase();

    if (set) {
      return (flags.indexOf(getArgument().toLowerCase()) >= 0);
    } else {
      return (flags.indexOf(getArgument().toLowerCase()) < 0);
    }
  }

  protected boolean handleBody(StoredMessage msg) {
    return false;
  }

  protected boolean handleHeader(StoredMessage msg, String header) {
    try {
      Enumeration e = msg.getMessage().getMatchingHeaderLines(new String[] {header});

      while (e.hasMoreElements()) {
        String line = (String) e.nextElement();

        if (line.indexOf(getArgument()) >= 0) return true;
      }

      return false;
    } catch (MailboxException e) {
      System.out.println("Exception " + e + " was thrown in StringArgSearchPart.");
      return false;
    } catch (MailException e) {
      System.out.println("Exception " + e + " was thrown in StringArgSearchPart.");
      return false;
    }
  }

  public void setArgument(String argument) {
    this.argument = argument;
  }

  public String getArgument() {
    return argument;
  }
}