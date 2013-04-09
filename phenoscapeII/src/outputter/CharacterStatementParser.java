/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.jdom.Element;

/**
 * @author updates
 * identify entities, and spatial regions and entity locators associated with the entities, form simple or composite entities
   also identify quality clues from the character statement, such as "size of" "number of", and "fusion of". 
 */
public class CharacterStatementParser extends Parser {
	ArrayList<String> ontologyIRIs;
	ArrayList<Entity> entities = new ArrayList<Entity>();
	String qualityClue;

	/**
	 * 
	 */
	public CharacterStatementParser(ArrayList<String> ontologyIRIs) {
		super(ontologyIRIs);
	}


	/** 
	 * @param: subjects should be empty, characterClue is null. 
	 */
	@Override
	public void parse(Element statement) {
		// TODO: move KeyEntityFinder here.

	}


	public String getQualityClue(){
		return this.qualityClue;
	}

	public ArrayList<Entity> getEntities(){
		return this.entities;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
