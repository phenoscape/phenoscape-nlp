/**
 * 
 */
package outputter.data;

/**
 * @author updates
 *
 */
public interface FormalConcept {
	void setSearchString(String string); //string used to search ontology to find the ids and labels
	void setString(String string); //the original string to be ontologized, or in case of other known concepts (such as complement of) required no search, it is the label.
	//void setXMLid(String xmlid);
	void setLabel(String label);
	void setId(String id);
	void setClassIRI(String IRI);
	void setConfidenceScore(float score);//may also record difficulty level
	String getSearchString();
	String getString();
	//String getXMLid();
	String getLabel();
	String getId();
	String getClassIRI();
	float getConfidenceScore();
	String toString(); //for display
	String content(); //for comparison, including identifying info such as classIRI
	boolean isOntologized();
	
}
