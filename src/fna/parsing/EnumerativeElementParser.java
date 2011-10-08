/**
 * 
 */
package fna.parsing;
import org.jdom.Element;


/**
 * @author Hong Updates
 *
 */
public abstract class EnumerativeElementParser {
	protected Element parent = null;
	protected String text = null;
	protected String enutag = null;
	
	public EnumerativeElementParser(Element parent, String text, String enutag){
		this.parent = parent;
		this.text = text;
		this.enutag = enutag;
	}

	protected abstract Element parse();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
