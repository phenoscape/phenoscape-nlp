/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.jdom.Element;

/**
 * @author updates
 * parse out Entities and Qualities, form EQ directly without access state statements as they are just binary values, i.e., T/F. (not two different vaules, but binary values)
 */
public class BinaryCharacterStatementParser extends StateStatementParser {

	/**
	 * @param ontologyIRIs
	 */
	public BinaryCharacterStatementParser(ArrayList<String> ontologyIRIs) {
		super(ontologyIRIs);
	}

	public void parse(Element statement){
		//TODO
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
