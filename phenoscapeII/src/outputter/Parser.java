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
	
	ArrayList<String> ontologyIRIs;
	ArrayList<Entity> entities = new ArrayList<Entity>();
	String qualityClue;
	ArrayList<Entity> subjectEntities = new ArrayList<Entity>(); 
	
	public Parser(ArrayList<String> ontologyIRIs){
		this.ontologyIRIs = ontologyIRIs; 
	}

	protected abstract void parse(Element statement);
	
	protected void setParseContextSubjects(ArrayList<Entity> subjects) {
		this.subjectEntities = subjects;
		
	}

	protected void setParseContextQualityClue(String qualityClue) {
		this.qualityClue = qualityClue;
		
	}
	
}
