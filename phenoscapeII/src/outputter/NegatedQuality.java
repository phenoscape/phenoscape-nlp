/**
 * 
 */
package outputter;

/**
 * @author updates
 *
 */
public class NegatedQuality extends Quality {
	Quality parentQuality;
	public static FormalRelation negation = new FormalRelation("no", "complement_of", "PHENOSCAPE_complement_of", ""); //TODO add iri
	

	/**
	 * 
	 */
	public NegatedQuality() {
		negation.setConfidenceScore((float)1.0);
	}

	/**
	 * @param string
	 * @param label
	 * @param id
	 * @param iri
	 */
	public NegatedQuality(String string, String label, String id, String iri, Quality parentQuality) {
		super(string, label, id, iri);
		this.parentQuality = parentQuality;
		negation.setConfidenceScore((float)1.0);
	}
	
	public void setParentQuality(Quality parentQuality){
		this.parentQuality = parentQuality;
	}
	
	public Quality getParentQuality(){
		return this.parentQuality;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
