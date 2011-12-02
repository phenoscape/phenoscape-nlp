 /* $Id: SyntacticParser.java 790 2011-04-11 17:57:38Z hong1.cui $ */
/**
 * 
 */
package fna.charactermarkup;

/**
 * @author hongcui
 *
 */
public interface SyntacticParser {

	public void POSTagging() throws Exception;
	public void parsing() throws Exception;
	public void extracting() throws Exception;

}
