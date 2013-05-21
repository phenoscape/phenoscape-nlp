/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.jdom.Element;

/**
 * @author updates
 *
 */
public abstract class Parser {
	
	//ArrayList<String> ontologyIRIs;
	ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
	String qualityClue;
	ArrayList<EntityProposals> subjectEntities = new ArrayList<EntityProposals>(); 
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
