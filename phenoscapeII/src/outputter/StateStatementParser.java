/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.jdom.Element;

/**
 * @author updates
 *
 *calling this parser with contextSubject and optionally qualityClue.
 *parse out EQ statements from a state statement, using context subjects and quality clues from the character statement
 */
public class StateStatementParser extends Parser {
	ArrayList<EQStatement> EQStatements = new ArrayList<EQStatement> (); 
	/**
	 * 
	 */
	public StateStatementParser(ArrayList<String> ontologyIRIs) {
		super(ontologyIRIs);
	}

	/* (non-Javadoc)
	 * @see outputter.Parser#parse(org.jdom.Element)
	 */
	@Override
	public void parse(Element statement) {
		//TODO move createEQfromStateStatement here
	}

	public ArrayList<EQStatement> getEQStatements(){
		return this.EQStatements;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
