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
	ArrayList<Entity> entities = new ArrayList<Entity>();
	String qualityClue;
	ArrayList<Entity> subjectEntities = new ArrayList<Entity>(); 
	TermOutputerUtilities ontoutil;
	
	public Parser(TermOutputerUtilities ontoutil){
		this.ontoutil = ontoutil;
		
	}

	protected abstract void parse(Element statement, Element root);
	
	protected void setParseContextSubjects(ArrayList<Entity> subjects) {
		this.subjectEntities = subjects;
		
	}

	protected void setParseContextQualityClue(String qualityClue) {
		this.qualityClue = qualityClue;
		
	}
	
}
