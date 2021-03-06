/**
 * 
 */
package outputter.data;

import java.util.ArrayList;
import java.util.Comparator;


/**
 * @author Hong Cui
 *
 */
public class SimpleEntity extends Entity implements FormalConcept{
	String string; 
	String searchString;
	String label; //lable of the class representing the phrase in an ontology
	String id; //id of the class representing the phrase in an ontology
	String classIRI; //class iri of the class representing the phrase in an ontology
	float confidenceScore; //the confidence the system has in the id and classIRI represent the semantics of the string.

	
	public SimpleEntity(){
		
	}

	/**
	 * 
	 */
	public SimpleEntity(String string, String label, String id, String iri, String searchstring) {
		this.string = string;
		this.label = label;
		this.id = id;
		this.classIRI = iri;	
		this.searchString = searchstring;
	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#setString(java.lang.String)
	 */
	@Override
	public void setSearchString(String string) {
		this.string = string;

	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#setLabel(java.lang.String)
	 */
	@Override
	public void setLabel(String label) {
		this.label = label;

	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#setId(java.lang.String)
	 */
	@Override
	public void setId(String id) {
		this.id = id;

	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#setClassIRI(java.lang.String)
	 */
	@Override
	public void setClassIRI(String IRI) {
		this.classIRI = IRI;
	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#setConfidenceScore(float)
	 */
	@Override
	public void setConfidenceScore(float score) {
		this.confidenceScore = score;

	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#getString()
	 */
	@Override
	public String getSearchString() {
		
		return this.string;
	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#getLabel()
	 */
	@Override
	public String getLabel() {
		if(this.label==null)
			return this.string;
		return this.label;
	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#getId()
	 */
	@Override
	public String getId() {
		
		return this.id;
	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#getClassIRI()
	 */
	@Override
	public String getClassIRI() {		
		return this.classIRI;
	}
	
	/* (non-Javadoc)
	 * @see outputter.FormalConcept#getConfidienceScore()
	 */
	@Override
	public float getConfidenceScore() {
	
		return this.confidenceScore;
	}


	@Override
	public boolean isOntologized() {
		
		return this.id != null;
	}

	public String toString(){
		//return "phrase="+this.string+" entity="+(this.label==null? this.string: this.label)+ " score="+this.confidenceScore;
		return "phrase="+this.string+" entity="+this.label + " score="+this.confidenceScore;
	}
	
	public String content(){
		return this.classIRI;
	}
	
	//Clones the simple entity
	public SimpleEntity clone() {
			SimpleEntity entity1 = new SimpleEntity();
			entity1.setId(this.getId());
			entity1.setClassIRI(this.getClassIRI());
			entity1.setConfidenceScore(this.getConfidenceScore());
			entity1.setSearchString(this.getSearchString());
			entity1.setString(this.getString());
			entity1.setLabel(this.getLabel());
		return entity1;
	}

	@Override
	public int compare(Entity e1, Entity e2){
		return e1.content().compareTo(e2.content());
	}
	
	@Override
	public boolean equals(Object e){
		if(e instanceof SimpleEntity){
			if(this.content().compareTo(((SimpleEntity)e).content())==0) return true;
		}
		return false;
	}

	@Override
	public ArrayList<Entity> getIndividualEntities() {
		ArrayList<Entity> individuals = new ArrayList<Entity>();
		individuals.add(this);
		return individuals;
	}

	@Override
	public void setString(String string) {
		this.string = string;
		
	}

	@Override
	public String getString() {
		return this.string;
	}
	
    public String getunontologized(){
    	if(this.getId()==null)
    		return this.getString()+"#";
    	return "";
    }
    
}
