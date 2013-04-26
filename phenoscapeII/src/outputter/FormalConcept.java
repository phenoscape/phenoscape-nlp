/**
 * 
 */
package outputter;

/**
 * @author updates
 *
 */
public interface FormalConcept {
	void setString(String string);
	void setLabel(String label);
	void setId(String id);
	void setConfidenceScore(float score);//may also record difficulty level
	String getString();
	String getLabel();
	String getId();
	float getConfidienceScore();
	String toString();
	boolean isOntologized();
}
