header {
package rice.email.proxy.imap.parser.antlr;

import rice.email.proxy.imap.commands.*;
import rice.email.proxy.imap.ImapConnection;

import java.io.IOException;

}
class ImapCommandLexer extends Lexer;

options {
	exportVocab=CommonLex;
	//codeGenMakeSwitchThreshold=999;
	//codeGenBitsetTestThreshold=999;
	defaultErrorHandler=false;
	caseSensitiveLiterals=false;
}

tokens {
	//PATTERN;
	CHECK="CHECK";
	NOOP="NOOP";
	LOGOUT="LOGOUT";
	CAPABILITY="CAPABILITY";
	CREATE="CREATE";
	DELETE="DELETE";
	LIST="LIST";
	SUBSCRIBE="SUBSCRIBE";
	UNSUBSCRIBE="UNSUBSCRIBE";
	LSUB="LSUB";
	EXAMINE="EXAMINE";
	LOGIN="LOGIN";
	AUTHENTICATE="AUTHENTICATE";
	SELECT="SELECT";
	FETCH="FETCH";
	UID="UID";
	APPEND="APPEND";
	COPY="COPY";
	STORE="STORE";
	STATUS="STATUS";
	EXPUNGE="EXPUNGE";
	CLOSE="CLOSE";
	BODY="BODY";
  RFC822="RFC822";
  PEEK="PEEK";
  HEADER="HEADER";
  FIELDS="FIELDS";
  NOT="NOT";
  TEXT="TEXT";
  MIME="MIME";
  SIZE="SIZE";
  ALL="ALL";
  FAST="FAST";
  FULL="FULL"; 
  BODYSTRUCTURE="BODYSTRUCTURE";
  ENVELOPE="ENVELOPE";
  FLAGS="FLAGS";
  INTERNALDATE="INTERNALDATE";
  SEARCH="SEARCH";
  ANSWERED="ANSWERED";
  BCC="BCC";
  BEFORE="BEFORE";
  CC="CC";
  DELETED="DELETED";
  DRAFT="DRAFT";
  FLAGGED="FLAGGED";
  FROM="FROM";
  KEYWORD="KEYWORD";
  LARGER="LARGER";
  NEW="NEW";
  OLD="OLD";
  ON="ON";
  OR="OR";
  RECENT="RECENT";
  RENAME="RENAME";
  SEEN="SEEN";
  SENTBEFORE="SENTBEFORE";
  SENTON="SENTON";
  SENTSINCE="SENTSINCE";
  SINCE="SINCE";
  SMALLER="SMALLER";
  SUBJECT="SUBJECT";
  TO="TO";
  UNANSWERED="UNANSWERED";
  UNDELETED="UNDELETED";
  UNDRAFT="UNDRAFT";
  UNFLAGGED="UNFLAGGED";
  UNKEYWORD="UNKEYWORD";
  UNSEEN="UNSEEN";
}

{
	boolean expectingCommand = true;
}

PERIOD : '.'
  ;

SPACE	:	' '
	;
	
LPAREN	:	'('
	;
	
RPAREN	:	')'
	;
	
LSANGLE	:	'<'
	;
	
RSANGLE	:	'>'
	;

ATOM :
  (ATOM_CHAR)+
	; 

FLAG :	'\\' ATOM
	;

LSBRACKET : '['
	;

RSBRACKET : ']'
	;

protected
NUMBER	:	
  ('0'..'9')+
	;

protected
QUOTE	:	'\"'
	;

protected
QUOTED_CHAR :	~('\"' | '\\' | '\r' | '\n') | ('\\' QUOTED_SPECIALS)
	;

STRING	:	QUOTED // | LITERAL
	;

protected
QUOTED	:	QUOTE! (QUOTED_CHAR)* QUOTE!
	;


protected
QUOTED_SPECIALS :	'\"' | '\\'
	;
  
protected
ATOM_CHAR : ~('<' | '>' | '.' | '(' | ')' | '{' | '[' | ']' | ' ' | '\\' | '\"' | '\u007f'..'\u00FF' | '\u0000'..'\u001f')
  ;

protected
CHAR	:	('\u0001'..'\u007f')
	;

protected
CTL	:	('\u0000'..'\u001f' | '\u007f')
	;

protected
PLUS	:	'+'
	;

LITERAL_START	:	'{'! NUMBER '}'!
	;

protected
UNKNOWN :	(options {greedy=false;}:.)* '\r' '\n'
	;