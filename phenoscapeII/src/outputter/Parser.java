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
	protected TermOutputerUtilities ontoutil;
	
	public Parser(){
		ontoutil = new TermOutputerUtilities(ApplicationUtilities.getProperty("ontology.dir"), ApplicationUtilities.getProperty("database.name"));
	}

	protected abstract void parse(Element statement, Element root);
	
	protected void setParseContextSubjects(ArrayList<Entity> subjects) {
		this.subjectEntities = subjects;
		
	}

	protected void setParseContextQualityClue(String qualityClue) {
		this.qualityClue = qualityClue;
		
	}
	
}
