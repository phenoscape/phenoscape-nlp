/**
 * 
 */
package outputter.process;

import java.util.ArrayList;

import org.jdom.Element;

import outputter.data.EntityProposals;
import outputter.knowledge.TermOutputerUtilities;

/**
 * @author Hong Cui
 *
 */
public abstract class Parser {
	
	ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>(); //all entities parsed
	String qualityClue;
	ArrayList<EntityProposals> subjectEntities = new ArrayList<EntityProposals>(); //entities that are used as a subject
	TermOutputerUtilities ontoutil;
	
	public Parser(TermOutputerUtilities ontoutil){
		this.ontoutil = ontoutil;
	}

	protected abstract void parse(Element statement, Element root);
	
	protected void setParseContextSubjects(ArrayList<EntityProposals> subjects) {
		this.subjectEntities = subjects;
		
	}

	protected void setParseContextQualityClue(String qualityClue) {
		this.qualityClue = qualityClue;
		
	}
	
}
