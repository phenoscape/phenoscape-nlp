 /* $Id: ChunkRatio.java 790 2011-04-11 17:57:38Z hong1.cui $ */
/**
 * 
 */
package fna.charactermarkup;

/**
 * @author hongcui
 *
 */
public class ChunkRatio extends Chunk {
	String label;
	/**
	 * @param text
	 */
	public ChunkRatio(String text, String label) {
		super(text);
		this.label = label;
	}

	public String getLabel(){
		return this.label;
	}
}
