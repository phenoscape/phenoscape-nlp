/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.jdom.Element;

/**
 * @author updates
 * calculate overall confidence score for this character unit
 * based on
 * 1. formalconcept-ontology matching confidence
 * 2. overall soundness of the EQs
 * 3. # of decisions made to generate the EQs (more decision steps, more chance for errors)
 * 4. # of alternative options available at each decision points (fewer choice, less chance for errors)
 */
public class ConfidenceScoring {
	
	
	/**
	 * 
	 */
	public ConfidenceScoring() {
	}

	public float score(Element characterUnit, ArrayList<EQStatement> EQStatements){
		//TODO
		return 0;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("");


	}

}
