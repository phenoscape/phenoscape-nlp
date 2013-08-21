/**
 * 
 */
package outputter.data;

/**
 * @author updates
 *
 */
public interface FormalConcept {
	void setString(String string);
	//void setXMLid(String xmlid);
	void setLabel(String label);
	void setId(String id);
	void setClassIRI(String IRI);
	void setConfidenceScore(float score);//may also record difficulty level
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
