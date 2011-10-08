/**
 * $Id: ParsingException.java 840 2011-06-05 03:57:51Z hong1.cui $
 */
package fna.parsing;

/**
 * To encapsulate all the exceptions in the parsing package.
 * 
 * @author chunshui
 */
public class ParsingException extends RuntimeException {

	private static final long serialVersionUID = 460940822178582494L;
	
	public ParsingException() {
		
	}
	
	public ParsingException(String message) {
		super(message);
	}
	
	public ParsingException(Throwable cause) {
		super(cause);
	}
	
	public ParsingException(String message, Throwable cause) {
		super(message, cause);
	}
}
