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
		negation.setConfidenceScore(1);
	}

	/**
	 * @param string
	 * @param label
	 * @param id
	 * @param iri
	 */
	public NegatedQuality(String string, String label, String id, Quality parentQuality) {
		super(string, label, id);
		this.parentQuality = parentQuality;
		
		negation.setConfidenceScore(1);
	}

	public NegatedQuality(Quality quality, Quality parentQuality) {
		super(quality.getString(), quality.getLabel(), quality.getId());
		this.parentQuality = parentQuality;
		negation.setConfidenceScore(1);
	}
	
	
	public void setParentQuality(Quality parentQuality){
		this.parentQuality = parentQuality;
	}
	
	public Quality getParentQuality(){
		return this.parentQuality;
	}

	/**
	 * not waisted => shape and complement_of some waisted
	 */
	public String toString(){
		return "phrase="+this.string+" quality="+parentQuality.getLabel()+" and "+negation.toString()+ " some "+ this.label+" score="+this.confidenceScore;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
